package sml_package;

import static postfix.Token.ADD;
import static postfix.Token.DIV;
import static postfix.Token.MOD;
import static postfix.Token.MUL;
import static postfix.Token.POW;
import static postfix.Token.SUB;
import static sml_package.Condition.EQ;
import static sml_package.Condition.GE;
import static sml_package.Condition.GT;
import static sml_package.Condition.LE;
import static sml_package.Condition.LT;
import static sml_package.Condition.NE;
import static sml_package.Statement.COMMENT;
import static sml_package.Statement.INPUT;
import static sml_package.Statement.INT;
import static sml_package.Statement.LET;
import static symboltable.SymbolType.CONSTANT;
import static symboltable.SymbolType.LINE;
import static symboltable.SymbolType.VARIABLE;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.StringTokenizer;

import memory.CodeWriter;
import memory.Memory;
import requirement.requirements.AbstractRequirement;
import requirement.requirements.Requirements;
import requirement.requirements.StringType;
import sml_package.exceptions.InvalidVariableNameException;
import sml_package.exceptions.NotALineException;
import sml_package.exceptions.NotAVariableException;
import sml_package.exceptions.UnexpectedTokensException;
import sml_package.exceptions.VariableAlreadyDeclaredException;
import sml_package.exceptions.VariableNotDeclaredException;
import symboltable.SymbolTable;
import symboltable.UnknownSymbolException;

public class SML_Compiler {

	public static final SymbolTable symbolTable = new SymbolTable();

	private static final CodeWriter memory = new Memory(256);

	private static final StringBuilder program = new StringBuilder();

	public static final int[]   ifgFlags   = new int[256];
	public static final int[][] ifFlags    = new int[256][2];
	public static final int[][] whileFlags = new int[256][2];

	public static String inputFileName;
	public static int    line_count;
	public static String originalLine;
	public static String variable;

	public static boolean success;

	private static final Collection<String> keywords = new HashSet<>();

	static {
		keywords.addAll(Arrays.asList(ADD.value, SUB.value, MUL.value, DIV.value, POW.value,
		        MOD.value, "=", LT.value, GT.value, LE.value, GE.value, EQ.value, NE.value));
		for (Statement s : Statement.values()) {
			keywords.add(s.identifier);
		}
	}

	public static void main(String[] args) {

		Requirements reqs = getRequirements();

		for (int i = 0; i < args.length; ++i) {
			if (args[i].startsWith("--"))
				reqs.fulfil(args[i].substring(2), args[++i]);
			else if (args[i].startsWith("-"))
				reqs.fulfil(args[i].substring(1), true);
			else
				System.err.printf("Error: invalid argument: %s", args[i]);
		}

		compile(reqs);
	}

	public static Requirements getRequirements() {
		Requirements reqs = new Requirements();

		reqs.add("input", StringType.ANY);
		reqs.add("output", StringType.ANY);
		reqs.add("screen");
		reqs.add("st");

		reqs.fulfil("input", "stdin");
		reqs.fulfil("output", "out.sml");
		reqs.fulfil("screen", false);
		reqs.fulfil("st", false);

		return reqs;
	}

	static void compile(Requirements reqs) {
		if (!reqs.fulfilled()) {
			for (AbstractRequirement r : reqs) {
				if (!r.fulfilled())
					System.err.printf("No value for parameter %s found.%n", r.key());
			}
			System.err.printf("Execution terminated due to above erros.%n");
			return;
		}

		reset();

		String input = (String) reqs.getValue("input");

		if (input.equals("stdin"))
			loadProgramFromStdin();
		else
			loadProgramFromFile(new File(input));

		pass1();
		pass2();

		String  output = (String) reqs.getValue("output");
		boolean screen = (boolean) reqs.getValue("screen");
		boolean st     = (boolean) reqs.getValue("st");

		if (success) {
			if (screen || output.equals("stdout"))
				writeResultsToStdout();
			if (!output.equals("stdout"))
				writeResultsToFile(new File(output));
		} else {
			System.out.println("Due to above errors compilation failed :(");
		}

		if (st)
			System.out.println(symbolTable);
	}

	private static void pass1() {

		memory.initializeForWriting();

		System.out.println("*** Starting compilation\t\t\t ***");

		StringTokenizer lines = new StringTokenizer(program.toString(), "\n");

		String    line = "";
		String[]  tokens;
		Statement statement;

		next_line: while (!line.matches("\\d\\d end")) {

			try {
				// get line and remove extra whitespace
				try {
					originalLine = lines.nextToken();
					line = originalLine.strip().replaceAll("\\s+", " ");
				} catch (NoSuchElementException e) {
					System.err.printf("error at: %s:\t\t EOF reached; no 'end' command found\n",
					        inputFileName);
					success = false;
					break next_line;
				}

				if (line.isBlank())
					continue next_line;

				// handle line number
				tokens = line.split(" ");
				if (isNumber(tokens[0])) {
					line_count = Integer.parseInt(tokens[0]);
					symbolTable.addEntry(String.valueOf(line_count), LINE,
					        memory.getInstructionCounter(), "");
				} else {
					System.err.printf("error at: %s:\t\t '%s' is not a valid line number\n",
					        inputFileName, tokens[0]);
					success = false;
					continue next_line;
				}

				// handle command
				if (tokens.length == 1) {
					System.err.printf("error at: %s:%02d:%02d: no command found\n", inputFileName,
					        line_count, originalLine.length());
					success = false;
					continue next_line;
				}

				statement = Statement.of(tokens[1]);

				// handle constructors (int etc. declarations)
				if (statement.isConstructor) {

					for (int i = 2, count = tokens.length; i < count; i++) {
						variable = tokens[i];
						if (isVariableName(variable)) {
							boolean isDeclared = symbolTable.existsSymbol(variable, VARIABLE);
							if (isDeclared)
								throw new VariableAlreadyDeclaredException(variable);
						} else
							throw new InvalidVariableNameException(variable);
					}

					statement.evaluate(line);
					continue next_line;
				}

				// handle non-constructors
				// if not comment, assert correct number of tokens, every variable is declared and declare constants if needed
				if (!statement.equals(COMMENT)) {
					int targetNumberOfTokens = statement.length;
					if ((targetNumberOfTokens != -1) && (targetNumberOfTokens > tokens.length)) {
						throw new UnexpectedTokensException(originalLine
						        .substring(find(originalLine, tokens[targetNumberOfTokens])));
					}

					int tokensToScan = targetNumberOfTokens == -1 ? tokens.length
					        : targetNumberOfTokens;

					for (int i = 2; i < tokensToScan; i++) {
						String var = tokens[i];
						variable = var;
						// declare constants
						if (isNumber(var)) {
							if (!symbolTable.existsSymbol(var, CONSTANT)) {
								int location = addConstant(Integer.parseInt(var));
								symbolTable.addEntry(var, CONSTANT, location, INT.identifier);
								// future: get type of constant and add with its type
							}
							// assert variable is declared
						} else if (isVariableName(var)) {
							if (!symbolTable.existsSymbol(var, VARIABLE))
								throw new VariableNotDeclaredException(var);
						}
					}
				}

				// ===== AT THIS POINT ALL VARIABLES ARE DECLARED AND ALL CONSTANTS ARE SET =====

				// statement-specific checks
				if (statement.equals(INPUT)) {
					for (String var : line.substring(9).split(" ")) {
						variable = var;
						if (isNumber(var))
							throw new NotAVariableException(var);
					}
				} else if (statement.equals(LET)) {
					String var = tokens[2];
					variable = var;
					if (!isVariableName(var))
						throw new NotAVariableException(var);
				}

				// actually write machine code for each command
				statement.evaluate(line);

			} // end try (one statement / one line)
			catch (Exception e) {
				e.printStackTrace();
				System.err.printf("error at: %s:%02d:%02d: %s", inputFileName, line_count,
				        find(originalLine, variable), e.getMessage());
				success = false;
			}
		} // end while
	} // end pass1

	private static void pass2() {
		System.out.println("*** Completing goto instructions\t ***");
		for (int i = 0; i < 256; i++) {
			try {
				if (ifgFlags[i] < -1) {
					int location = symbolTable.getSymbolLocation(
					        symbolTable.getNextLine(String.valueOf(-ifgFlags[i])), LINE);
					memory.write(i, memory.read(i) + location);
				} else if (ifgFlags[i] != -1) {
					String var = String.valueOf(i);
					variable = var;
					try {
						int location = symbolTable.getSymbolLocation(String.valueOf(ifgFlags[i]),
						        LINE);
						memory.write(i, memory.read(i) + location);
					} catch (UnknownSymbolException e) {
						throw new NotALineException(var);
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
				System.err.printf("error at: %s:%02d:%02d: %s", inputFileName, line_count,
				        find(originalLine, variable), e.getMessage());
				success = false;
			}
		}
		System.out.println("*** Compilation ended\t\t\t\t ***");
	}

	private static void loadProgramFromStdin() {

		@SuppressWarnings("resource")
		Scanner scanner = new Scanner(System.in);
		program.setLength(0);

		String userInput = "";
		int    lineCount = 0;

		while (!userInput.equals("end")) {
			System.out.printf("%02d > ", lineCount);
			userInput = scanner.nextLine();

			program.append(String.format("%02d %s%n", lineCount, userInput));
			++lineCount;
		}
		System.out.printf("program loading done%n");

	}

	private static void loadProgramFromFile(File file) {
		try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
			program.setLength(0);
			String line = reader.readLine();
			while (line != null) {
				program.append(line).append(System.lineSeparator());
				line = reader.readLine();
			}
			System.out.println("*** Program loading completed\t\t ***");
		} catch (FileNotFoundException e) {
			System.err.printf("Couldn't find file %s.%n", file);
		} catch (IOException e) {
			System.err.printf("Unexpected System.err.printfor while reading from file %s.%n", file);
		}
	}

	private static void writeResultsToStdout() {
		System.out.println(memory.listShort());
	}

	private static void writeResultsToFile(File file) {
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
			writer.write(memory.list());
		} catch (IOException e) {
			System.err.printf("Unexpected error while writing to file %s.%n", file);
		}
	}

	private static void reset() {
		memory.clear();

		Arrays.fill(ifgFlags, -1);

		for (int i = 0; i < ifFlags.length; i++)
			for (int j = 0; j < ifFlags[0].length; j++)
				ifFlags[i][j] = -1;

		for (int i = 0; i < ifFlags.length; i++)
			for (int j = 0; j < whileFlags[0].length; j++)
				whileFlags[i][j] = -1;

		symbolTable.clear();

		inputFileName = "";
		success = true;
	}

	public static int addInstruction(int instruction) {
		return memory.writeInstruction(instruction);
	}

	public static int addConstant(int value) {
		return memory.writeConstant(value);
	}

	public static int addVariable() {
		return memory.assignPlaceForVariable();
	}

	public static void setBranchLocation(int location, int address) {
		memory.write(location, memory.read(location) + address);
	}

	public static boolean isVariableName(String var) {
		return var.matches("[a-zA-Z]\\w*") && !keywords.contains(var);
	}

	public static boolean isNumber(String con) {
		try {
			Integer.parseInt(con, 10);
			return true;
		} catch (NumberFormatException e) {
			return false;
		}
	}

	private static int find(String line, String s) {
		return line.indexOf(s, 2);
	}
}

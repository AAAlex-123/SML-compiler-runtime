package sml_package;

import static postfix.Token.ADD;
import static postfix.Token.DIV;
import static postfix.Token.MOD;
import static postfix.Token.MUL;
import static postfix.Token.POW;
import static postfix.Token.SUB;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.StringTokenizer;

import memory.CodeWriter;
import memory.Memory;
import postfix.InfixToPostfix;
import postfix.PostfixEvaluator;
import requirement.requirements.AbstractRequirement;
import requirement.requirements.Requirements;
import requirement.requirements.StringType;
import symboltable.SymbolTable;
import symboltable.UnknownSymbolException;

public class SML_Compiler {
	private static final String COMMENT  = "//";
	private static final String INT      = "int";
	private static final String INPUT    = "input";
	private static final String LET      = "let";
	private static final String PRINT    = "print";
	private static final String GOTO     = "goto";
	private static final String IFGOTO   = "ifg";
	private static final String IF       = "if";
	private static final String ELSE     = "else";
	private static final String ENDIF    = "endif";
	private static final String WHILE    = "while";
	private static final String ENDWHILE = "endwhile";
	private static final String END      = "end";
	private static final String NOOP     = "noop";
	private static final String DUMP     = "dump";

	private static final HashMap<String, Integer> lengths = new HashMap<>();

	public static final SymbolTable symbolTable = new SymbolTable();

	private static final CodeWriter memory = new Memory(256);

	private static final StringBuilder program = new StringBuilder();

	private static final int[]   ifgFlags   = new int[256];
	private static final int[][] ifFlags    = new int[256][2];
	private static final int[][] whileFlags = new int[256][2];

	public static int     line_count;
	private static String command;

	public static String inputFileName;

	public static boolean succ;

	private static final Collection<String> keywords     = new HashSet<>(
	        Arrays.asList(COMMENT, INPUT, IF, GOTO, LET, PRINT, END, IFGOTO, ELSE, ENDIF, WHILE,
	                ENDWHILE, NOOP, DUMP, ADD.value, SUB.value, MUL.value, DIV.value, POW.value,
	                MOD.value, "=", "==", "<=", ">=", "!=", "<", ">"));

	private static final Collection<String> constructors = new HashSet<>(Arrays.asList(INT));

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

		if (succ) {
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

	private static int pass1() {

		memory.initializeForWriting();

		System.out.println("*** Starting compilation\t\t\t ***");

		StringTokenizer tok = new StringTokenizer(program.toString(), "\n");

		String   originalLine;
		String   line = "";
		String[] tokens;

		next_line:
		while (!line.matches("\\d\\d end")) {

			// get line and remove extra whitespace
			try {
				originalLine = tok.nextToken();
				System.out.println("read: " + originalLine);
				line = originalLine.strip().replace("\s+", " ");
			} catch (NoSuchElementException e) {
				System.err.printf("error at: %s:\t\t EOF reached; no '99 end' command found\n", inputFileName);
				return 1;
			}

			if (line.isBlank())
				continue next_line;

			// handle line number
			tokens = line.split(" ");
			if (isNumber(tokens[0])) {
				line_count = Integer.parseInt(tokens[0]);
				symbolTable.addEntry(String.valueOf(line_count), LINE, memory.getInstructionCounter(), "");
			}
			else {
				System.err.printf("error at: %s:\t\t '%s' is not a valid line number\n", inputFileName, tokens[0]);
				return 1;
			}

			// handle command
			if (tokens.length == 1) {
				System.err.printf("error at: %s:%02d:%02d: no command found\n", inputFileName, line_count, originalLine.length());
				succ = false;
			}

			command = tokens[1];

			// handle constructors (int etc. declarations)
			if (constructors.contains(command)) {
				for (int i = 2, count = tokens.length; i < count; i++) {
					String variable = tokens[i];
					if (isVariableName(variable)) {
						boolean isDeclared = symbolTable.existsSymbol(variable, VARIABLE);
						if (isDeclared) {
							System.err.printf(
							        "error at: %s:%02d:%02d: variable '%s' already declared\n",
							        inputFileName, line_count, find(originalLine, variable),
							        variable);
							succ = false;
						} else {
							switch (command) {
							case INT:
								int location = addVariable();
								symbolTable.addEntry(variable, VARIABLE, location, command);
								break;
							default:
								System.err.printf("bad constructor");
							}
							// future: add other types
						}
					} else if (isNumber(variable)) {
						System.err.printf(
						        "error at: %s:%02d:%02d: can't declare constant '%s' as variable\n",
						        inputFileName, line_count, find(originalLine, variable),
						        variable);
						succ = false;
					} else if (keywords.contains(variable)) {
						System.err.printf(
						        "error at: %s:%02d:%02d: variable name '%s' is reserved\n",
						        inputFileName, line_count, find(originalLine, variable),
						        variable);
						succ = false;
					} else {
						System.err.printf(
						        "%s:%02d:\t error: hmmm there is an error here... (pls let me know)\n",
						        inputFileName, line_count, variable);
						succ = false;
					}
				}
				continue next_line;
			}

			// handle non-constructors
			// if not comment, assert correct number of tokens, every variable is declared and declare constants if needed
			if (!command.equals(COMMENT)) {
				int targetNumberOfTokens = lengths.get(command);
				if (targetNumberOfTokens != -1) {
					System.err.printf("error at: %s:%02d:%02d: unexpected stuff '%s'\n",
					        inputFileName,
					        line_count, find(originalLine, tokens[targetNumberOfTokens]),
					        originalLine
					                .substring(find(originalLine, tokens[targetNumberOfTokens])));
					succ = false;
				}

				int tokensToScan = targetNumberOfTokens == -1 ? tokens.length
				        : targetNumberOfTokens;

				for (int i = 2; i < tokensToScan; i++) {
					String token = tokens[i];
					// declare constants
					if (isNumber(token)) {
						if (!symbolTable.existsSymbol(token, CONSTANT)) {
							int location = addVariable();
							symbolTable.addEntry(token, CONSTANT, location, INT);
							addConstant(Integer.parseInt(token));
							// future: get type and add with its type
						}
						// assert variable is declared
					} else if (isVariableName(token)) {
						if (!symbolTable.existsSymbol(token, VARIABLE)) {
							System.err.printf(
							        "error at: %s:%02d:%02d: variable '%s' not declared\n",
							        inputFileName, line_count, find(originalLine, token), token);
							succ = false;
						}
					} else if (keywords.contains(token)) {
						;
					} else {
						System.err.printf(
						        "%s:%02d:\t error: hmmm there is an error here... (pls let me know)\n",
						        inputFileName, line_count, token);
						succ = false;
					}
				}
			}
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
		} // end while
		return succ ? 0 : 1;
	} // end pass1

	private static int pass2() {
		System.out.println("*** Completing jump instructions\t ***");
		for (int i = 0; i < 256; i++)
			if (ifgFlags[i] < -1) {
				int location = symbolTable.getSymbolLocation(
				        symbolTable.getNextLine(String.valueOf(-ifgFlags[i])), LINE);
				memory.write(i, memory.read(i) + location);
			}
			else if (ifgFlags[i] != -1) {
				try {
					int location = symbolTable.getSymbolLocation(String.valueOf(ifgFlags[i]), LINE);
					memory.write(i, memory.read(i) + location);
				} catch (UnknownSymbolException e) {
					System.out.printf("%s:%02d:\t error: variable %s not found\n", inputFileName,
					        line_count, ifgFlags[i]);
					succ = false;
				}
			}
		System.out.println("*** Compilation ended\t\t\t\t ***");
		return 0;
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
			String line      = reader.readLine();
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
		boolean zeros = false;
		for (int i = 0, size = memory.size(); i < size; i++) {

			int val = memory.read(i);

			// replace any number of consecutive zeros with '** ****'
			if ((val == 0) && !zeros) {
				zeros = true;
				System.out.println("** ****");
			} else if (val != 0)
				zeros = false;

			if (!zeros)
				System.out.printf("%02x %04x\n", i, val);
		}
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
		succ = true;

		resetMap();
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

	private static boolean isVariableName(String var) {
		return var.matches("[a-zA-Z]\\w*") && !keywords.contains(var);
	}

	private static boolean isNumber(String con) {
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

	private static void resetMap() {
		lengths.clear();
		lengths.put(COMMENT, -1);
		lengths.put(INT, -1);
		lengths.put(INPUT, -1);
		lengths.put(LET, -1);
		lengths.put(PRINT, -1);
		lengths.put(IFGOTO, 7);
		lengths.put(IF, 5);
		lengths.put(ELSE, 3);
		lengths.put(ENDIF, 3);
		lengths.put(WHILE, 5);
		lengths.put(ENDWHILE, 3);
		lengths.put(END, 2);
		lengths.put(NOOP, 2);
		lengths.put(DUMP, 2);
	}
}

package compiler;

import static compiler.Statement.COMMENT;
import static compiler.Statement.INT;
import static compiler.postfix.Token.ADD;
import static compiler.postfix.Token.DIV;
import static compiler.postfix.Token.MOD;
import static compiler.postfix.Token.MUL;
import static compiler.postfix.Token.POW;
import static compiler.postfix.Token.SUB;
import static compiler.symboltable.SymbolType.CONSTANT;
import static compiler.symboltable.SymbolType.LINE;
import static compiler.symboltable.SymbolType.VARIABLE;

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
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.Stack;
import java.util.StringTokenizer;

import compiler.blocks.Block;
import compiler.exceptions.CompilerException;
import compiler.exceptions.InvalidLineNameException;
import compiler.exceptions.InvalidVariableNameException;
import compiler.exceptions.NotALineException;
import compiler.exceptions.NotAVariableException;
import compiler.exceptions.UnclosedBlockException;
import compiler.exceptions.UnexpectedTokenException;
import compiler.exceptions.UnexpectedTokensException;
import compiler.exceptions.VariableAlreadyDeclaredException;
import compiler.exceptions.VariableNotDeclaredException;
import compiler.symboltable.SymbolInfo;
import compiler.symboltable.SymbolTable;
import compiler.symboltable.SymbolType;
import memory.CodeWriter;
import memory.Memory;
import requirement.requirements.AbstractRequirement;
import requirement.requirements.Requirements;
import requirement.requirements.StringType;
import runtime.Instruction;

/**
 * A Compiler for the high-level language. It defines the {@code static} method
 * {@link SML_Compiler#compile compile} which generates machine code
 * instructions for a high-level-language program. The Compiler is
 * {@code stateless} meaning that no information is stored between compilations
 * and that an instance of a Compiler is not necessary to compile a program.
 * Before each call to {@code compile} the Compiler is automatically reset.
 * <p>
 * The compilation uses {@link requirement.requirements.AbstractRequirement
 * Requirements} in order to specify different parameters. They can be obtained
 * with the {@link SML_Compiler#getRequirements() getRequriements()} method,
 * which contains more information about each individual Requirement.
 *
 * @author Alex Mandelias
 */
public class SML_Compiler {

	private static final SymbolTable           symbolTable = new SymbolTable();
	private static final CodeWriter            memory      = new Memory(256);
	private static final StringBuilder         program     = new StringBuilder();
	private static final Map<Integer, Integer> ifgFlags    = new HashMap<>();
	private static final Stack<Block>          blockStack  = new Stack<>();
	private static final Collection<String>    keywords    = new HashSet<>();

	static {
		keywords.addAll(Arrays.asList(ADD.value, SUB.value, MUL.value, DIV.value, POW.value,
		        MOD.value, "="));
		for (Statement s : Statement.values())
			keywords.add(s.identifier);
		for (Condition s : Condition.values())
			keywords.add(s.value);
	}

	private static class CompilationData {
		public String  inputFileName;
		public String  originalLine;
		public int     lineNumber;
		public String  variable;
		public boolean success;
	}

	/* Don't let anyone instantiate this class */
	private SML_Compiler() {}

	/**
	 * Uses the command line arguments to specify the parameters necessary to
	 * compile a high-level-language program, and then compiles it. Parameters
	 * starting with a single dash '-' are set to {@code true}. Parameters starting
	 * with a double dash '--' are set to whatever the next argument is.
	 * <p>
	 * The different parameters are documented in the
	 * {@link SML_Compiler#getRequirements() getRequirements()} method.
	 *
	 * @param args the command line arguments
	 */
	public static void main(String[] args) {

		Requirements reqs = getRequirements();

		for (int i = 0, count = args.length; i < count; ++i) {
			if (args[i].startsWith("--"))
				reqs.fulfil(args[i].substring(2), args[++i]);
			else if (args[i].startsWith("-"))
				reqs.fulfil(args[i].substring(1), true);
			else
				err("Invalid parameter: %s. Parameters must start with either one '-' or two '--' dashes.",
				        args[i]);
		}

		compile(reqs);
	}

	/**
	 * Returns the {@code Requirements} needed for compilation. They have their
	 * default values and can be used as-is for compilation.
	 *
	 * <pre>
	 * | Value   | Default | Explanation          | Command Line |
	 * |---------|---------|----------------------|--------------|
	 * | input   | stdin   | "stdin" or filename  | --           |
	 * | output  | out.sml | "stdout" or filename | --           |
	 * | screen  | false   | output to stdout too | -            |
	 * | st      | false   | output SymbolTable   | -            |
	 * | verbose | false   | output all messages  | -            |
	 * </pre>
	 *
	 * @return the Requirements
	 */
	public static Requirements getRequirements() {
		Requirements reqs = new Requirements();

		reqs.add("input", StringType.ANY);
		reqs.add("output", StringType.ANY);
		reqs.add("screen");
		reqs.add("st");
		reqs.add("verbose");

		reqs.fulfil("input", "stdin");
		reqs.fulfil("output", "out.sml");
		reqs.fulfil("screen", false);
		reqs.fulfil("st", false);
		reqs.fulfil("verbose", false);

		return reqs;
	}

	/**
	 * Uses the parameters from the {@code requirements} in order to load the
	 * program, compile it and output the results.
	 * <p>
	 * The different Requirements are documented in the
	 * {@link SML_Compiler#getRequirements() getRequirements()} method.
	 *
	 * @param requirements the parameters needed to compile
	 */
	public static void compile(Requirements requirements) {
		if (!requirements.fulfilled()) {
			for (AbstractRequirement r : requirements)
				if (!r.fulfilled())
					err("No value for parameter '%s' found", r.key());

			err("Compilation couldn't start due to missing parameters");
			return;
		}

		reset();

		CompilationData data = new CompilationData();


		String  input  = (String) requirements.getValue("input");
		String  output = (String) requirements.getValue("output");
		boolean screen = (boolean) requirements.getValue("screen");
		boolean st     = (boolean) requirements.getValue("st");
		boolean verbose = (boolean) requirements.getValue("verbose");

		data.inputFileName = input.equals("stdin") ? "<stdin>" : input;

		if (!verbose) {

			// === SILENT COMPILATION ===

			if (input.equals("stdin"))
				loadProgramFromStdin();
			else
				loadProgramFromFile(new File(input));

			pass1(data);
			pass2(data);

			if (data.success) {
				if (output.equals("stdout")) {
					out("The following memory dump is suitable for execution.%n-------");
					writeResultsToStdout(true);
					out("-------%n");
				} else
					writeResultsToFile(new File(output));

				if (screen) {
					out("The following memory dump is NOT suitable for execution.%n-------");
					writeResultsToStdout(false);
					out("-------%n");
				}
			}

			if (st)
				out("Symbol Table:%n%s", symbolTable);

		} else {

			// === VERBOSE COMPILATION ===

			if (input.equals("stdin")) {
				out("Loading program from Standard Input");
				out("The line number for each statement will be printed");
				out("Type 'end' to stop inputting code");
				loadProgramFromStdin();
			} else {
				out("Loading program from file: %s", input);
				loadProgramFromFile(new File(input));
			}
			out("Progarm loading completed");

			out("Compilation started");
			pass1(data);

			out("Completing goto instructions");
			pass2(data);

			out("Compilation ended");

			if (data.success) {
				if (output.equals("stdout")) {
					out("The following memory dump is suitable for execution.%n-------");
					writeResultsToStdout(true);
					out("-------%n");
				} else {
					out("Writing generated machine code to file: %s", output);
					writeResultsToFile(new File(output));
				}

				if (screen) {
					out("The following memory dump is NOT suitable for execution.%n-------");
					writeResultsToStdout(false);
					out("-------%n");
				}
			}

			if (st)
				out("Symbol Table:%n%s", symbolTable);

			if (data.success)
				out("Compilation succeeded :)");
			else
				out("Compilation failed :(");
		}
	}

	/* Does everything apart from completing 'jump' instructions */
	private static void pass1(CompilationData data) {

		memory.initializeForWriting();

		StringTokenizer lineTokenizer = new StringTokenizer(program.toString(), "\n");

		String    line = "";
		String[]  tokens;
		Statement statement;

		next_line: while (!line.matches("\\d\\d end")) {

			try {
				// always terminate, even when no '\d\d end' statement is found
				if (!lineTokenizer.hasMoreTokens()) {
					addInstruction(Instruction.HALT.opcode());
					break next_line;
				}

				// get line and remove extra whitespace
				data.originalLine = lineTokenizer.nextToken();
				line = data.originalLine.strip().replaceAll("\\s+", " ");

				if (line.isBlank())
					continue next_line;

				// handle line number
				tokens = line.split(" ");
				String lineNo = tokens[0];
				if (!isNumber(lineNo))
					throw new InvalidLineNameException(lineNo);

				addLine(lineNo);
				data.lineNumber = Integer.parseInt(lineNo);

				// handle command
				if (tokens.length == 1)
					throw new UnexpectedTokenException("", "a command");

				statement = Statement.of(tokens[1]);

				// handle constructors (int etc. declarations)
				if (statement.isConstructor) {
					for (int i = 2, count = tokens.length; i < count; ++i) {
						String var = tokens[i];
						data.variable = var;

						if (!isVariableName(var))
							throw new InvalidVariableNameException(var);

						if (variableDeclared(var))
							throw new VariableAlreadyDeclaredException(var);
					}

					statement.evaluate(line);
					continue next_line;
				}

				// handle non-constructors
				/*
				 * If the statement is not a comment, assert correct number of tokens, every
				 * variable is declared and declare constants if needed. Declaring every number
				 * found as a constant has the unfortunate side effect of treating line numbers
				 * in goto statements as constants. For example, the statements `10 goto 5` and
				 * `15 ifg a > b goto 5` would both declare a constant of value `5`. To fix
				 * this, a more elaborate and statement-specific parsing is required.
				 */
				if (!statement.equals(COMMENT)) {
					int tokensInStatement = statement.length;

					if ((tokensInStatement != -1) && (tokensInStatement < tokens.length)) {
						data.variable = tokens[tokensInStatement];
						throw new UnexpectedTokensException(
						        data.originalLine.substring(find(data)));
					}

					int tokensToScan = tokensInStatement == -1 ? tokens.length
					        : tokensInStatement;

					for (int i = 2, count = tokensToScan; i < count; ++i) {
						String var = tokens[i];
						data.variable = var;
						if (isNumber(var)) {
							// declare constants
							if (!constantDeclared(var))
								declareConstant(var, INT.identifier);

						} else if (isVariableName(var)) {
							// assert variable is declared
							if (!variableDeclared(var))
								throw new VariableNotDeclaredException(var);

						} else if (keywords.contains(var)) {
							// dismiss keywords

						} else {
							throw new RuntimeException("the fuck how did we get here");
						}
					}
				}

				/*
				 * at this point:
				 *   - all variables are declared
				 *   - all constants are set
				 *   - all lines exist
				 */

				// statement-specific checks
				switch (statement) {
				case INPUT:
					String var;
					for (String inputVar : line.substring(9).split(" "))
						if (isNumber(inputVar))
							throw new NotAVariableException(inputVar);
					break;
				case LET:
					var = tokens[2];
					data.variable = var;
					if (!isVariableName(var))
						throw new NotAVariableException(var);

					var = tokens[3];
					data.variable = var;
					if (!var.equals("="))
						throw new UnexpectedTokenException(var, "=");
					break;
				case IFGOTO:
					var = tokens[3];
					data.variable = var;
					if (Condition.of(var) == null)
						throw new UnexpectedTokenException(var, "a condition");

					var = tokens[5];
					data.variable = var;
					if (!var.equals("goto"))
						throw new UnexpectedTokenException(var, "goto");
					break;
				case END:
					if (!blockStack.empty())
						throw new UnclosedBlockException(blockStack.pop());
					break;
				default:
					break;
				}

				// actually write machine code for each command
				statement.evaluate(line);

			} // end try (one statement / one line)
			catch (CompilerException e) {
				err("at: %s:%02d:%02d: %s", data.inputFileName, data.lineNumber,
				        find(data), e.getMessage());
				data.success = false;
			}
		} // end of while
	} // end of pass1

	private static void pass2(CompilationData data) {
		for (Entry<Integer, Integer> entry : ifgFlags.entrySet()) {

			int instructionAddress = entry.getKey();
			int lineToJump         = entry.getValue();

			try {
				String var = String.valueOf(instructionAddress);
				data.variable = var;

				if (!lineDeclared(var))
					throw new NotALineException(String.valueOf(lineToJump));

				int location    = getLine(String.valueOf(lineToJump)).location;
				int instruction = memory.read(instructionAddress);
				memory.write(instructionAddress, instruction + location);

			} catch (CompilerException e) {
				err("at: %s:%02d:%02d: %s", data.inputFileName, data.lineNumber,
				        find(data), e.getMessage());
				data.success = false;
			}
		}
	}

	// --- 5 methods for input, output and reset ---

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
	}

	private static void loadProgramFromFile(File file) {
		program.setLength(0);
		String lineSep = System.lineSeparator();

		try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
			for (String line = reader.readLine(); line != null; line = reader.readLine())
				program.append(line).append(lineSep);

		} catch (FileNotFoundException e) {
			err("Couldn't find file: %s", file);
		} catch (IOException e) {
			err("Unexpected error while reading from file: %s", file);
		}
	}

	private static void writeResultsToStdout(boolean suitableForExecution) {
		out("Generated machine code:%n%s",
		        suitableForExecution ? memory.list() : memory.listShort());
	}

	private static void writeResultsToFile(File file) {
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
			writer.write(memory.list());

		} catch (IOException e) {
			err("Unexpected error while writing to file: %s", file);
		}
	}

	private static void reset() {
		memory.clear();
		ifgFlags.clear();
		blockStack.clear();
		symbolTable.clear();
	}

	// --- 2 method for uniform message printing ---

	private static void out(String text, Object... args) {
		System.out.printf("Compilation Info:  %s%n", String.format(text, args));
	}

	private static void err(String text, Object... args) {
		System.err.printf("Compilation Error: %s%n", String.format(text, args));
	}

	// --- 3 memory wrapper-delegate methods

	/**
	 * Adds an instruction to the memory.
	 *
	 * @param instruction the instruction
	 *
	 * @return the location in memory where it was placed
	 */
	static int addInstruction(int instruction) {
		return memory.writeInstruction(instruction);
	}

	/**
	 * Writes a constant to the memory.
	 *
	 * @param constant the constant
	 *
	 * @return the location in memory where it was placed
	 */
	static int addConstant(int constant) {
		return memory.writeConstant(constant);
	}

	/**
	 * Allocates an address for a variable.
	 *
	 * @return the address of the variable
	 */
	public static int addVariable() {
		return memory.assignPlaceForVariable();
	}

	// --- 11 symbolt table wrapper-delegate methods ---

	/**
	 * Declares line by creating an Entry in the Symbol Table.
	 *
	 * @param symbol the line's symbol
	 *
	 * @return the location of the declared line in memory, the location of the
	 *         first instruction corresponding to this line
	 */
	static int addLine(String symbol) {
		int location = memory.getInstructionCounter();
		symbolTable.addEntry(symbol, LINE, location, "");
		return location;
	}

	/**
	 * Declares a constant of a specific {@code varType} for a {@code symbol} by
	 * storing it an address for it in memory and creating an Entry in the Symbol
	 * Table.
	 *
	 * @param symbol  the constant's symbol
	 * @param varType the constant's varType (int, string etc.)
	 *
	 * @return the location of the declared constant in memory
	 */
	static int declareConstant(String symbol, String varType) {
		int location = addConstant(Integer.parseInt(symbol));
		symbolTable.addEntry(symbol, CONSTANT, location, varType);
		return location;
	}

	/**
	 * Declares a variable of a specific {@code varType} for a {@code symbol} by
	 * allocating an address for it in memory and creating an Entry in the Symbol
	 * Table.
	 *
	 * @param symbol  the variable's symbol
	 * @param varType the variable's varType (int, string etc.)
	 *
	 * @return the location of the declared variable in memory
	 */
	static int declareVariable(String symbol, String varType) {
		int location = addVariable();
		SML_Compiler.symbolTable.addEntry(symbol, VARIABLE, location,
		        varType);
		return location;
	}

	/**
	 * Delegate method.
	 *
	 * @param symbol the symbol
	 *
	 * @return {@code true} if it exists, {@code false} othewise
	 *
	 * @see compiler.symboltable.SymbolTable#existsSymbol(String, SymbolType)
	 *      SymbolTable.existsSymbol(String, VARIABLE)
	 */
	static boolean variableDeclared(String symbol) {
		return symbolTable.existsSymbol(symbol, VARIABLE);
	}

	/**
	 * Delegate method.
	 *
	 * @param symbol the symbol
	 *
	 * @return {@code true} if it exists, {@code false} othewise
	 *
	 * @see compiler.symboltable.SymbolTable#existsSymbol(String, SymbolType)
	 *      SymbolTable.existsSymbol(String, CONSTANT)
	 */
	static boolean constantDeclared(String symbol) {
		return symbolTable.existsSymbol(symbol, CONSTANT);
	}

	/**
	 * Delegate method.
	 *
	 * @param symbol the symbol
	 *
	 * @return {@code true} if it exists, {@code false} othewise
	 *
	 * @see compiler.symboltable.SymbolTable#existsSymbol(String, SymbolType)
	 *      SymbolTable.existsSymbol(String, LINE)
	 */
	static boolean lineDeclared(String symbol) {
		return symbolTable.existsSymbol(symbol, LINE);
	}

	/**
	 * Delegate method.
	 *
	 * @param symbol the symbol
	 *
	 * @return information about the symbol
	 *
	 * @see compiler.symboltable.SymbolTable#getSymbol(String, SymbolType...)
	 *      SymbolTable.getSymbol(String, VARIABLE)
	 */
	static SymbolInfo getVariable(String symbol) {
		return symbolTable.getSymbol(symbol, VARIABLE);
	}

	/**
	 * Delegate method.
	 *
	 * @param symbol the symbol
	 *
	 * @return information about the symbol
	 *
	 * @see compiler.symboltable.SymbolTable#getSymbol(String, SymbolType...)
	 *      SymbolTable.getSymbol(String, CONSTANT)
	 */
	static SymbolInfo getConstant(String symbol) {
		return symbolTable.getSymbol(symbol, CONSTANT);
	}

	/**
	 * Delegate method.
	 *
	 * @param symbol the symbol
	 *
	 * @return information about the symbol
	 *
	 * @see compiler.symboltable.SymbolTable#getSymbol(String)
	 *      SymbolTable.getSymbol(String, LINE)
	 */
	static SymbolInfo getLine(String symbol) {
		return symbolTable.getSymbol(symbol, LINE);
	}

	/**
	 * Delegate method.
	 *
	 * @param symbol the symbol
	 *
	 * @return information about the symbol
	 *
	 * @see compiler.symboltable.SymbolTable#getSymbol(String, SymbolType...)
	 *      SymbolTable.getSymbol(String, VARIABLE, CONSTANT)
	 */
	static SymbolInfo getSymbol(String symbol) {
		return symbolTable.getSymbol(symbol, VARIABLE, CONSTANT);
	}

	/**
	 * Returns the {@code SymbolTable} that the compiler uses to keep track of the
	 * different symbols found in the high-level program.
	 *
	 * @return the Symbol Table
	 */
	static SymbolTable getSymbolTable() {
		return symbolTable;
	}

	// --- 2 methods for handling branch instructions ---

	/**
	 * Sets the {@code address} in memory where the a branch instruction will jump
	 * to. The instruction is located at the {@code location} in memory.
	 *
	 * @param location the address of the instruction
	 * @param address  the address to jump
	 */
	static void setBranchLocation(int location, int address) {
		memory.write(location, memory.read(location) + address);
	}

	/**
	 * Sets the {@code line} in the high-level-program where the a branch
	 * instruction will jump to. The instruction is located at the {@code location}
	 * in memory. This method does NOT complete the actual branch instruction. It
	 * acts merely marks the line so that later, when the address of that line in
	 * the machine code is known, the branch instruction can be completed.
	 *
	 * @param location   the address of the instruction.
	 * @param lineToJump the line to jump
	 */
	static void setLineToJump(int location, int lineToJump) {
		ifgFlags.put(location, lineToJump);
	}

	// --- 2 method for handling the stack of blocks ---

	/**
	 * Pushes a new {@code Block} to the stack.
	 *
	 * @param block the block
	 */
	static void pushBlock(Block block) {
		blockStack.push(block);
	}

	/**
	 * Retrieves the most recently added {@code Block} from the stack.
	 *
	 * @return the block
	 */
	static Block popBlock() {
		return blockStack.pop();
	}

	// --- 2 methods for defining variables and constants ---

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

	// --- idk really ---

	private static int find(CompilationData data) {
		return data.originalLine.indexOf(data.variable, 2);
	}
}

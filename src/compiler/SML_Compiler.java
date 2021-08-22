package compiler;

import static compiler.Statement.COMMENT;
import static compiler.Statement.INT;
import static compiler.Statement.LET;
import static compiler.postfix.Token.ADD;
import static compiler.postfix.Token.DIV;
import static compiler.postfix.Token.MOD;
import static compiler.postfix.Token.MUL;
import static compiler.postfix.Token.POW;
import static compiler.postfix.Token.SUB;
import static compiler.symboltable.SymbolType.CONSTANT;
import static compiler.symboltable.SymbolType.LABEL;
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
import compiler.exceptions.InvalidLabelNameException;
import compiler.exceptions.InvalidLineNameException;
import compiler.exceptions.InvalidVariableNameException;
import compiler.exceptions.LabelAlreadyDeclaredException;
import compiler.exceptions.LabelNotDeclaredException;
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

	private static final Collection<String> keywords = new HashSet<>();

	static {
		keywords.addAll(Arrays.asList(ADD.value, SUB.value, MUL.value, DIV.value, POW.value, MOD.value, "="));
		for (Statement s : Statement.values())
			keywords.add(s.identifier);
		for (Condition s : Condition.values())
			keywords.add(s.value);
	}

	private final SymbolTable          symbolTable;
	private final CodeWriter           memory;
	private final StringBuilder        program;
	private final Map<Integer, String> labelFlags;
	private final Stack<Block>         blockStack;

	private static class CompilationData {
		public String  inputFileName;
		public String  originalLine;
		public int     lineNumber;
		public String  variable;
		public boolean success;
	}

	/* Don't let anyone instantiate this class */
	private SML_Compiler() {
		symbolTable = new SymbolTable();
		memory      = new Memory(256);
		program     = new StringBuilder();
		labelFlags    = new HashMap<>();
		blockStack  = new Stack<>();
	}

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

		SML_Compiler compiler = new SML_Compiler();

		CompilationData data = new CompilationData();


		String  input   = (String) requirements.getValue("input");
		String  output  = (String) requirements.getValue("output");
		boolean screen  = (boolean) requirements.getValue("screen");
		boolean st      = (boolean) requirements.getValue("st");
		boolean verbose = (boolean) requirements.getValue("verbose");

		data.inputFileName = input.equals("stdin") ? "<stdin>" : input;
		data.success = true;

		if (!verbose) {

			// === SILENT COMPILATION ===

			if (input.equals("stdin"))
				compiler.loadProgramFromStdin();
			else
				compiler.loadProgramFromFile(new File(input));

			compiler.pass1(data);
			compiler.pass2(data);

			if (data.success) {
				if (output.equals("stdout")) {
					out("The following memory dump is suitable for execution.%n-------");
					compiler.writeResultsToStdout(true);
				} else
					compiler.writeResultsToFile(new File(output));

				if (screen) {
					out("The following memory dump is NOT suitable for execution.%n-------");
					compiler.writeResultsToStdout(false);
					out("-------%n");
				}
			}

			if (st)
				out("Symbol Table:%n%s", compiler.symbolTable);

		} else {

			// === VERBOSE COMPILATION ===

			if (input.equals("stdin")) {
				out("Loading program from Standard Input");
				out("The line number for each statement will be printed");
				out("Type 'end' to stop inputting code");
				compiler.loadProgramFromStdin();
			} else {
				out("Loading program from file: %s", input);
				compiler.loadProgramFromFile(new File(input));
			}
			out("Progarm loading completed");

			out("Compilation started");
			compiler.pass1(data);

			out("Completing goto instructions");
			compiler.pass2(data);

			out("Compilation ended");

			if (data.success) {
				if (output.equals("stdout")) {
					out("Generated machine code (suitable for execution):");
					compiler.writeResultsToStdout(true);
				} else {
					out("Writing generated machine code to file: %s", output);
					compiler.writeResultsToFile(new File(output));
				}

				if (screen) {
					out("Generated machine code (NOT suitable for execution):");
					compiler.writeResultsToStdout(false);
				}
			}

			if (st)
				out("Symbol Table:%n%s", compiler.symbolTable);

			if (data.success)
				out("Compilation succeeded :)");
			else
				out("Compilation failed :(");
		}
	}

	/* Does everything apart from completing 'jump' instructions */
	private void pass1(CompilationData data) {

		memory.initializeForWriting();

		StringTokenizer lineTokenizer = new StringTokenizer(program.toString(), System.lineSeparator());

		String    line = "";
		String[]  tokens;
		Statement statement;

		next_line: while (lineTokenizer.hasMoreTokens()) {

			try {

				// get line and remove extra whitespace
				data.originalLine = lineTokenizer.nextToken();
				line = data.originalLine.strip().replaceAll("\\s+", " ");

				if (line.isBlank())
					continue next_line;

				tokens = line.split(" ");

				// handle line number
				String lineNo = tokens[0];
				if (!isNumber(lineNo))
					throw new InvalidLineNameException(lineNo);

				data.lineNumber = Integer.parseInt(lineNo);

				// handle command
				if (tokens.length == 1)
					throw new UnexpectedTokenException("", "a statement");

				statement = Statement.of(tokens[1]);

				// handle constructors (int etc. declarations)
				if (statement.isConstructor) {
					for (int i = 2, count = tokens.length; i < count; ++i) {
						String var = tokens[i];
						data.variable = var;

						if (statement == Statement.LABEL) {
							if (tokens.length > Statement.LABEL.length)
								throw new UnexpectedTokensException(data.originalLine.substring(find(data)));

							if (!isLabelName(var))
								throw new InvalidLabelNameException(var);

							if (labelDeclared(var))
								throw new LabelAlreadyDeclaredException(var);
						} else {
							if (!isVariableName(var))
								throw new InvalidVariableNameException(var);

							if (variableDeclared(var))
								throw new VariableAlreadyDeclaredException(var);
						}
					}

					statement.evaluate(line, this);
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
					int expectedCount = statement.length;

					if ((expectedCount != -1) && (expectedCount < tokens.length)) {
						data.variable = tokens[expectedCount];
						throw new UnexpectedTokensException(data.originalLine.substring(find(data)));
					}

					int tokensToScan = expectedCount == -1 ? tokens.length
							: expectedCount;

					for (int i = 2, count = tokensToScan; i < count; ++i) {
						String var = tokens[i];
						data.variable = var;

						// declare constants, assert variables and labels are declared
						if (isNumber(var)) {
							if (!constantDeclared(var))
								declareConstant(var, INT.identifier);

						} else if (isVariableName(var)) {
							if (!variableDeclared(var))
								throw new VariableNotDeclaredException(var);

						} else if (isLabelName(var)) {
							/*
							 * because goto instructions may jump to a label further down the program, it is
							 * not necessary for labels to be declared
							 */

						} else if (keywords.contains(var)) {
							// dismiss keywords

						} else {
							if (!statement.equals(LET))
								throw new RuntimeException("the fuck how did we get here");
						}
					}
				}

				/*
				 * at this point: - all variables are declared - all constants are set - all
				 * lines exist
				 */

				// statement-specific checks
				if (statement == Statement.END) {
					if (!blockStack.empty())
						throw new UnclosedBlockException(blockStack.pop());
				}

				// actually write machine code for each command
				statement.checkSyntax(line);
				statement.evaluate(line, this);

			} // end try (one statement / one line)
			catch (CompilerException e) {
				err("at: %s:%02d:%02d: %s", data.inputFileName, data.lineNumber,
						find(data), e.getMessage());
				data.success = false;
			}
		} // end of while
	} // end of pass1

	private void pass2(CompilationData data) {
		for (Entry<Integer, String> entry : labelFlags.entrySet()) {

			int    instructionAddress = entry.getKey();
			String labelToJump = entry.getValue();

			try {
				data.variable = labelToJump;

				if (!labelDeclared(labelToJump))
					throw new LabelNotDeclaredException(labelToJump);

				int location = getLabel(labelToJump).location;
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

	private void loadProgramFromStdin() {
		@SuppressWarnings("resource")
		Scanner scanner = new Scanner(System.in);
		program.setLength(0);

		String userInput = "";
		int    lineCount = 0;

		while (!userInput.equals("end")) {
			System.out.printf("%02d > ", lineCount);
			userInput = scanner.nextLine();

			if (!userInput.isBlank())
				program.append(String.format("%02d %s%n", lineCount, userInput));
			++lineCount;
		}
	}

	private void loadProgramFromFile(File file) {
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

	private void writeResultsToStdout(boolean suitableForExecution) {
		out("%n%s", suitableForExecution ? memory.list() : memory.listShort());
	}

	private void writeResultsToFile(File file) {
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
			writer.write(memory.list());

		} catch (IOException e) {
			err("Unexpected error while writing to file: %s", file);
		}
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
	int addInstruction(int instruction) {
		return memory.writeInstruction(instruction);
	}

	/**
	 * Writes a constant to the memory.
	 *
	 * @param constant the constant
	 *
	 * @return the location in memory where it was placed
	 */
	int addConstant(int constant) {
		return memory.writeConstant(constant);
	}

	/**
	 * Allocates an address for a variable.
	 *
	 * @return the address of the variable
	 */
	public int addVariable() {
		return memory.assignPlaceForVariable();
	}

	// --- 11 symbolt table wrapper-delegate methods ---

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
	int declareVariable(String symbol, String varType) {
		int location = addVariable();
		symbolTable.addEntry(symbol, VARIABLE, location, varType);
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
	int declareConstant(String symbol, String varType) {
		int location = addConstant(Integer.parseInt(symbol));
		symbolTable.addEntry(symbol, CONSTANT, location, varType);
		return location;
	}

	/**
	 * Declares a label by creating an Entry in the Symbol Table.
	 *
	 * @param symbol the label's symbol
	 *
	 * @return the location of the declared label in memory, the location of the
	 *         first instruction corresponding to this label
	 */
	int declareLabel(String symbol) {
		int location = memory.getInstructionCounter();
		symbolTable.addEntry(symbol, LABEL, location, "");
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
	boolean variableDeclared(String symbol) {
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
	boolean constantDeclared(String symbol) {
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
	boolean labelDeclared(String symbol) {
		return symbolTable.existsSymbol(symbol, LABEL);
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
	SymbolInfo getVariable(String symbol) {
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
	SymbolInfo getConstant(String symbol) {
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
	SymbolInfo getLabel(String symbol) {
		return symbolTable.getSymbol(symbol, LABEL);
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
	SymbolInfo getSymbol(String symbol) {
		return symbolTable.getSymbol(symbol, VARIABLE, CONSTANT);
	}

	/**
	 * Returns the {@code SymbolTable} that the compiler uses to keep track of the
	 * different symbols found in the high-level program.
	 *
	 * @return the Symbol Table
	 */
	SymbolTable getSymbolTable() {
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
	void setBranchLocation(int location, int address) {
		memory.write(location, memory.read(location) + address);
	}

	/**
	 * Sets the {@code label} in the high-level-program where the a branch
	 * instruction will jump to. The instruction is located at the {@code location}
	 * in memory. This method does NOT complete the actual branch instruction. It
	 * acts merely marks the label so that later, when the address of that label in
	 * the machine code is known, the branch instruction can be completed.
	 *
	 * @param location    the address of the instruction.
	 * @param labelToJump the label to jump
	 */
	void setLabelToJump(int location, String labelToJump) {
		labelFlags.put(location, labelToJump);
	}

	// --- 2 method for handling the stack of blocks ---

	/**
	 * Pushes a new {@code Block} to the stack.
	 *
	 * @param block the block
	 */
	void pushBlock(Block block) {
		blockStack.push(block);
	}

	/**
	 * Retrieves the most recently added {@code Block} from the stack.
	 *
	 * @return the block
	 */
	Block popBlock() {
		return blockStack.pop();
	}

	// --- 3 methods for defining variables, constants and labels ---

	private static boolean isVariableName(String var) {
		return var.matches("[a-zA-Z]\\w*") && !keywords.contains(var);
	}

	private static boolean isLabelName(String var) {
		return var.matches(":\\w*") && !keywords.contains(var);
	}

	private static boolean isNumber(String con) {
		return con.matches("[1-9]\\d*");
	}

	// --- idk really ---

	private static int find(CompilationData data) {
		return data.originalLine.indexOf(data.variable, 2);
	}
}

package compiler;

import static compiler.Statement.COMMENT;
import static compiler.Statement.INT;
import static compiler.Statement.LET;
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
import java.io.InputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.EmptyStackException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
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
import compiler.postfix.InfixToPostfix;
import compiler.postfix.Token;
import compiler.symboltable.SymbolInfo;
import compiler.symboltable.SymbolTable;
import compiler.symboltable.SymbolType;
import memory.CodeWriter;
import memory.Memory;
import requirement.requirements.AbstractRequirement;
import requirement.requirements.StringType;
import requirement.util.Requirements;
import utility.StreamSet;

/**
 * A Compiler for the high-level language. It defines the {@code static} method
 * {@link #compile} which generates machine code instructions for a high-level
 * language program. The Compiler is {@code stateless} meaning that no
 * information is stored between compilations and that an instance of a Compiler
 * is not necessary to compile a program. Each call to {@code compile} creates a
 * new Compiler instance therefore there aren't any synchronisation issues.
 * <p>
 * The compilation uses {@link requirement.requirements.AbstractRequirement
 * Requirements} in order to specify different parameters. They can be obtained
 * with the {@link #getRequirements()} method, which contains more information
 * about each individual Requirement.
 *
 * @author Alex Mandelias
 */
public class SML_Compiler {

	private static final Collection<String> keywords = new HashSet<>();

	static {
		SML_Compiler.keywords.addAll(Arrays.asList(Token.ADD.value, Token.SUB.value,
		        Token.MUL.value, Token.DIV.value, Token.POW.value, Token.MOD.value, "="));

		for (final Condition s : Condition.values())
			SML_Compiler.keywords.add(s.value);

		SML_Compiler.keywords.add("jumpto");
	}

	private final InputStream inputStream;
	private final PrintStream outputStream, errorStream;

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

	/** Constructs a Compiler with the "standard" in, out and error streams */
	public SML_Compiler() {
		this(new StreamSet());
	}

	/**
	 * Constructs a Compiler using a {@code StreamSet}, which can be obtained by
	 * calling the static {@link #streams()} method.
	 *
	 * @param streamset the set of Streams with which to construct the Compiler
	 *
	 * @see StreamSet
	 */
	public SML_Compiler(StreamSet streamset) {
		this(streamset.in, streamset.out, streamset.err);
	}

	/**
	 * Constructs a Compiler using the streams provided.
	 *
	 * @param in  the Compiler's Input Stream
	 * @param out the Compiler's Output Stream
	 * @param err the Compiler's Error Stream
	 */
	public SML_Compiler(InputStream in, PrintStream out, PrintStream err) {
		inputStream = in;
		outputStream = out;
		errorStream = err;

		symbolTable = new SymbolTable();
		memory = new Memory(256);
		program = new StringBuilder();
		labelFlags = new HashMap<>();
		blockStack = new Stack<>();
	}

	/**
	 * Uses the command line arguments to specify the parameters necessary to
	 * compile a high-level-language program, and then compiles it. Parameters
	 * starting with a single dash '-' are set to {@code true}. Parameters starting
	 * with a double dash '--' are set to whatever the next argument is.
	 * <p>
	 * The different parameters are documented in the {@link #getRequirements()}
	 * method.
	 *
	 * @param args the command line arguments
	 */
	public static void main(String[] args) {

		final Requirements reqs = SML_Compiler.getRequirements();

		final SML_Compiler compiler = new SML_Compiler();

		for (int i = 0, count = args.length; i < count; ++i)
			if (args[i].startsWith("--"))
				reqs.fulfil(args[i].substring(2), args[++i]);
			else if (args[i].startsWith("-"))
				reqs.fulfil(args[i].substring(1), true);
			else
				compiler.err(
				        "Invalid parameter: %s. Parameters must start with either one '-' or two '--' dashes.",
				        args[i]);

		compiler.compile(reqs);
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
		final Requirements reqs = new Requirements();

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
	 * Returns a {@code StreamSet} that can be passed as a parameter to construct a
	 * Compiler. The {@code StreamSet} can be configured with different input,
	 * output and error Streams for the Compiler to use instead of the "standard"
	 * in, out and err Streams of the {@code System} class.
	 *
	 * @return the StreamSet
	 *
	 * @see StreamSet
	 */
	public static StreamSet streams() {
		return new StreamSet();
	}

	/**
	 * Uses the parameters from the {@code requirements} in order to load the
	 * program, compile it and output the results.
	 * <p>
	 * The different Requirements are documented in the {@link #getRequirements()}
	 * method.
	 *
	 * @param requirements the parameters needed to compile
	 */
	public void compile(Requirements requirements) {
		if (!requirements.fulfilled()) {
			for (final AbstractRequirement r : requirements)
				if (!r.fulfilled())
					err("No value for parameter '%s' found", r.key());

			err("Compilation couldn't start due to missing parameters");
			return;
		}

		final CompilationData data = new CompilationData();

		final String  input   = (String) requirements.getValue("input");
		final String  output  = (String) requirements.getValue("output");
		final boolean screen  = (boolean) requirements.getValue("screen");
		final boolean st      = (boolean) requirements.getValue("st");
		final boolean verbose = (boolean) requirements.getValue("verbose");

		data.inputFileName = input.equals("stdin") ? "<stdin>" : input;
		data.success = true;

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
				} else
					writeResultsToFile(new File(output));

				if (screen) {
					out(
					        "The following memory dump is NOT suitable for execution.%n-------");
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
					out("Generated machine code (suitable for execution):");
					writeResultsToStdout(true);
				} else {
					out("Writing generated machine code to file: %s", output);
					writeResultsToFile(new File(output));
				}

				if (screen) {
					out("Generated machine code (NOT suitable for execution):");
					writeResultsToStdout(false);
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
	private void pass1(CompilationData data) {

		memory.initializeForWriting();

		final StringTokenizer lineTokenizer = new StringTokenizer(program.toString(),
		        System.lineSeparator());

		String    line = "";
		String[]  tokens;
		Statement statement;

		next_line: while (lineTokenizer.hasMoreTokens())
			try {

				// get line and remove extra whitespace
				data.originalLine = lineTokenizer.nextToken();
				line = data.originalLine.strip().replaceAll("\\s+", " ");

				if (line.isBlank())
					continue next_line;

				tokens = line.split(" ");

				// handle line number
				final String lineNo = tokens[0];
				data.variable = lineNo;
				if (!SML_Compiler.isLine(lineNo))
					throw new InvalidLineNameException(lineNo);

				data.lineNumber = Integer.parseInt(lineNo);

				// handle command
				if (tokens.length == 1)
					throw new UnexpectedTokenException("", "a statement");

				statement = Statement.of(tokens[1]);

				// handle constructors (int etc. declarations)
				if (statement.isConstructor) {
					for (int i = 2, count = tokens.length; i < count; ++i) {
						final String var = tokens[i];
						data.variable = var;

						if (statement == Statement.LABEL) {
							if (tokens.length > Statement.LABEL.length)
								throw new UnexpectedTokensException(
								        data.originalLine.substring(SML_Compiler.find(data)));

							if (!SML_Compiler.isLabelName(var))
								throw new InvalidLabelNameException(var);

							if (labelDeclared(var))
								throw new LabelAlreadyDeclaredException(var);
						} else {
							if (!SML_Compiler.isVariableName(var))
								throw new InvalidVariableNameException(var);

							if (variableDeclared(var))
								throw new VariableAlreadyDeclaredException(var);
						}
					}

					statement.evaluate(line, this);
					continue next_line;
				}
				if (!statement.equals(COMMENT)) {
					final int expectedCount = statement.length;

					if (expectedCount != -1) {
						if (expectedCount < tokens.length) {
							data.variable = tokens[expectedCount];
							throw new UnexpectedTokensException(
							        data.originalLine.substring(SML_Compiler.find(data)));
						}
						if (expectedCount > tokens.length) {
							data.variable = tokens[tokens.length - 1];
							throw new UnexpectedTokenException("", "more tokens");
						}
					}

					// obtain tokens to scan according to statement (LET or otherwise)

					final String[] tokensToScan;
					if (statement.equals(LET)) {
						final List<Token> infixTokens = InfixToPostfix
						        .convertToPostfix(line.split("=")[1]);
						infixTokens.removeIf(Token::isOperatorOrParenthesis);

						tokensToScan = new String[infixTokens.size()];
						int i = 0;
						for (final Token token : infixTokens) {
							tokensToScan[i] = token.value;
							++i;
						}
					} else {
						tokensToScan = new String[tokens.length - 2];
						System.arraycopy(tokens, 2, tokensToScan, 0, tokensToScan.length);
					}

					next_token: for (int i = 0, count = tokensToScan.length; i < count; ++i) {
						final String var = tokensToScan[i];
						data.variable = var;

						if (SML_Compiler.keywords.contains(var))
							continue next_token;

						final SymbolType type = SymbolType.typeOf(var);

						// declare constants, assert variables and labels are declared
						switch (type) {
						case CONSTANT:
							if (!constantDeclared(var))
								declareConstant(var, INT.identifier);
							break;
						case VARIABLE:
							if (!variableDeclared(var))
								throw new VariableNotDeclaredException(var);
							break;
						case LABEL:
							/*
							 * because goto instructions may jump to a label further down the
							 * program, it is not necessary for labels to be declared
							 */
							break;
						default:
							break;
						}
					}
				}

				// at this point are variables and constants are declared

				// statement-specific checks
				if ((statement == Statement.END) && !blockStack.empty())
					throw new UnclosedBlockException(blockStack.pop());

				// actually write machine code for each command
				statement.checkSyntax(line);
				statement.evaluate(line, this);

			} // end try (one statement / one line)
			catch (final CompilerException e) {
				err("at: %s:%02d:%02d: %s", data.inputFileName, data.lineNumber,
				        SML_Compiler.find(data), e.getMessage());
				data.success = false;
			} catch (final EmptyStackException e1) {

			}
	} // end of pass1

	private void pass2(CompilationData data) {
		for (final Entry<Integer, String> entry : labelFlags.entrySet()) {

			final int    instructionAddress = entry.getKey();
			final String labelToJump        = entry.getValue();

			try {
				data.variable = labelToJump;

				if (!labelDeclared(labelToJump))
					throw new LabelNotDeclaredException(labelToJump);

				final int location    = getLabel(labelToJump).location;
				final int instruction = memory.read(instructionAddress);
				memory.write(instructionAddress, instruction + location);

			} catch (final CompilerException e) {
				err("at: %s:%02d:%02d: %s", data.inputFileName, data.lineNumber,
				        SML_Compiler.find(data), e.getMessage());
				data.success = false;
			}
		}
	}

	// --- 5 methods for input, output and reset ---

	private void loadProgramFromStdin() {
		@SuppressWarnings("resource")
		final Scanner scanner = new Scanner(inputStream);
		program.setLength(0);

		String userInput = "";
		int    lineCount = 0;

		while (!userInput.equals("end")) {
			msg("%02d > ", lineCount);
			userInput = scanner.nextLine();

			if (!userInput.isBlank())
				program.append(String.format("%02d %s%n", lineCount, userInput));
			++lineCount;
		}
	}

	private void loadProgramFromFile(File file) {
		program.setLength(0);
		final String lineSep = System.lineSeparator();

		try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
			for (String line = reader.readLine(); line != null; line = reader.readLine())
				program.append(line).append(lineSep);

		} catch (final FileNotFoundException e) {
			err("Couldn't find file: %s", file);
		} catch (final IOException e) {
			err("Unexpected error while reading from file: %s", file);
		}
	}

	private void writeResultsToStdout(boolean suitableForExecution) {
		out("%n%s", suitableForExecution ? memory.list() : memory.listShort());
	}

	private void writeResultsToFile(File file) {
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
			writer.write(memory.list());

		} catch (final IOException e) {
			err("Unexpected error while writing to file: %s", file);
		}
	}

	// --- 3 method for uniform message printing ---

	private void msg(String format, Object... args) {
		outputStream.printf(format, args);
	}

	private void out(String format, Object... args) {
		outputStream.printf("Compilation Info:  %s%n", String.format(format, args));
	}

	private void err(String format, Object... args) {
		errorStream.printf("Compilation Error: %s%n", String.format(format, args));
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
		final int location = addVariable();
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
		final int location = addConstant(Integer.parseInt(symbol));
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
		final int location = memory.getInstructionCounter();
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
	 *
	 * @throws EmptyStackException if the stack of Blocks is empty
	 */
	Block popBlock() {
		return blockStack.pop();
	}

	// --- 3 methods for defining variables, constants and labels ---

	private static boolean isVariableName(String var) {
		return (SymbolType.typeOf(var) == VARIABLE) && !SML_Compiler.keywords.contains(var);
	}

	private static boolean isLabelName(String var) {
		return (SymbolType.typeOf(var) == LABEL) && !SML_Compiler.keywords.contains(var);
	}

	private static boolean isLine(String con) {
		return con.matches("\\d+");
	}

	// --- idk really ---

	private static int find(CompilationData data) {
		return data.originalLine.indexOf(data.variable, 2);
	}
}

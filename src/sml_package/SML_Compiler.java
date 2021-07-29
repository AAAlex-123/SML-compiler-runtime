package sml_package;

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
import java.util.HashMap;
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

	private static final String[] keyWords     = new String[] { COMMENT, INPUT, IF, GOTO, LET,
	        PRINT, END,
	        IFGOTO, ELSE, ENDIF, WHILE, ENDWHILE, NOOP, DUMP,
	        "+", "-", "*", "/", "^", "%", "=",
	        "==", "<=", ">=", "!=", "<", ">" };
	private static final char[]   valid        = new char[] { 'a', 'b', 'c', 'd', 'e', 'f', 'g',
	        'h', 'i', 'j',
	        'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't',
	        'u', 'v', 'w', 'x', 'y', 'z', '_' };
	private static final String[] constructors = new String[] { INT };

	public static void main(String[] args) {

		Requirements reqs = getRequirements();

		for (int i = 0; i < args.length; ++i) {
			if (args[i].startsWith("--"))
				reqs.fulfil(args[i], args[++i]);
			else if (args[i].startsWith("-"))
				reqs.fulfil(args[i], true);
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

		System.out.println("*** Starting compilation\t\t\t ***");

		StringTokenizer tok = new StringTokenizer(program.toString(), "\n");

		String   originalLine;
		String   line = "";
		String[] tokens;

		while (!line.equals("99 end")) {

			// get line and remove extra whitespace
			try {
				originalLine = tok.nextToken().strip().replace("\s+", " ");
			} catch (NoSuchElementException e) {
				System.err.printf("error at: %s:\t\t EOF reached; no '99 end' command found\n", inputFileName);
				return 1;
			}

			if (line.isBlank())
				continue;

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
			if (isConstructor(command)) {
				for (String constructor : constructors) {
					if (command.matches(constructor)) {
						String variable;
						int tokensToScan = lengths.get(command) == -1 ? tokens.length : lengths.get(command);
						for (int i=2; i<tokensToScan; i++) {
							variable = tokens[i];
							if (isVariable(variable)) {
								boolean isDeclared = symbolTable.existsSymbol(variable, VARIABLE);
								if (isDeclared) {
									System.err.printf(
									        "error at: %s:%02d:%02d: variable '%s' already declared\n",
									        inputFileName, line_count, find(originalLine, variable),
									        variable);
									succ = false;
								} else {
									if (command.matches(INT)) {
										int location = addVariable();
										symbolTable.addEntry(variable, VARIABLE, location, command);
									}
									// future: add other types
								}
							} else if (isNumber(variable)) {
								System.err.printf(
								        "error at: %s:%02d:%02d: can't declare constant '%s' as variable\n",
								        inputFileName, line_count, find(originalLine, variable),
								        variable);
								succ = false;
							} else if (Arrays.asList(keyWords).contains(variable)) {
								System.err.printf(
								        "error at: %s:%02d:%02d: variable name '%s' is reserved\n",
								        inputFileName, line_count, find(originalLine, variable),
								        variable);
								succ = false;
							} else if (!isValidName(variable)) {
								System.err.printf(
								        "error at: %s:%02d:%02d: variable '%s' has invalid name\n",
								        inputFileName, line_count, find(originalLine, variable),
								        variable);
								succ = false;
							} else {
								System.err.printf("%s:%02d:\t error: hmmm there is an error here... (pls let me know)\n", inputFileName, line_count, variable);
								succ = false;
							}
						}
					}
				}
				continue;
			}
			// handle non-constructors
			// if not comment, assert every variable is declared and declare constants if needed
			if (!command.equals(COMMENT)) {
				int tokensToScan = lengths.get(command) == -1 ? tokens.length : lengths.get(command);
				for (int i=2; i<tokensToScan; i++) {
					String token = tokens[i];
					// declare constants
					if (isNumber(token)) {
						if (!symbolTable.existsSymbol(token, CONSTANT)) {
							int location = addVariable();
							symbolTable.addEntry(token, CONSTANT, location, "int");
							addConstant(Integer.parseInt(token));
							// future: get type and add with its type
						}
					// assert variable is declared
					} else if (isVariable(token)){
						if (!symbolTable.existsSymbol(token, VARIABLE)) {
							System.err.printf(
							        "error at: %s:%02d:%02d: variable '%s' not declared\n",
							        inputFileName, line_count, find(originalLine, token), token);
							succ = false;
						}
					} else if (Arrays.asList(keyWords).contains(token)) {
						;
					} else if (!isValidName(token)) {
						System.err.printf(
						        "error at: %s:%02d:%02d: variable '%s' has invalid name\n",
						        inputFileName, line_count, find(originalLine, token), token);
						succ = false;
					} else {
						System.err.printf("%s:%02d:\t error: hmmm there is an error here... (pls let me know)\n", inputFileName, line_count, token);
						succ = false;
					}
				}
			}

			// check for excessive stuff
			if (tokens.length > lengths.get(command)) {
				System.err.printf("error at: %s:%02d:%02d: unexpected stuff '%s'\n", inputFileName,
				        line_count, find(originalLine, tokens[lengths.get(command)]),
				        originalLine.substring(find(originalLine, tokens[lengths.get(command)])));
				succ = false;
			}

			// actually write machine code for each command
			if (command.equals(DUMP)) {
				addInstruction(Command.DUMP.opcode());

			} else if (command.equals(NOOP)) {

			} else if (command.equals(COMMENT)) {
				;
			} else if (command.equals(INPUT)) {
				String[] vars = line.substring(9).split(" ");
				for (String var : vars) {
					if (isNumber(var)) {
						System.err.printf("error at: %s:%02d:%02d: can't input to constant '%s'\n",
						        inputFileName, line_count, find(originalLine, var), var);
						succ = false;
					} else if (isVariable(var)) {
						int loc = symbolTable.getSymbolLocation(var, VARIABLE);
						addInstruction(Command.READ_INT.opcode() + loc);

					} else {
						;
					}
				}

			} else if (command.equals(LET)) {
				String var = tokens[2];
				if (isNumber(var)) {
					System.err.printf("error at: %s:%02d:%02d: can't assign to constant '%s'\n",
					        inputFileName, line_count, find(originalLine, var), var);
					succ = false;
				} else if (isVariable(var)){
					int loc = symbolTable.getSymbolLocation(var, VARIABLE);

					String infix = line.split("=")[1];
					PostfixEvaluator.evaluatePostfix(InfixToPostfix.convertToPostfix(infix));

					addInstruction(Command.STORE.opcode() + loc);
				} else {
					;
				}

			} else if (command.equals(PRINT)) {
				String[] vars = line.substring(9).split(" ");
				for (String var : vars) {
					if (isVariable(var) || isNumber(var)) {
						int    loc     = symbolTable.getSymbolLocation(var, CONSTANT, VARIABLE);
						String varType = symbolTable.getVarType(var);

						// future: print according to type
						if (varType.equals("int")) {
							addInstruction(Command.WRITE_NL.opcode() + loc);
						} else {
							System.out.printf("%s:%02d:\t error: variable %s has unknown type %s\n", inputFileName, line_count, var, varType);
							succ = false;
						}
					} else {
						;
					}
				}

			} else if (command.equals(GOTO)) {
				int target_line = Integer.parseInt(tokens[2]);

				int location = addInstruction(Command.BRANCH.opcode());
				ifgFlags[location] = target_line;

			} else if (command.equals(IFGOTO)) {
				int target_line = Integer.parseInt(tokens[6]);
				int loc1, loc2;

				String op1 = tokens[2];
				loc1 = symbolTable.getSymbolLocation(op1, CONSTANT, VARIABLE);

				String op2 = tokens[4];
				loc2 = symbolTable.getSymbolLocation(op2, CONSTANT, VARIABLE);

				String condition = tokens[3];
				if (condition.equals("<")) {
					addInstruction(Command.LOAD.opcode() + loc1);
					addInstruction(Command.SUBTRACT.opcode() + loc2);
					int location = addInstruction(Command.BRANCHNEG.opcode());
					ifgFlags[location] = target_line;
				} else if (condition.equals(">")) {
					addInstruction(Command.LOAD.opcode() + loc2);
					addInstruction(Command.SUBTRACT.opcode() + loc1);
					int location = addInstruction(Command.BRANCHNEG.opcode());
					ifgFlags[location] = target_line;
				} else if (condition.equals("<=")) {
					addInstruction(Command.LOAD.opcode() + loc1);
					addInstruction(Command.SUBTRACT.opcode() + loc2);
					int location = addInstruction(Command.BRANCHNEG.opcode());
					ifgFlags[location] = target_line;
					location = addInstruction(Command.BRANCHZERO.opcode());
					ifgFlags[location] = target_line;
				} else if (condition.equals(">=")) {
					addInstruction(Command.LOAD.opcode() + loc2);
					addInstruction(Command.SUBTRACT.opcode() + loc1);
					int location = addInstruction(Command.BRANCHNEG.opcode());
					ifgFlags[location] = target_line;
					location = addInstruction(Command.BRANCHZERO.opcode());
					ifgFlags[location] = target_line;
				}else if(condition.equals("==")) {
					addInstruction(Command.LOAD.opcode() + loc1);
					addInstruction(Command.SUBTRACT.opcode() + loc2);
					int location = addInstruction(Command.BRANCHZERO.opcode());
					ifgFlags[location] = target_line;
				} else if(condition.equals("!=")) {
					addInstruction(Command.LOAD.opcode() + loc1);
					addInstruction(Command.SUBTRACT.opcode() + loc2);
					int location = addInstruction(Command.BRANCHZERO.opcode());
					ifgFlags[location] = -line_count;
					location = addInstruction(Command.BRANCH.opcode());
					ifgFlags[location] = target_line;
				} else {
					System.out.println("Error: invalid if condition");
				}
			} else if (command.equals(IF)) {
				String op1, op2;
				int loc1, loc2;

				op1 = tokens[2];
				loc1 = symbolTable.getSymbolLocation(op1, CONSTANT, VARIABLE);

				op2 = tokens[4];
				loc2 = symbolTable.getSymbolLocation(op2, CONSTANT, VARIABLE);
				String condition = tokens[3];
				if (condition.equals("<")) {
					addInstruction(Command.LOAD.opcode() + loc2);
					addInstruction(Command.SUBTRACT.opcode() + loc1);
					int location = addInstruction(Command.BRANCHZERO.opcode());
					ifFlags[line_count][0] = location;
					location = addInstruction(Command.BRANCHNEG.opcode());
					ifFlags[line_count][1] = location;
				} else if (condition.equals(">")) {
					addInstruction(Command.LOAD.opcode() + loc1);
					addInstruction(Command.SUBTRACT.opcode() + loc2);
					int location = addInstruction(Command.BRANCHZERO.opcode());
					ifFlags[line_count][0] = location;
					location = addInstruction(Command.BRANCHNEG.opcode());
					ifFlags[line_count][1] = location;
				} else if (condition.equals("<=")) {
					addInstruction(Command.LOAD.opcode() + loc2);
					addInstruction(Command.SUBTRACT.opcode() + loc1);
					int location = addInstruction(Command.BRANCHNEG.opcode());
					ifFlags[line_count][0] = location;
				} else if (condition.equals(">=")) {
					addInstruction(Command.LOAD.opcode() + loc1);
					addInstruction(Command.SUBTRACT.opcode() + loc2);
					int location = addInstruction(Command.BRANCHNEG.opcode());
					ifFlags[line_count][0] = location;
				} else if (condition.equals("==")) {
					addInstruction(Command.LOAD.opcode() + loc1);
					int location1 = addInstruction(Command.SUBTRACT.opcode() + loc2);
					addInstruction(Command.BRANCHZERO.opcode() + location1 + 1);
					int location = addInstruction(Command.BRANCH.opcode());
					ifFlags[line_count][1] = location;
				} else if (condition.equals("!=")) {
					addInstruction(Command.LOAD.opcode() + loc1);
					addInstruction(Command.SUBTRACT.opcode() + loc2);
					int location = addInstruction(Command.BRANCHZERO.opcode());
					ifFlags[line_count][0] = location;
				} else {
					System.out.println("Error: invalid if condition");
				}

			} else if (command.equals(ELSE)) {
				int location = addInstruction(Command.BRANCH.opcode());
				ifFlags[line_count][0] = location;

				String target_line = tokens[2];
				for (int i=0; i<ifFlags[0].length; i++) {
					int branch_loc = ifFlags[Integer.parseInt(target_line)][i];
					if (branch_loc != -1) {
						int location1 = symbolTable.getSymbolLocation(String.valueOf(line_count))
						        + 1;
						memory.write(branch_loc, memory.read(branch_loc) + location1);
					}
				}

			} else if (command.equals(ENDIF)) {
				String target_line = tokens[2];
				for (int i=0; i<ifFlags[0].length; i++) {
					int branch_loc = ifFlags[Integer.parseInt(target_line)][i];
					if (branch_loc != -1) {
						int location = symbolTable.getSymbolLocation(String.valueOf(line_count));
						memory.write(branch_loc, memory.read(branch_loc) + location);
					}
				}

			} else if (command.equals(WHILE)) {
				String op1, op2;
				int loc1, loc2;

				op1 = tokens[2];
				loc1 = symbolTable.getSymbolLocation(op1, CONSTANT, VARIABLE);

				op2 = tokens[4];
				loc2 = symbolTable.getSymbolLocation(op2, CONSTANT, VARIABLE);
				String condition = tokens[3];
				if (condition.equals("<")) {
					addInstruction(Command.LOAD.opcode() + loc2);
					addInstruction(Command.SUBTRACT.opcode() + loc1);
					int location = addInstruction(Command.BRANCHZERO.opcode());
					whileFlags[line_count][0] = location;
					location = addInstruction(Command.BRANCHNEG.opcode());
					whileFlags[line_count][1] = location;
				} else if (condition.equals(">")) {
					addInstruction(Command.LOAD.opcode() + loc1);
					addInstruction(Command.SUBTRACT.opcode() + loc2);
					int location = addInstruction(Command.BRANCHZERO.opcode());
					whileFlags[line_count][0] = location;
					location = addInstruction(Command.BRANCHNEG.opcode());
					whileFlags[line_count][1] = location;
				} else if (condition.equals("<=")) {
					addInstruction(Command.LOAD.opcode() + loc2);
					addInstruction(Command.SUBTRACT.opcode() + loc1);
					int location = addInstruction(Command.BRANCHNEG.opcode());
					whileFlags[line_count][0] = location;
				} else if (condition.equals(">=")) {
					addInstruction(Command.LOAD.opcode() + loc1);
					addInstruction(Command.SUBTRACT.opcode() + loc2);
					int location = addInstruction(Command.BRANCHNEG.opcode());
					whileFlags[line_count][0] = location;
				} else if (condition.equals("==")) {
					addInstruction(Command.LOAD.opcode() + loc1);
					int location1 = addInstruction(Command.SUBTRACT.opcode() + loc2);
					int location  = addInstruction(Command.BRANCHZERO.opcode() + location1 + 1);
					whileFlags[line_count][0] = location;
					location = addInstruction(Command.BRANCH.opcode());
					whileFlags[line_count][1] = location;
				} else if (condition.equals("!=")) {
					addInstruction(Command.LOAD.opcode() + loc1);
					addInstruction(Command.SUBTRACT.opcode() + loc2);
					int location = addInstruction(Command.BRANCHZERO.opcode());
					whileFlags[line_count][0] = location;
				} else {
					System.out.println("Error: invalid if condition");
				}
			} else if (command.equals(ENDWHILE)) {
				int location = symbolTable.getSymbolLocation(tokens[2], LINE);
				addInstruction(Command.BRANCH.opcode() + location);

				String target_line = tokens[2];
				for (int i=0; i<whileFlags[0].length; i++) {

					int branchLocation = whileFlags[Integer.parseInt(target_line)][i];

					if ((branchLocation != -1) && ((memory.read(branchLocation) % 0x100) == 0)) {
						int location1 = symbolTable.getSymbolLocation(String.valueOf(line_count))
						        + 1;
						memory.write(branchLocation, memory.read(branchLocation) + location1);
					}
				}

			} else if (command.equals(END)) {
				addInstruction(Command.HALT.opcode());
			} else {
				if (!Arrays.asList(constructors).contains(command))
					System.out.printf("CompilationError: Unknown command %s", command);
			}
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

		while (!userInput.equals("99 end")) {
			System.out.printf("%02d > ", lineCount);
			userInput = scanner.nextLine();

			program.append(userInput);
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

	private static boolean isVariable(String var) {
		try {
			Integer.parseInt(var);
			return false;
		} catch (NumberFormatException e) {
			return !Arrays.asList(keyWords).contains(var) && isValidName(var);
		}
	}

	private static boolean isNumber(String con) {
		try {
			Integer.parseInt(con, 10);
			return true;
		} catch (NumberFormatException e) {
			return false;
		}
	}

	private static boolean isValidName(String name) {
		if (('0' <= name.charAt(0)) && (name.charAt(0) <= '9'))
			return false;
		for (int i = 0; i < name.length(); i++) {
			boolean flag = false;
			for (int j = 0; j < valid.length; j++)
				flag = (name.charAt(i) == valid[j]) || flag;
			if (!flag)
				return false;
		}
		return true;
	}

	private static boolean isConstructor(String com) {
		for (int i = 0; i < constructors.length; i++)
			if (com.equals(constructors[i]))
				return true;
		return false;
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

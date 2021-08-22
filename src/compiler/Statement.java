package compiler;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import compiler.blocks.Block;
import compiler.blocks.IfBlock;
import compiler.blocks.WhileBlock;
import compiler.exceptions.CompilerException;
import compiler.exceptions.InvalidConditionException;
import compiler.exceptions.InvalidStatementException;
import compiler.exceptions.UnexpectedTokenException;
import compiler.postfix.InfixToPostfix;
import compiler.postfix.PostfixEvaluator;
import compiler.postfix.Token;
import compiler.symboltable.SymbolInfo;
import compiler.symboltable.SymbolTable;
import compiler.symboltable.SymbolType;
import runtime.Instruction;

/**
 * A collection of the different statements that are supported by the high-level
 * language. Each statement evaluates a line of high-level code assuming both
 * that it is syntactically correct and that all variables and constants are
 * correctly declared.
 *
 * @author Alex Mandelias
 */
enum Statement {

	/** Comment */
	COMMENT("//", -1, false) {
		@Override
		public void evaluate(String line, SML_Compiler compiler) {

		}

		@Override
		public void checkSyntax(String line) throws CompilerException {

		}
	},

	/** Declaration of integer variables */
	INT("int", -1, true) {
		@Override
		public void evaluate(String line, SML_Compiler compiler) {
			for (String var : vars(line))
				compiler.declareVariable(var, identifier);
		}

		@Override
		public void checkSyntax(String line) throws CompilerException {
			for (String var : vars(line))
				SymbolType.assertType(var, SymbolType.VARIABLE);
		}

		private String[] vars(String line) {
			return Arrays.asList(line.split(" ")).subList(2, line.split(" ").length).toArray(String[]::new);
		}
	},

	/** Prompt the user for input */
	INPUT("input", -1, false) {
		@Override
		public void evaluate(String line, SML_Compiler compiler) {
			for (final String var : vars(line)) {
				final int location = compiler.getVariable(var).location;
				compiler.addInstruction(Instruction.READ_INT.opcode() + location);
			}
		}

		@Override
		public void checkSyntax(String line) throws CompilerException {
			for (String var : vars(line))
				SymbolType.assertType(var, SymbolType.VARIABLE);
		}

		public String[] vars(String line) {
			return Arrays.asList(line.split(" ")).subList(2, line.split(" ").length).toArray(String[]::new);
		}
	},

	/** Evaluate an expression and assign to variable */
	LET("let", -1, false) {
		@Override
		public void evaluate(String line, SML_Compiler compiler) {
			// generate postfix
			final String      infix   = infix(line);
			final List<Token> postfix = InfixToPostfix.convertToPostfix(infix);
			final SymbolTable symbolTable = compiler.getSymbolTable();
			final List<Integer> instructions = PostfixEvaluator.evaluatePostfix(postfix,
					symbolTable, compiler);

			// write instructions for evaluating postfix
			for (final int instruction : instructions)
				compiler.addInstruction(instruction);

			// store result of postfix that is loaded after the last instruction of evaluation
			final String var   = var(line);
			final int location = compiler.getVariable(var).location;

			compiler.addInstruction(Instruction.STORE.opcode() + location);
		}

		@Override
		public void checkSyntax(String line) throws CompilerException {
			String[] tokens = line.split(" ");

			SymbolType.assertType(tokens[2], SymbolType.VARIABLE);

			if (!tokens[3].equals("="))
				throw new UnexpectedTokenException(tokens[3], "'='");

			final List<Token> postfix = InfixToPostfix.convertToPostfix(infix(line));

			for (Token token : postfix) {
				if (!token.isOperator() || (token == Token.LEFT_PAREN) || (token == Token.RIGHT_PAREN)) {
					SymbolType.assertNot(token.value, SymbolType.LABEL);
				}
			}
		}

		private String var(String line) {
			return line.split(" ")[2];
		}

		private String infix(String line) {
			return line.split("=")[1];
		}
	},

	/** Print the value of a variable to the screen */
	PRINT("print", -1, false) {
		@Override
		public void evaluate(String line, SML_Compiler compiler) {
			for (final String var : vars(line)) {
				final SymbolInfo info = compiler.getSymbol(var);

				final String varType  = info.varType;
				final int    location = info.location;

				if (varType.equals(INT.identifier))
					compiler.addInstruction(Instruction.WRITE_NL.opcode() + location);
			}
		}

		@Override
		public void checkSyntax(String line) throws CompilerException {
			for (String var : vars(line))
				SymbolType.assertNot(var, SymbolType.LABEL);
		}

		private String[] vars(String line) {
			return Arrays.asList(line.split(" ")).subList(2, line.split(" ").length).toArray(String[]::new);
		}
	},

	/** Define a label (location to jump to) */
	LABEL("label", 3, true) {
		@Override
		public void evaluate(String line, SML_Compiler compiler) {
			compiler.declareLabel(labelToJump(line));
		}

		@Override
		public void checkSyntax(String line) throws CompilerException {
			SymbolType.assertType(labelToJump(line), SymbolType.LABEL);
		}

		private String labelToJump(String line) {
			return line.split(" ")[2];
		}
	},

	/** Unconditional jump to line */
	GOTO("goto", 3, false) {
		@Override
		public void evaluate(String line, SML_Compiler compiler) {
			final int location = compiler.addInstruction(Instruction.BRANCH.opcode());
			final String labelToJump = labelToJump(line);

			compiler.setLabelToJump(location, labelToJump);
		}

		@Override
		public void checkSyntax(String line) throws CompilerException {
			SymbolType.assertType(labelToJump(line), SymbolType.LABEL);
		}

		private String labelToJump(String line) {
			return line.split(" ")[2];
		}
	},

	/** Conditional jump to line */
	IFGOTO("ifg", 7, false) {
		@Override
		public void evaluate(String line, SML_Compiler compiler) {

			int       loc1, loc2;
			Condition condition;
			String labelToJump;

			loc1 = compiler.getSymbol(op1(line)).location;

			try {
				condition = Condition.of(condition(line));
			} catch (InvalidConditionException e) {
				// checkSyntax was called, this should never throw
				throw new RuntimeException(e);
			}

			loc2 = compiler.getSymbol(op2(line)).location;

			labelToJump = labelToJump(line);

			int location;
			switch (condition) {
			case LT:
				compiler.addInstruction(Instruction.LOAD.opcode() + loc1);
				compiler.addInstruction(Instruction.SUBTRACT.opcode() + loc2);
				location = compiler.addInstruction(Instruction.BRANCHNEG.opcode());
				compiler.setLabelToJump(location, labelToJump);
				break;
			case GT:
				compiler.addInstruction(Instruction.LOAD.opcode() + loc2);
				compiler.addInstruction(Instruction.SUBTRACT.opcode() + loc1);
				location = compiler.addInstruction(Instruction.BRANCHNEG.opcode());
				compiler.setLabelToJump(location, labelToJump);
				break;
			case LE:
				compiler.addInstruction(Instruction.LOAD.opcode() + loc1);
				compiler.addInstruction(Instruction.SUBTRACT.opcode() + loc2);
				location = compiler.addInstruction(Instruction.BRANCHNEG.opcode());
				compiler.setLabelToJump(location, labelToJump);
				location = compiler.addInstruction(Instruction.BRANCHZERO.opcode());
				compiler.setLabelToJump(location, labelToJump);
				break;
			case GE:
				compiler.addInstruction(Instruction.LOAD.opcode() + loc2);
				compiler.addInstruction(Instruction.SUBTRACT.opcode() + loc1);
				location = compiler.addInstruction(Instruction.BRANCHNEG.opcode());
				compiler.setLabelToJump(location, labelToJump);
				location = compiler.addInstruction(Instruction.BRANCHZERO.opcode());
				compiler.setLabelToJump(location, labelToJump);
				break;
			case EQ:
				compiler.addInstruction(Instruction.LOAD.opcode() + loc1);
				compiler.addInstruction(Instruction.SUBTRACT.opcode() + loc2);
				location = compiler.addInstruction(Instruction.BRANCHZERO.opcode());
				compiler.setLabelToJump(location, labelToJump);
				break;
			case NE:
				compiler.addInstruction(Instruction.LOAD.opcode() + loc1);
				compiler.addInstruction(Instruction.SUBTRACT.opcode() + loc2);
				location = compiler.addInstruction(Instruction.BRANCHZERO.opcode());
				compiler.setLabelToJump(location, labelToJump);
				location = compiler.addInstruction(Instruction.BRANCH.opcode());
				compiler.setLabelToJump(location, labelToJump);
				break;
			default:
				break;
			}
		}

		@Override
		public void checkSyntax(String line) throws CompilerException {
			SymbolType.assertNot(op1(line), SymbolType.LABEL);
			SymbolType.assertNot(op2(line), SymbolType.LABEL);
			SymbolType.assertType(labelToJump(line), SymbolType.LABEL);
			Condition.of(condition(line));

			String goto_ = line.split(" ")[5];
			if (!goto_.split(" ")[5].equals("goto"))
				throw new UnexpectedTokenException(goto_, "goto");
		}

		private String op1(String line) {
			return line.split(" ")[2];
		}

		private String condition(String line) {
			return line.split(" ")[3];
		}

		private String op2(String line) {
			return line.split(" ")[4];
		}

		private String labelToJump(String line) {
			return line.split(" ")[6];
		}
	},

	/** Beginning of {@code if} block */
	IF("if", 5, false) {
		@Override
		public void evaluate(String line, SML_Compiler compiler) {

			final IfBlock block = new IfBlock();

			int       loc1, loc2;
			Condition condition;

			loc1 = compiler.getSymbol(op1(line)).location;

			try {
				condition = Condition.of(condition(line));
			} catch (InvalidConditionException e) {
				// checkSyntax was called, this should never throw
				throw new RuntimeException(e);
			}

			loc2 = compiler.getSymbol(op2(line)).location;

			int location = 0;
			switch (condition) {
			case LT:
				compiler.addInstruction(Instruction.LOAD.opcode() + loc1);
				location = compiler.addInstruction(Instruction.SUBTRACT.opcode() + loc2);
				compiler.addInstruction(Instruction.BRANCHNEG.opcode() + location + 3);
				location = compiler.addInstruction(Instruction.BRANCH.opcode());
				break;
			case GT:
				compiler.addInstruction(Instruction.LOAD.opcode() + loc2);
				location = compiler.addInstruction(Instruction.SUBTRACT.opcode() + loc1);
				compiler.addInstruction(Instruction.BRANCHNEG.opcode() + location + 3);
				location = compiler.addInstruction(Instruction.BRANCH.opcode());
				break;
			case LE:
				compiler.addInstruction(Instruction.LOAD.opcode() + loc2);
				compiler.addInstruction(Instruction.SUBTRACT.opcode() + loc1);
				location = compiler.addInstruction(Instruction.BRANCHNEG.opcode());
				break;
			case GE:
				compiler.addInstruction(Instruction.LOAD.opcode() + loc1);
				compiler.addInstruction(Instruction.SUBTRACT.opcode() + loc2);
				location = compiler.addInstruction(Instruction.BRANCHNEG.opcode());
				break;
			case EQ:
				compiler.addInstruction(Instruction.LOAD.opcode() + loc1);
				location = compiler.addInstruction(Instruction.SUBTRACT.opcode() + loc2);
				compiler.addInstruction(Instruction.BRANCHZERO.opcode() + location + 3);
				location = compiler.addInstruction(Instruction.BRANCH.opcode());
				break;
			case NE:
				compiler.addInstruction(Instruction.LOAD.opcode() + loc1);
				compiler.addInstruction(Instruction.SUBTRACT.opcode() + loc2);
				location = compiler.addInstruction(Instruction.BRANCHZERO.opcode());
				break;
			default:
				break;
			}
			block.locationOfBranchToEndOfBlock = location;
			compiler.pushBlock(block);
		}

		@Override
		public void checkSyntax(String line) throws CompilerException {
			SymbolType.assertNot(op1(line), SymbolType.LABEL);
			SymbolType.assertNot(op2(line), SymbolType.LABEL);
			Condition.of(condition(line));
		}

		private String op1(String line) {
			return line.split(" ")[2];
		}

		private String condition(String line) {
			return line.split(" ")[3];
		}

		private String op2(String line) {
			return line.split(" ")[4];
		}
	},

	/** Beginning of {@code else} block and end of {@code if} block */
	ELSE("else", 2, false) {
		@Override
		public void evaluate(String line, SML_Compiler compiler) {
			final IfBlock oldBlock = (IfBlock) compiler.popBlock();

			final int locationOfBranchToEnd = oldBlock.locationOfBranchToEndOfBlock;
			final int locationOfEnd         = compiler.addInstruction(Instruction.BRANCH.opcode());

			final Block block = new IfBlock();
			block.locationOfBranchToEndOfBlock = locationOfEnd;
			compiler.pushBlock(block);

			compiler.setBranchLocation(locationOfBranchToEnd, locationOfEnd + 1);
		}

		@Override
		public void checkSyntax(String line) throws CompilerException {

		}
	},

	/** End of {@code if} or {@code else} block */
	ENDIF("endif", 2, false) {
		@Override
		public void evaluate(String line, SML_Compiler compiler) {
			final IfBlock block = (IfBlock) compiler.popBlock();
			final int     locationOfBranchToEnd = block.locationOfBranchToEndOfBlock;

			// dummy command just to get the location of endif
			final int locationOfEnd = compiler.addInstruction(Instruction.NOOP.opcode());
			compiler.setBranchLocation(locationOfBranchToEnd, locationOfEnd + 1);
		}

		@Override
		public void checkSyntax(String line) throws CompilerException {

		}
	},

	/** Start of {@code while} block */
	WHILE("while", 5, false) {
		@Override
		public void evaluate(String line, SML_Compiler compiler) {

			final WhileBlock block = new WhileBlock();

			int       loc1, loc2;
			Condition condition;

			loc1 = compiler.getSymbol(op1(line)).location;

			try {
				condition = Condition.of(condition(line));
			} catch (InvalidConditionException e) {
				// checkSyntax was called, this should never throw
				throw new RuntimeException(e);
			}

			loc2 = compiler.getSymbol(op2(line)).location;

			int start    = 0;
			int location = 0;
			switch (condition) {
			case LT:
				start = compiler.addInstruction(Instruction.LOAD.opcode() + loc1);
				location = compiler.addInstruction(Instruction.SUBTRACT.opcode() + loc2);
				compiler.addInstruction(Instruction.BRANCHNEG.opcode() + location + 3);
				location = compiler.addInstruction(Instruction.BRANCH.opcode());
				break;
			case GT:
				start = compiler.addInstruction(Instruction.LOAD.opcode() + loc2);
				location = compiler.addInstruction(Instruction.SUBTRACT.opcode() + loc1);
				compiler.addInstruction(Instruction.BRANCHNEG.opcode() + location + 3);
				location = compiler.addInstruction(Instruction.BRANCH.opcode());
				break;
			case LE:
				start = compiler.addInstruction(Instruction.LOAD.opcode() + loc2);
				compiler.addInstruction(Instruction.SUBTRACT.opcode() + loc1);
				location = compiler.addInstruction(Instruction.BRANCHNEG.opcode());
				break;
			case GE:
				start = compiler.addInstruction(Instruction.LOAD.opcode() + loc1);
				compiler.addInstruction(Instruction.SUBTRACT.opcode() + loc2);
				location = compiler.addInstruction(Instruction.BRANCHNEG.opcode());
				break;
			case EQ:
				start = compiler.addInstruction(Instruction.LOAD.opcode() + loc1);
				location = compiler.addInstruction(Instruction.SUBTRACT.opcode() + loc2);
				compiler.addInstruction(Instruction.BRANCHZERO.opcode() + location + 3);
				location = compiler.addInstruction(Instruction.BRANCH.opcode());
				break;
			case NE:
				start = compiler.addInstruction(Instruction.LOAD.opcode() + loc1);
				compiler.addInstruction(Instruction.SUBTRACT.opcode() + loc2);
				location = compiler.addInstruction(Instruction.BRANCHZERO.opcode());
				break;
			default:
				break;
			}
			block.locationOfFirstInstruction = start;
			block.locationOfBranchToEndOfBlock = location;
			compiler.pushBlock(block);
		}

		@Override
		public void checkSyntax(String line) throws CompilerException {
			SymbolType.assertNot(op1(line), SymbolType.LABEL);
			SymbolType.assertNot(op2(line), SymbolType.LABEL);
			Condition.of(condition(line));
		}

		private String op1(String line) {
			return line.split(" ")[2];
		}

		private String condition(String line) {
			return line.split(" ")[3];
		}

		private String op2(String line) {
			return line.split(" ")[4];
		}
	},

	/** End of while block */
	ENDWHILE("endwhile", 2, false) {
		@Override
		public void evaluate(String line, SML_Compiler compiler) {

			final WhileBlock block = (WhileBlock) compiler.popBlock();

			final int whileStartLocation = block.locationOfFirstInstruction;
			final int whileEndLocation = compiler.addInstruction(Instruction.BRANCH.opcode() + whileStartLocation);


			final int locationOfBranchToEnd = block.locationOfBranchToEndOfBlock;
			compiler.setBranchLocation(locationOfBranchToEnd, whileEndLocation + 1);
		}

		@Override
		public void checkSyntax(String line) throws CompilerException {

		}
	},

	/** End of program */
	END("end", 2, false) {
		@Override
		public void evaluate(String line, SML_Compiler compiler) {
			compiler.addInstruction(Instruction.HALT.opcode());
		}

		@Override
		public void checkSyntax(String line) throws CompilerException {

		}
	},

	/** No-operation */
	NOOP("noop", 2, false) {
		@Override
		public void evaluate(String line, SML_Compiler compiler) {
			compiler.addInstruction(Instruction.NOOP.opcode());
		}

		@Override
		public void checkSyntax(String line) throws CompilerException {

		}
	},

	/** Dump memory contents to screen */
	DUMP("dump", 2, false) {
		@Override
		public void evaluate(String line, SML_Compiler compiler) {
			compiler.addInstruction(Instruction.DUMP.opcode());
		}

		@Override
		public void checkSyntax(String line) throws CompilerException {

		}
	};

	/**
	 * The identifier of a line of high-level code that corresponds to this
	 * Statement.
	 */
	public final String identifier;

	/**
	 * The expected length of a line of high-level code that corresponds to this
	 * Statement.
	 */
	public final int length;

	/**
	 * {@code true} if this Statement is used for declaring variables, {@code false}
	 * otherwise.
	 */
	public final boolean isConstructor;

	private static final Map<String, Statement> map;

	static {
		map = new HashMap<>();
		for (final Statement s : Statement.values())
			Statement.map.put(s.identifier, s);
	}

	/**
	 * Uses an {@code SML_Compiler} to generate machine code for a line of
	 * high-level code for the {@code compiler}.
	 *
	 * @param line     a line of high-level code
	 * @param compiler the compiler for which to generate machine code
	 */
	public abstract void evaluate(String line, SML_Compiler compiler);

	public abstract void checkSyntax(String line) throws CompilerException;

	/**
	 * Returns the {@code Statement} that has the given {@code identifier}.
	 *
	 * @param identifier the {@code identifier} of the Statement
	 *
	 * @return the Statement with that identifier
	 *
	 * @throws InvalidStatementException when there is no Statement with the given
	 *                                   identifier
	 */
	public static Statement of(String identifier) throws InvalidStatementException {

		final Statement statement = Statement.map.get(identifier);
		if (statement == null)
			throw new InvalidStatementException(identifier);

		return statement;
	}

	Statement(String identifier, int length, boolean isConstructor) {
		this.identifier = identifier;
		this.length = length;
		this.isConstructor = isConstructor;
	}
}

package compiler;

import java.util.EmptyStackException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import compiler.blocks.Block;
import compiler.blocks.IfBlock;
import compiler.blocks.WhileBlock;
import compiler.exceptions.CompilerException;
import compiler.exceptions.InvalidConditionException;
import compiler.exceptions.InvalidStatementException;
import compiler.exceptions.NoBlockException;
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
 * language. Each statement checks a line of high-level code for its syntax and
 * also evaluates it assuming both that it is syntactically correct and that all
 * variables and constants are correctly declared.
 *
 * @author Alex Mandelias
 */
enum Statement {

	/** Comment */
	COMMENT("//", -1, false) {
		@Override
		public void evaluate(String line, SML_Compiler compiler) throws CompilerException {

		}

		@Override
		public void checkSyntax(String line) throws CompilerException {

		}
	},

	/** Declaration of integer variables */
	INT("int", -1, true) {
		@Override
		public void evaluate(String line, SML_Compiler compiler) throws CompilerException {
			for (String var : vars(line))
				compiler.declareVariable(var, identifier);
		}

		@Override
		public void checkSyntax(String line) throws CompilerException {
			for (String var : vars(line))
				SymbolType.assertType(var, SymbolType.VARIABLE);
		}

		private String[] vars(String line) {
			String[] tokens = line.split(" ");
			String[] vars   = new String[tokens.length - 2];
			System.arraycopy(tokens, 2, vars, 0, vars.length);
			return vars;
		}
	},

	/** Prompt the user for input */
	INPUT("input", -1, false) {
		@Override
		public void evaluate(String line, SML_Compiler compiler) throws CompilerException {
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
			String[] tokens = line.split(" ");
			String[] vars   = new String[tokens.length - 2];
			System.arraycopy(tokens, 2, vars, 0, vars.length);
			return vars;
		}
	},

	/** Evaluate an expression and assign to variable */
	LET("let", -1, false) {
		@Override
		public void evaluate(String line, SML_Compiler compiler) throws CompilerException {
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
				if (!token.isOperatorOrParenthesis())
					SymbolType.assertTypeNot(token.value, SymbolType.LABEL);
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
		public void evaluate(String line, SML_Compiler compiler) throws CompilerException {
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
				SymbolType.assertTypeNot(var, SymbolType.LABEL);
		}

		private String[] vars(String line) {
			String[] tokens = line.split(" ");
			String[] vars   = new String[tokens.length - 2];
			System.arraycopy(tokens, 2, vars, 0, vars.length);
			return vars;
		}
	},

	/** Define a label (location to jump to) */
	LABEL("label", 3, true) {
		@Override
		public void evaluate(String line, SML_Compiler compiler) throws CompilerException {
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
		public void evaluate(String line, SML_Compiler compiler) throws CompilerException {
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
		public void evaluate(String line, SML_Compiler compiler) throws CompilerException {

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
			SymbolType.assertTypeNot(op1(line), SymbolType.LABEL);
			SymbolType.assertTypeNot(op2(line), SymbolType.LABEL);
			SymbolType.assertType(labelToJump(line), SymbolType.LABEL);
			Condition.of(condition(line));

			String jumpto = line.split(" ")[5];
			if (!jumpto.equals("jumpto"))
				throw new UnexpectedTokenException(jumpto, "jumpto");
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
		public void evaluate(String line, SML_Compiler compiler) throws CompilerException {

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
			SymbolType.assertTypeNot(op1(line), SymbolType.LABEL);
			SymbolType.assertTypeNot(op2(line), SymbolType.LABEL);
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
		public void evaluate(String line, SML_Compiler compiler) throws CompilerException {
			final IfBlock oldBlock;
			try {
				oldBlock = (IfBlock) compiler.popBlock();
			} catch (ClassCastException e) {
				throw new NoBlockException(new IfBlock());

			} catch (EmptyStackException e) {
				throw new NoBlockException(new IfBlock());
			}

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
		public void evaluate(String line, SML_Compiler compiler) throws CompilerException {

			final IfBlock block;
			try {
				block = (IfBlock) compiler.popBlock();
			} catch (ClassCastException e) {
				throw new NoBlockException(new IfBlock());

			} catch (EmptyStackException e) {
				throw new NoBlockException(new IfBlock());
			}

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
		public void evaluate(String line, SML_Compiler compiler) throws CompilerException {

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
			SymbolType.assertTypeNot(op1(line), SymbolType.LABEL);
			SymbolType.assertTypeNot(op2(line), SymbolType.LABEL);
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
		public void evaluate(String line, SML_Compiler compiler) throws CompilerException {

			final WhileBlock block;
			try {
				block = (WhileBlock) compiler.popBlock();
			} catch (ClassCastException e) {
				throw new NoBlockException(new WhileBlock());

			} catch (EmptyStackException e) {
				throw new NoBlockException(new WhileBlock());
			}

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
		public void evaluate(String line, SML_Compiler compiler) throws CompilerException {
			compiler.addInstruction(Instruction.HALT.opcode());
		}

		@Override
		public void checkSyntax(String line) throws CompilerException {

		}
	},

	/** No-operation */
	NOOP("noop", 2, false) {
		@Override
		public void evaluate(String line, SML_Compiler compiler) throws CompilerException {
			compiler.addInstruction(Instruction.NOOP.opcode());
		}

		@Override
		public void checkSyntax(String line) throws CompilerException {

		}
	},

	/** Dump memory contents to screen */
	DUMP("dump", 2, false) {
		@Override
		public void evaluate(String line, SML_Compiler compiler) throws CompilerException {
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
	 * @throws CompilerException TODO
	 */
	public abstract void evaluate(String line, SML_Compiler compiler) throws CompilerException;

	/**
	 * Checks if the syntax of the {@code line} is correct for this Statement. This
	 * method should be called before the {@code evaluate} method since it assumes
	 * that the syntax is correct.
	 *
	 * @param line the line of high-level-language code
	 *
	 * @throws CompilerException if the syntax is not correct
	 */
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

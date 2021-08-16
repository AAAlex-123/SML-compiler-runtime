package compiler;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import compiler.blocks.Block;
import compiler.blocks.IfBlock;
import compiler.blocks.WhileBlock;
import compiler.postfix.InfixToPostfix;
import compiler.postfix.PostfixEvaluator;
import compiler.postfix.Token;
import compiler.symboltable.SymbolInfo;
import compiler.symboltable.SymbolTable;
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
		public void evaluate(String line) {

		}
	},

	/** Declaration of integer variables */
	INT("int", -1, true) {
		@Override
		public void evaluate(String line) {
			final String[] tokens = line.split(" ");
			for (int i = 2, count = tokens.length; i < count; ++i)
				SML_Compiler.declareVariable(tokens[i], identifier);
		}
	},

	/** Prompt the user for input */
	INPUT("input", -1, false) {
		@Override
		public void evaluate(String line) {
			final String[] vars = line.substring(9).split(" ");
			for (final String var : vars) {
				final int location = SML_Compiler.getVariable(var).location;
				SML_Compiler.addInstruction(Instruction.READ_INT.opcode() + location);
			}
		}
	},

	/** Evaluate an expression and assign to variable */
	LET("let", -1, false) {
		@Override
		public void evaluate(String line) {
			// generate postfix
			final String        infix        = line.split("=")[1];
			final List<Token>   postfix      = InfixToPostfix.convertToPostfix(infix);

			// declare constants that weren't interpreted as constants (e.g. 2+3)
			for (Token token : postfix) {
				final String symbol = token.value;
				if (symbol.matches("\\d+"))
					if (!SML_Compiler.constantDeclared(symbol))
						SML_Compiler.declareConstant(symbol, INT.identifier);
			}

			final SymbolTable   symbolTable  = SML_Compiler.getSymbolTable();
			final List<Integer> instructions = PostfixEvaluator.evaluatePostfix(postfix,
			        symbolTable);

			// write instructions for evaluating postfix
			for (final int instruction : instructions)
				SML_Compiler.addInstruction(instruction);

			// store result of postfix that is loaded after the last instruction of evaluation
			final String var      = line.split(" ")[2];
			final int    location = SML_Compiler.getVariable(var).location;

			SML_Compiler.addInstruction(Instruction.STORE.opcode() + location);
		}
	},

	/** Print the value of a variable to the screen */
	PRINT("print", -1, false) {
		@Override
		public void evaluate(String line) {
			final String[] vars = line.substring(9).split(" ");
			for (final String var : vars) {
				final SymbolInfo info = SML_Compiler.getSymbol(var);

				final String varType  = info.varType;
				final int    location = info.location;

				if (varType.equals(INT.identifier))
					SML_Compiler.addInstruction(Instruction.WRITE_NL.opcode() + location);
			}
		}
	},

	/** Unconditional jump to line */
	GOTO("goto", 3, false) {
		@Override
		public void evaluate(String line) {
			final String[] tokens = line.split(" ");

			final int location   = SML_Compiler.addInstruction(Instruction.BRANCH.opcode());
			final String lineToJump = tokens[2];

			SML_Compiler.setLineToJump(location, lineToJump);
		}
	},

	/** Conditional jump to line */
	IFGOTO("ifg", 7, false) {
		@Override
		public void evaluate(String line) {
			final String[] tokens = line.split(" ");

			String    op1, op2;
			int       loc1, loc2;
			Condition condition;
			String    targetLine;

			op1 = tokens[2];
			loc1 = SML_Compiler.getSymbol(op1).location;

			condition = Condition.of(tokens[3]);

			op2 = tokens[4];
			loc2 = SML_Compiler.getSymbol(op2).location;

			targetLine = tokens[6];

			int location;
			switch (condition) {
			case LT:
				SML_Compiler.addInstruction(Instruction.LOAD.opcode() + loc1);
				SML_Compiler.addInstruction(Instruction.SUBTRACT.opcode() + loc2);
				location = SML_Compiler.addInstruction(Instruction.BRANCHNEG.opcode());
				SML_Compiler.setLineToJump(location, targetLine);
				break;
			case GT:
				SML_Compiler.addInstruction(Instruction.LOAD.opcode() + loc2);
				SML_Compiler.addInstruction(Instruction.SUBTRACT.opcode() + loc1);
				location = SML_Compiler.addInstruction(Instruction.BRANCHNEG.opcode());
				SML_Compiler.setLineToJump(location, targetLine);
				break;
			case LE:
				SML_Compiler.addInstruction(Instruction.LOAD.opcode() + loc1);
				SML_Compiler.addInstruction(Instruction.SUBTRACT.opcode() + loc2);
				location = SML_Compiler.addInstruction(Instruction.BRANCHNEG.opcode());
				SML_Compiler.setLineToJump(location, targetLine);
				location = SML_Compiler.addInstruction(Instruction.BRANCHZERO.opcode());
				SML_Compiler.setLineToJump(location, targetLine);
				break;
			case GE:
				SML_Compiler.addInstruction(Instruction.LOAD.opcode() + loc2);
				SML_Compiler.addInstruction(Instruction.SUBTRACT.opcode() + loc1);
				location = SML_Compiler.addInstruction(Instruction.BRANCHNEG.opcode());
				SML_Compiler.setLineToJump(location, targetLine);
				location = SML_Compiler.addInstruction(Instruction.BRANCHZERO.opcode());
				SML_Compiler.setLineToJump(location, targetLine);
				break;
			case EQ:
				SML_Compiler.addInstruction(Instruction.LOAD.opcode() + loc1);
				SML_Compiler.addInstruction(Instruction.SUBTRACT.opcode() + loc2);
				location = SML_Compiler.addInstruction(Instruction.BRANCHZERO.opcode());
				SML_Compiler.setLineToJump(location, targetLine);
				break;
			case NE:
				SML_Compiler.addInstruction(Instruction.LOAD.opcode() + loc1);
				SML_Compiler.addInstruction(Instruction.SUBTRACT.opcode() + loc2);
				location = SML_Compiler.addInstruction(Instruction.BRANCHZERO.opcode());
				SML_Compiler.setLineToJump(location, targetLine);
				location = SML_Compiler.addInstruction(Instruction.BRANCH.opcode());
				SML_Compiler.setLineToJump(location, targetLine);
				break;
			default:
				break;
			}
		}
	},

	/** Beginning of {@code if} block */
	IF("if", 5, false) {
		@Override
		public void evaluate(String line) {

			final IfBlock block = new IfBlock();

			final String[] tokens = line.split(" ");

			String    op1, op2;
			int       loc1, loc2;
			Condition condition;

			op1 = tokens[2];
			loc1 = SML_Compiler.getSymbol(op1).location;

			condition = Condition.of(tokens[3]);

			op2 = tokens[4];
			loc2 = SML_Compiler.getSymbol(op2).location;

			int location = 0;
			switch (condition) {
			case LT:
				SML_Compiler.addInstruction(Instruction.LOAD.opcode() + loc1);
				location = SML_Compiler.addInstruction(Instruction.SUBTRACT.opcode() + loc2);
				SML_Compiler.addInstruction(Instruction.BRANCHNEG.opcode() + location + 3);
				location = SML_Compiler.addInstruction(Instruction.BRANCH.opcode());
				break;
			case GT:
				SML_Compiler.addInstruction(Instruction.LOAD.opcode() + loc2);
				location = SML_Compiler.addInstruction(Instruction.SUBTRACT.opcode() + loc1);
				SML_Compiler.addInstruction(Instruction.BRANCHNEG.opcode() + location + 3);
				location = SML_Compiler.addInstruction(Instruction.BRANCH.opcode());
				break;
			case LE:
				SML_Compiler.addInstruction(Instruction.LOAD.opcode() + loc2);
				SML_Compiler.addInstruction(Instruction.SUBTRACT.opcode() + loc1);
				location = SML_Compiler.addInstruction(Instruction.BRANCHNEG.opcode());
				break;
			case GE:
				SML_Compiler.addInstruction(Instruction.LOAD.opcode() + loc1);
				SML_Compiler.addInstruction(Instruction.SUBTRACT.opcode() + loc2);
				location = SML_Compiler.addInstruction(Instruction.BRANCHNEG.opcode());
				break;
			case EQ:
				SML_Compiler.addInstruction(Instruction.LOAD.opcode() + loc1);
				location = SML_Compiler.addInstruction(Instruction.SUBTRACT.opcode() + loc2);
				SML_Compiler.addInstruction(Instruction.BRANCHZERO.opcode() + location + 3);
				location = SML_Compiler.addInstruction(Instruction.BRANCH.opcode());
				break;
			case NE:
				SML_Compiler.addInstruction(Instruction.LOAD.opcode() + loc1);
				SML_Compiler.addInstruction(Instruction.SUBTRACT.opcode() + loc2);
				location = SML_Compiler.addInstruction(Instruction.BRANCHZERO.opcode());
				break;
			default:
				break;
			}
			block.locationOfBranchToEndOfBlock = location;
			SML_Compiler.pushBlock(block);
		}
	},

	/** Beginning of {@code else} block and end of {@code if} block */
	ELSE("else", 2, false) {
		@Override
		public void evaluate(String line) {
			final IfBlock oldBlock              = (IfBlock) SML_Compiler.popBlock();
			final int     locationOfBranchToEnd = oldBlock.locationOfBranchToEndOfBlock;
			final int     locationOfEnd         = SML_Compiler
			        .addInstruction(Instruction.BRANCH.opcode());

			final Block block = new IfBlock();
			block.locationOfBranchToEndOfBlock = locationOfEnd;
			SML_Compiler.pushBlock(block);

			SML_Compiler.setBranchLocation(locationOfBranchToEnd, locationOfEnd + 1);
		}
	},

	/** End of {@code if} or {@code else} block */
	ENDIF("endif", 2, false) {
		@Override
		public void evaluate(String line) {
			final IfBlock block                 = (IfBlock) SML_Compiler.popBlock();
			final int     locationOfBranchToEnd = block.locationOfBranchToEndOfBlock;

			// dummy command just to get the location of endif
			final int locationOfEnd = SML_Compiler.addInstruction(Instruction.NOOP.opcode());
			SML_Compiler.setBranchLocation(locationOfBranchToEnd, locationOfEnd + 1);
		}
	},

	/** Start of {@code while} block */
	WHILE("while", 5, false) {
		@Override
		public void evaluate(String line) {

			final WhileBlock block = new WhileBlock();

			final String[] tokens = line.split(" ");

			String    op1, op2;
			int       loc1, loc2;
			Condition condition;

			op1 = tokens[2];
			loc1 = SML_Compiler.getSymbol(op1).location;

			condition = Condition.of(tokens[3]);

			op2 = tokens[4];
			loc2 = SML_Compiler.getSymbol(op2).location;

			int start    = 0;
			int location = 0;
			switch (condition) {
			case LT:
				start = SML_Compiler.addInstruction(Instruction.LOAD.opcode() + loc1);
				location = SML_Compiler.addInstruction(Instruction.SUBTRACT.opcode() + loc2);
				SML_Compiler.addInstruction(Instruction.BRANCHNEG.opcode() + location + 3);
				location = SML_Compiler.addInstruction(Instruction.BRANCH.opcode());
				break;
			case GT:
				start = SML_Compiler.addInstruction(Instruction.LOAD.opcode() + loc2);
				location = SML_Compiler.addInstruction(Instruction.SUBTRACT.opcode() + loc1);
				SML_Compiler.addInstruction(Instruction.BRANCHNEG.opcode() + location + 3);
				location = SML_Compiler.addInstruction(Instruction.BRANCH.opcode());
				break;
			case LE:
				start = SML_Compiler.addInstruction(Instruction.LOAD.opcode() + loc2);
				SML_Compiler.addInstruction(Instruction.SUBTRACT.opcode() + loc1);
				location = SML_Compiler.addInstruction(Instruction.BRANCHNEG.opcode());
				break;
			case GE:
				start = SML_Compiler.addInstruction(Instruction.LOAD.opcode() + loc1);
				SML_Compiler.addInstruction(Instruction.SUBTRACT.opcode() + loc2);
				location = SML_Compiler.addInstruction(Instruction.BRANCHNEG.opcode());
				break;
			case EQ:
				start = SML_Compiler.addInstruction(Instruction.LOAD.opcode() + loc1);
				location = SML_Compiler.addInstruction(Instruction.SUBTRACT.opcode() + loc2);
				SML_Compiler.addInstruction(Instruction.BRANCHZERO.opcode() + location + 3);
				location = SML_Compiler.addInstruction(Instruction.BRANCH.opcode());
				break;
			case NE:
				start = SML_Compiler.addInstruction(Instruction.LOAD.opcode() + loc1);
				SML_Compiler.addInstruction(Instruction.SUBTRACT.opcode() + loc2);
				location = SML_Compiler.addInstruction(Instruction.BRANCHZERO.opcode());
				break;
			default:
				break;
			}
			block.locationOfFirstInstruction = start;
			block.locationOfBranchToEndOfBlock = location;
			SML_Compiler.pushBlock(block);
		}
	},

	/** End of while block */
	ENDWHILE("endwhile", 2, false) {
		@Override
		public void evaluate(String line) {

			final WhileBlock block = (WhileBlock) SML_Compiler.popBlock();

			final int whileStartLocation = block.locationOfFirstInstruction;
			final int whileEndLocation   = SML_Compiler
			        .addInstruction(Instruction.BRANCH.opcode() + whileStartLocation);


			final int locationOfBranchToEnd = block.locationOfBranchToEndOfBlock;
			SML_Compiler.setBranchLocation(locationOfBranchToEnd, whileEndLocation + 1);
		}
	},

	/** End of program */
	END("end", 2, false) {
		@Override
		public void evaluate(String line) {
			SML_Compiler.addInstruction(Instruction.HALT.opcode());
		}
	},

	/** No-operation */
	NOOP("noop", 2, false) {
		@Override
		public void evaluate(String line) {
			SML_Compiler.addInstruction(Instruction.NOOP.opcode());
		}
	},

	/** Dump memory contents to screen */
	DUMP("dump", 2, false) {
		@Override
		public void evaluate(String line) {
			SML_Compiler.addInstruction(Instruction.DUMP.opcode());
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
	 * Uses information from the SML_Compiler's Symbol Table to generate machine
	 * code for a line of high-level code and stores it to the SML_Compiler.
	 *
	 * @param line a line of high-level code
	 */
	public abstract void evaluate(String line);

	/**
	 * Returns the {@code Statement} that has the given {@code identifier}.
	 *
	 * @param identifier the {@code identifier} of the Statement
	 *
	 * @return the Statement with that identifier
	 */
	public static Statement of(String identifier) {
		return Statement.map.get(identifier);
	}

	Statement(String identifier, int length, boolean isConstructor) {
		this.identifier = identifier;
		this.length = length;
		this.isConstructor = isConstructor;
	}
}

package sml_package;

import static symboltable.SymbolType.CONSTANT;
import static symboltable.SymbolType.VARIABLE;

import java.util.HashMap;
import java.util.Map;

import postfix.InfixToPostfix;
import postfix.PostfixEvaluator;
import sml_package.blocks.Block;
import sml_package.blocks.IfBlock;
import sml_package.blocks.WhileBlock;

public enum Statement {

	COMMENT("//", -1, false) {
		@Override
		public void evaluate(String line) {
			;
		}
	},

	INT("int", -1, true) {
		@Override
		public void evaluate(String line) {
			String[] tokens = line.split(" ");
			for (int i = 2, count = tokens.length; i < count; i++) {
				String variable = tokens[i];
				int    location = SML_Compiler.addVariable();

				SML_Compiler.symbolTable.addEntry(variable, VARIABLE, location,
				        identifier);
			}
		}
	},

	INPUT("input", -1, false) {
		@Override
		public void evaluate(String line) {
			String[] vars = line.substring(9).split(" ");
			for (String var : vars) {
				int loc = SML_Compiler.symbolTable.getSymbol(var, VARIABLE).location;
				SML_Compiler.addInstruction(Command.READ_INT.opcode() + loc);
			}
		}
	},

	LET("let", -1, false) {
		@Override
		public void evaluate(String line) {
			String var = line.split(" ")[2];
			int    loc = SML_Compiler.symbolTable.getSymbol(var, VARIABLE).location;

			String infix = line.split("=")[1];
			PostfixEvaluator.evaluatePostfix(InfixToPostfix.convertToPostfix(infix));

			SML_Compiler.addInstruction(Command.STORE.opcode() + loc);
		}
	},

	PRINT("print", -1, false) {
		@Override
		public void evaluate(String line) {
			String[] vars = line.substring(9).split(" ");
			for (String var : vars) {
				int loc = SML_Compiler.symbolTable.getSymbol(var, CONSTANT, VARIABLE).location;

				String varType = SML_Compiler.symbolTable.getSymbol(var).varType;

				// future: print according to type
				if (varType.equals(INT.identifier)) {
					SML_Compiler.addInstruction(Command.WRITE_NL.opcode() + loc);
				}
			}
		}
	},

	GOTO("goto", 3, false) {
		@Override
		public void evaluate(String line) {
			String[] tokens = line.split(" ");

			int location   = SML_Compiler.addInstruction(Command.BRANCH.opcode());
			int lineToJump = Integer.parseInt(tokens[2]);

			SML_Compiler.setLineToJump(location, lineToJump);
		}
	},

	IFGOTO("ifg", 7, false) {
		@Override
		public void evaluate(String line) {
			String[] tokens = line.split(" ");

			String    op1, op2;
			int       loc1, loc2;
			Condition condition;
			int       targetLine;

			op1 = tokens[2];
			loc1 = SML_Compiler.symbolTable.getSymbol(op1, CONSTANT, VARIABLE).location;

			condition = Condition.of(tokens[3]);

			op2 = tokens[4];
			loc2 = SML_Compiler.symbolTable.getSymbol(op2, CONSTANT, VARIABLE).location;

			targetLine = Integer.parseInt(tokens[6]);

			int location;
			switch (condition) {
			case LT:
				SML_Compiler.addInstruction(Command.LOAD.opcode() + loc1);
				SML_Compiler.addInstruction(Command.SUBTRACT.opcode() + loc2);
				location = SML_Compiler.addInstruction(Command.BRANCHNEG.opcode());
				SML_Compiler.setLineToJump(location, targetLine);
				break;
			case GT:
				SML_Compiler.addInstruction(Command.LOAD.opcode() + loc2);
				SML_Compiler.addInstruction(Command.SUBTRACT.opcode() + loc1);
				location = SML_Compiler.addInstruction(Command.BRANCHNEG.opcode());
				SML_Compiler.setLineToJump(location, targetLine);
				break;
			case LE:
				SML_Compiler.addInstruction(Command.LOAD.opcode() + loc1);
				SML_Compiler.addInstruction(Command.SUBTRACT.opcode() + loc2);
				location = SML_Compiler.addInstruction(Command.BRANCHNEG.opcode());
				SML_Compiler.setLineToJump(location, targetLine);
				location = SML_Compiler.addInstruction(Command.BRANCHZERO.opcode());
				SML_Compiler.setLineToJump(location, targetLine);
				break;
			case GE:
				SML_Compiler.addInstruction(Command.LOAD.opcode() + loc2);
				SML_Compiler.addInstruction(Command.SUBTRACT.opcode() + loc1);
				location = SML_Compiler.addInstruction(Command.BRANCHNEG.opcode());
				SML_Compiler.setLineToJump(location, targetLine);
				location = SML_Compiler.addInstruction(Command.BRANCHZERO.opcode());
				SML_Compiler.setLineToJump(location, targetLine);
				break;
			case EQ:
				SML_Compiler.addInstruction(Command.LOAD.opcode() + loc1);
				SML_Compiler.addInstruction(Command.SUBTRACT.opcode() + loc2);
				location = SML_Compiler.addInstruction(Command.BRANCHZERO.opcode());
				SML_Compiler.setLineToJump(location, targetLine);
				break;
			case NE:
				SML_Compiler.addInstruction(Command.LOAD.opcode() + loc1);
				SML_Compiler.addInstruction(Command.SUBTRACT.opcode() + loc2);
				location = SML_Compiler.addInstruction(Command.BRANCHZERO.opcode());
				SML_Compiler.setLineToJump(location, targetLine);
				location = SML_Compiler.addInstruction(Command.BRANCH.opcode());
				SML_Compiler.setLineToJump(location, targetLine);
				break;
			default:
				break;
			}
		}
	},

	IF("if", 5, false) {
		@Override
		public void evaluate(String line) {

			final IfBlock block = new IfBlock();

			String[] tokens = line.split(" ");

			String    op1, op2;
			int       loc1, loc2;
			Condition condition;

			op1 = tokens[2];
			loc1 = SML_Compiler.symbolTable.getSymbol(op1, CONSTANT, VARIABLE).location;

			condition = Condition.of(tokens[3]);

			op2 = tokens[4];
			loc2 = SML_Compiler.symbolTable.getSymbol(op2, CONSTANT, VARIABLE).location;

			int location = 0;
			switch (condition) {
			case LT:
				SML_Compiler.addInstruction(Command.LOAD.opcode() + loc1);
				location = SML_Compiler.addInstruction(Command.SUBTRACT.opcode() + loc2);
				SML_Compiler.addInstruction(Command.BRANCHNEG.opcode() + location + 3);
				location = SML_Compiler.addInstruction(Command.BRANCH.opcode());
				break;
			case GT:
				SML_Compiler.addInstruction(Command.LOAD.opcode() + loc2);
				location = SML_Compiler.addInstruction(Command.SUBTRACT.opcode() + loc1);
				SML_Compiler.addInstruction(Command.BRANCHNEG.opcode() + location + 3);
				location = SML_Compiler.addInstruction(Command.BRANCH.opcode());
				break;
			case LE:
				SML_Compiler.addInstruction(Command.LOAD.opcode() + loc2);
				SML_Compiler.addInstruction(Command.SUBTRACT.opcode() + loc1);
				location = SML_Compiler.addInstruction(Command.BRANCHNEG.opcode());
				break;
			case GE:
				SML_Compiler.addInstruction(Command.LOAD.opcode() + loc1);
				SML_Compiler.addInstruction(Command.SUBTRACT.opcode() + loc2);
				location = SML_Compiler.addInstruction(Command.BRANCHNEG.opcode());
				break;
			case EQ:
				SML_Compiler.addInstruction(Command.LOAD.opcode() + loc1);
				location = SML_Compiler.addInstruction(Command.SUBTRACT.opcode() + loc2);
				SML_Compiler.addInstruction(Command.BRANCHZERO.opcode() + location + 3);
				location = SML_Compiler.addInstruction(Command.BRANCH.opcode());
				break;
			case NE:
				SML_Compiler.addInstruction(Command.LOAD.opcode() + loc1);
				SML_Compiler.addInstruction(Command.SUBTRACT.opcode() + loc2);
				location = SML_Compiler.addInstruction(Command.BRANCHZERO.opcode());
				break;
			default:
				break;
			}
			block.locationOfBranchToEndOfBlock = location;
			SML_Compiler.pushBlock(block);
		}
	},

	ELSE("else", 2, false) {
		@Override
		public void evaluate(String line) {
			IfBlock oldBlock     = (IfBlock) SML_Compiler.popBlock();
			int     locationOfIf = oldBlock.locationOfBranchToEndOfBlock;
			int location     = SML_Compiler.addInstruction(Command.BRANCH.opcode());

			Block block = new IfBlock();
			block.locationOfBranchToEndOfBlock = location;
			SML_Compiler.pushBlock(block);

			SML_Compiler.setBranchLocation(locationOfIf, location + 1);
		}
	},

	ENDIF("endif", 2, false) {
		@Override
		public void evaluate(String line) {
			IfBlock oldBlock     = (IfBlock) SML_Compiler.popBlock();
			int     locationOfIf = oldBlock.locationOfBranchToEndOfBlock;

			int location = SML_Compiler.symbolTable
			        .getSymbolLocation(String.valueOf(SML_Compiler.line_count));

			SML_Compiler.setBranchLocation(locationOfIf, location);
		}
	},

	WHILE("while", 5, false) {
		@Override
		public void evaluate(String line) {

			WhileBlock block = new WhileBlock();

			String[] tokens = line.split(" ");

			String    op1, op2;
			int       loc1, loc2;
			Condition condition;

			op1 = tokens[2];
			loc1 = SML_Compiler.symbolTable.getSymbol(op1, CONSTANT, VARIABLE).location;

			condition = Condition.of(tokens[3]);

			op2 = tokens[4];
			loc2 = SML_Compiler.symbolTable.getSymbol(op2, CONSTANT, VARIABLE).location;

			int start    = 0;
			int location = 0;
			switch (condition) {
			case LT:
				start = SML_Compiler.addInstruction(Command.LOAD.opcode() + loc1);
				location = SML_Compiler.addInstruction(Command.SUBTRACT.opcode() + loc2);
				SML_Compiler.addInstruction(Command.BRANCHNEG.opcode() + location + 3);
				location = SML_Compiler.addInstruction(Command.BRANCH.opcode());
				break;
			case GT:
				start = SML_Compiler.addInstruction(Command.LOAD.opcode() + loc2);
				location = SML_Compiler.addInstruction(Command.SUBTRACT.opcode() + loc1);
				SML_Compiler.addInstruction(Command.BRANCHNEG.opcode() + location + 3);
				location = SML_Compiler.addInstruction(Command.BRANCH.opcode());
				break;
			case LE:
				start = SML_Compiler.addInstruction(Command.LOAD.opcode() + loc2);
				SML_Compiler.addInstruction(Command.SUBTRACT.opcode() + loc1);
				location = SML_Compiler.addInstruction(Command.BRANCHNEG.opcode());
				break;
			case GE:
				start = SML_Compiler.addInstruction(Command.LOAD.opcode() + loc1);
				SML_Compiler.addInstruction(Command.SUBTRACT.opcode() + loc2);
				location = SML_Compiler.addInstruction(Command.BRANCHNEG.opcode());
				break;
			case EQ:
				start = SML_Compiler.addInstruction(Command.LOAD.opcode() + loc1);
				location = SML_Compiler.addInstruction(Command.SUBTRACT.opcode() + loc2);
				SML_Compiler.addInstruction(Command.BRANCHZERO.opcode() + location + 3);
				location = SML_Compiler.addInstruction(Command.BRANCH.opcode());
				break;
			case NE:
				start = SML_Compiler.addInstruction(Command.LOAD.opcode() + loc1);
				SML_Compiler.addInstruction(Command.SUBTRACT.opcode() + loc2);
				location = SML_Compiler.addInstruction(Command.BRANCHZERO.opcode());
				break;
			default:
				break;
			}
			block.locationOfFirstCommandToLoopBack = start;
			block.locationOfBranchToEndOfBlock = location;
			SML_Compiler.pushBlock(block);
		}
	},

	ENDWHILE("endwhile", 2, false) {
		@Override
		public void evaluate(String line) {

			WhileBlock block = (WhileBlock) SML_Compiler.popBlock();

			int whileStartLocation  = block.locationOfFirstCommandToLoopBack;
			int endWhileLocation   = SML_Compiler
			        .addInstruction(Command.BRANCH.opcode() + whileStartLocation);


			int locationOfWhile = block.locationOfBranchToEndOfBlock;
			SML_Compiler.setBranchLocation(locationOfWhile, endWhileLocation + 1);
		}
	},

	END("end", 2, false) {
		@Override
		public void evaluate(String line) {
			SML_Compiler.addInstruction(Command.HALT.opcode());
		}
	},

	NOOP("noop", 2, false) {
		@Override
		public void evaluate(String line) {
			SML_Compiler.addInstruction(Command.NOOP.opcode());
		}
	},

	DUMP("dump", 2, false) {
		@Override
		public void evaluate(String line) {
			SML_Compiler.addInstruction(Command.DUMP.opcode());
		}
	};

	public final String  identifier;
	public final int     length;
	public final boolean isConstructor;

	private static final Map<String, Statement> map;

	static {
		map = new HashMap<>();
		for (Statement s : Statement.values())
			map.put(s.identifier, s);
	}

	public abstract void evaluate(String line);

	public static Statement of(String identifier) {
		return map.get(identifier);
	}

	private Statement(String identifier, int length, boolean isConstructor) {
		this.identifier = identifier;
		this.length = length;
		this.isConstructor = isConstructor;
	}
}

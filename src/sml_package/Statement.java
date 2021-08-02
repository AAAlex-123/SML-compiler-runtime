package sml_package;

import static symboltable.SymbolType.CONSTANT;
import static symboltable.SymbolType.LINE;
import static symboltable.SymbolType.VARIABLE;

import java.util.HashMap;
import java.util.Map;

import postfix.InfixToPostfix;
import postfix.PostfixEvaluator;

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
				int loc = SML_Compiler.symbolTable.getSymbolLocation(var, VARIABLE);
				SML_Compiler.addInstruction(Command.READ_INT.opcode() + loc);
			}
		}
	},

	LET("let", -1, false) {
		@Override
		public void evaluate(String line) {
			String var = line.split(" ")[2];
			int    loc = SML_Compiler.symbolTable.getSymbolLocation(var, VARIABLE);

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
				int loc = SML_Compiler.symbolTable.getSymbolLocation(var, CONSTANT, VARIABLE);

				String varType = SML_Compiler.symbolTable.getVarType(var);

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
			loc1 = SML_Compiler.symbolTable.getSymbolLocation(op1, CONSTANT, VARIABLE);

			condition = Condition.of(tokens[3]);

			op2 = tokens[4];
			loc2 = SML_Compiler.symbolTable.getSymbolLocation(op2, CONSTANT, VARIABLE);

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
			String[] tokens = line.split(" ");

			String    op1, op2;
			int       loc1, loc2;
			Condition condition;

			op1 = tokens[2];
			loc1 = SML_Compiler.symbolTable.getSymbolLocation(op1, CONSTANT, VARIABLE);

			condition = Condition.of(tokens[3]);

			op2 = tokens[4];
			loc2 = SML_Compiler.symbolTable.getSymbolLocation(op2, CONSTANT, VARIABLE);

			int location;
			switch (condition) {
			case LT:
				SML_Compiler.addInstruction(Command.LOAD.opcode() + loc1);
				location = SML_Compiler.addInstruction(Command.SUBTRACT.opcode() + loc2);
				SML_Compiler.addInstruction(Command.BRANCHNEG.opcode() + location + 3);
				location = SML_Compiler.addInstruction(Command.BRANCH.opcode());
				SML_Compiler.ifFlags[SML_Compiler.line_count][0] = location;
				break;
			case GT:
				SML_Compiler.addInstruction(Command.LOAD.opcode() + loc2);
				location = SML_Compiler.addInstruction(Command.SUBTRACT.opcode() + loc1);
				SML_Compiler.addInstruction(Command.BRANCHNEG.opcode() + location + 3);
				location = SML_Compiler.addInstruction(Command.BRANCH.opcode());
				SML_Compiler.ifFlags[SML_Compiler.line_count][0] = location;
				break;
			case LE:
				SML_Compiler.addInstruction(Command.LOAD.opcode() + loc2);
				SML_Compiler.addInstruction(Command.SUBTRACT.opcode() + loc1);
				location = SML_Compiler.addInstruction(Command.BRANCHNEG.opcode());
				SML_Compiler.ifFlags[SML_Compiler.line_count][0] = location;
				break;
			case GE:
				SML_Compiler.addInstruction(Command.LOAD.opcode() + loc1);
				SML_Compiler.addInstruction(Command.SUBTRACT.opcode() + loc2);
				location = SML_Compiler.addInstruction(Command.BRANCHNEG.opcode());
				SML_Compiler.ifFlags[SML_Compiler.line_count][0] = location;
				break;
			case EQ:
				SML_Compiler.addInstruction(Command.LOAD.opcode() + loc1);
				location = SML_Compiler.addInstruction(Command.SUBTRACT.opcode() + loc2);
				SML_Compiler.addInstruction(Command.BRANCHZERO.opcode() + location + 3);
				location = SML_Compiler.addInstruction(Command.BRANCH.opcode());
				SML_Compiler.ifFlags[SML_Compiler.line_count][0] = location;
				break;
			case NE:
				SML_Compiler.addInstruction(Command.LOAD.opcode() + loc1);
				SML_Compiler.addInstruction(Command.SUBTRACT.opcode() + loc2);
				location = SML_Compiler.addInstruction(Command.BRANCHZERO.opcode());
				SML_Compiler.ifFlags[SML_Compiler.line_count][0] = location;
				break;
			default:
				break;
			}
		}
	},

	ELSE("else", 3, false) {
		@Override
		public void evaluate(String line) {
			String[] tokens = line.split(" ");

			int location = SML_Compiler.addInstruction(Command.BRANCH.opcode());
			SML_Compiler.ifFlags[SML_Compiler.line_count][0] = location;

			String target_line = tokens[2];
			for (int i = 0; i < SML_Compiler.ifFlags[0].length; i++) {
				int branch_loc = SML_Compiler.ifFlags[Integer.parseInt(target_line)][i];
				if (branch_loc != -1) {
					int location1 = SML_Compiler.symbolTable
					        .getSymbolLocation(String.valueOf(SML_Compiler.line_count))
					        + 1;
					SML_Compiler.setBranchLocation(branch_loc, location1);
				}
			}
		}
	},

	ENDIF("endif", 3, false) {
		@Override
		public void evaluate(String line) {
			String[] tokens = line.split(" ");

			String target_line = tokens[2];
			for (int i = 0; i < SML_Compiler.ifFlags[0].length; i++) {
				int branch_loc = SML_Compiler.ifFlags[Integer.parseInt(target_line)][i];
				if (branch_loc != -1) {
					int location = SML_Compiler.symbolTable
					        .getSymbolLocation(String.valueOf(SML_Compiler.line_count));
					SML_Compiler.setBranchLocation(branch_loc, location);
				}
			}
		}
	},

	WHILE("while", 5, false) {
		@Override
		public void evaluate(String line) {
			String[]  tokens = line.split(" ");

			String    op1, op2;
			int       loc1, loc2;
			Condition condition;

			op1 = tokens[2];
			loc1 = SML_Compiler.symbolTable.getSymbolLocation(op1, CONSTANT, VARIABLE);

			condition = Condition.of(tokens[3]);

			op2 = tokens[4];
			loc2 = SML_Compiler.symbolTable.getSymbolLocation(op2, CONSTANT, VARIABLE);

			int location, location1;
			switch (condition) {
			case LT:
				SML_Compiler.addInstruction(Command.LOAD.opcode() + loc2);
				SML_Compiler.addInstruction(Command.SUBTRACT.opcode() + loc1);
				location = SML_Compiler.addInstruction(Command.BRANCHZERO.opcode());
				SML_Compiler.whileFlags[SML_Compiler.line_count][0] = location;
				location = SML_Compiler.addInstruction(Command.BRANCHNEG.opcode());
				SML_Compiler.whileFlags[SML_Compiler.line_count][1] = location;
				break;
			case GT:
				SML_Compiler.addInstruction(Command.LOAD.opcode() + loc1);
				SML_Compiler.addInstruction(Command.SUBTRACT.opcode() + loc2);
				location = SML_Compiler.addInstruction(Command.BRANCHZERO.opcode());
				SML_Compiler.whileFlags[SML_Compiler.line_count][0] = location;
				location = SML_Compiler.addInstruction(Command.BRANCHNEG.opcode());
				SML_Compiler.whileFlags[SML_Compiler.line_count][1] = location;
				break;
			case LE:
				SML_Compiler.addInstruction(Command.LOAD.opcode() + loc2);
				SML_Compiler.addInstruction(Command.SUBTRACT.opcode() + loc1);
				location = SML_Compiler.addInstruction(Command.BRANCHNEG.opcode());
				SML_Compiler.whileFlags[SML_Compiler.line_count][0] = location;
				break;
			case GE:
				SML_Compiler.addInstruction(Command.LOAD.opcode() + loc1);
				SML_Compiler.addInstruction(Command.SUBTRACT.opcode() + loc2);
				location = SML_Compiler.addInstruction(Command.BRANCHNEG.opcode());
				SML_Compiler.whileFlags[SML_Compiler.line_count][0] = location;
				break;
			case EQ:
				SML_Compiler.addInstruction(Command.LOAD.opcode() + loc1);
				location1 = SML_Compiler.addInstruction(Command.SUBTRACT.opcode() + loc2);
				location = SML_Compiler.addInstruction(Command.BRANCHZERO.opcode() + location1 + 3);
				SML_Compiler.whileFlags[SML_Compiler.line_count][0] = location;
				location = SML_Compiler.addInstruction(Command.BRANCH.opcode());
				SML_Compiler.whileFlags[SML_Compiler.line_count][1] = location;
				break;
			case NE:
				SML_Compiler.addInstruction(Command.LOAD.opcode() + loc1);
				SML_Compiler.addInstruction(Command.SUBTRACT.opcode() + loc2);
				location = SML_Compiler.addInstruction(Command.BRANCHZERO.opcode());
				SML_Compiler.whileFlags[SML_Compiler.line_count][0] = location;
				break;
			default:
				break;
			}
		}
	},

	ENDWHILE("endwhile", 3, false) {
		@Override
		public void evaluate(String line) {
			String[] tokens = line.split(" ");

			String target_line = tokens[2];

			int location = SML_Compiler.symbolTable.getSymbolLocation(target_line, LINE);
			SML_Compiler.addInstruction(Command.BRANCH.opcode() + location);

			for (int i = 0; i < SML_Compiler.whileFlags[0].length; i++) {

				int branchLocation = SML_Compiler.whileFlags[Integer.parseInt(target_line)][i];

				// if ((branchLocation != -1) && ((SML_Compiler.memory.read(branchLocation) % 0x100) == 0)) {
				if (branchLocation != -1) {
					int location1 = SML_Compiler.symbolTable
					        .getSymbolLocation(String.valueOf(SML_Compiler.line_count))
					        + 1;
					SML_Compiler.setBranchLocation(branchLocation, location1);
				}
			}
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

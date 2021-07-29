package postfix;

import static symboltable.SymbolType.CONSTANT;
import static symboltable.SymbolType.VARIABLE;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Stack;

import sml_package.Command;
import sml_package.SML_Compiler;
import symboltable.UnknownSymbolException;

public class PostfixEvaluator {

	private static final Stack<Integer> stack = new Stack<>();

	private static final Collection<String> keywords  = new HashSet<>();
	private static final Collection<String> operators = new HashSet<>();

	static {
		keywords.addAll(Arrays.asList("rem", "input", "if", "goto", "let", "print", "end"));
		operators.addAll(Arrays.asList("+", "-", "*", "/", "^", "%", "(", ")"));
	}

	public static void evaluatePostfix(List<Token> postfix) {

		for (Token token : postfix) {
			String c = token.value;

			if (isConstant(c)) {
				int location;
				try {
					location = SML_Compiler.symbolTable.getSymbolLocation(c, CONSTANT);
				} catch (UnknownSymbolException e) {
					location = SML_Compiler.addConstant(Integer.parseInt(c));
					SML_Compiler.symbolTable.addEntry(c, CONSTANT, location, "");
				}
				stack.push(location);
			} else if (isVariable(c)) {
				int location;
				try {
					location = SML_Compiler.symbolTable.getSymbolLocation(c, VARIABLE);
				} catch (UnknownSymbolException e) {
					System.out.printf("%s:%02d: error: variable %s not found\n",
					        SML_Compiler.inputFileName, SML_Compiler.line_count, c);
					SML_Compiler.succ = false;
					return;
				}
				stack.push(location);

			} else if (isOperator(c)) {
				int y = stack.pop();
				int x = stack.pop();

				int instruction;

				if (c.equals("+"))
					instruction = Command.ADD.opcode();
				else if (c.equals("-"))
					instruction = Command.SUBTRACT.opcode();
				else if (c.equals("*"))
					instruction = Command.MULTIPLY.opcode();
				else if (c.equals("/"))
					instruction = Command.DIVIDE.opcode();
				else if (c.equals("%"))
					instruction = Command.MOD.opcode();
				else if (c.equals("^"))
					instruction = Command.POW.opcode();
				else
					instruction = -1;

				int tempResultLocation = SML_Compiler.addVariable();

				SML_Compiler.addInstruction(Command.LOAD.opcode() + x);
				SML_Compiler.addInstruction(instruction + y);
				SML_Compiler.addInstruction(Command.STORE.opcode() + tempResultLocation);

				stack.push(tempResultLocation);
			}
		}
		SML_Compiler.addInstruction(Command.LOAD.opcode() + stack.pop());
	}

	private static boolean isConstant(String con) {
		try {
			Integer.parseInt(con);
			return true;
		} catch (NumberFormatException e) {
			return false;
		}
	}

	private static boolean isVariable(String var) {
		try {
			Integer.parseInt(var);
			return false;
		} catch (NumberFormatException e) {
			return !keywords.contains(var) && !isOperator(var);
		}
	}

	private static boolean isOperator(String operator) {
		return operators.contains(operator);
	}
}

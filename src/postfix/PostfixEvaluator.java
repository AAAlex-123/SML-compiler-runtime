package postfix;

import static symboltable.SymbolType.CONSTANT;
import static symboltable.SymbolType.VARIABLE;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Stack;

import sml_package.SML_Compiler;
import sml_package.SML_Executor;
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
				int loc;
				try {
					loc = SML_Compiler.symbolTable.getSymbolLocation(c, CONSTANT);
				} catch (UnknownSymbolException e) {
					loc = SML_Compiler.memory.writeConstant(Integer.parseInt(c));
					SML_Compiler.symbolTable.addEntry(c, CONSTANT, loc, "");
				}
				stack.push(loc);
			} else if (isVariable(c)) {
				int loc;
				try {
					loc = SML_Compiler.symbolTable.getSymbolLocation(c, VARIABLE);
				} catch (UnknownSymbolException e) {
					System.out.printf("%s:%02d: error: variable %s not found\n",
					        SML_Compiler.inputFileName, SML_Compiler.line_count, c);
					SML_Compiler.succ = false;
					return;
				}
				stack.push(loc);

			} else if (isOperator(c)) {
				int y = stack.pop();
				int x = stack.pop();

				int instruction;

				if (c.equals("+"))
					instruction = SML_Executor.ADD;
				else if (c.equals("-"))
					instruction = SML_Executor.SUBTRACT;
				else if (c.equals("*"))
					instruction = SML_Executor.MULTIPLY;
				else if (c.equals("/"))
					instruction = SML_Executor.DIVIDE;
				else if (c.equals("%"))
					instruction = SML_Executor.MOD;
				else if (c.equals("^"))
					instruction = SML_Executor.POW;
				else
					instruction = -1;

				int tempResultLocation = SML_Compiler.memory.assignPlaceForVariable();

				SML_Compiler.memory.writeInstruction((SML_Executor.LOAD * 0x100) + x);
				SML_Compiler.memory.writeInstruction((instruction * 0x100) + y);
				SML_Compiler.memory
				        .writeInstruction((SML_Executor.STORE * 0x100) + tempResultLocation);

				stack.push(tempResultLocation);
			}
		}
		SML_Compiler.memory.writeInstruction((SML_Executor.LOAD * 0x100) + stack.pop());
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

package postfix;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class InfixToPostfix {

	private static final Stack<Token> stack = new Stack<>();
	private static final List<Token> postfix = new ArrayList<>();

	public static void main(String[] args) {
		String[] tests = new String[] {
		        "5 + 3 * 2",
		        "(6 + 2* 5 - 8 / 4",
		        "(6 + a* 5 - b / c",
		        "(6 + hello* 5 - world / text",
		        "(6+    a    *(5 -xdb)+  c",
		        "(6+    a    *(5 -xdb)+  c)"
		};

		for (String test : tests)
			System.out.printf("%s: %s%n", test, convertToPostfix(test));
	}

	public static List<Token> convertToPostfix(String infix) {
		stack.clear();
		postfix.clear();

		stack.push(Token.LEFT_PAREN);

		for (Token t : InfixTokeniser.tokenise(infix + ")")) {

			if (t == Token.LEFT_PAREN) {
				stack.push(t);

			} else if (t == Token.RIGHT_PAREN) {
				while (stack.peek().isOperator()) {
					postfix.add(stack.pop());
				}
				stack.pop();

			} else if (t.isOperator()) {
				while (stack.peek().isOperator() && !precedence(t, stack.peek()))
					postfix.add(stack.pop());

				stack.push(t);

			} else {
				postfix.add(t);
			}
		}

		return new ArrayList<>(postfix);
	}

	private static boolean precedence(Token op1, Token op2) {
		if (op1 == Token.POW)
			return true;

		if ((op1 == Token.MUL) || (op1 == Token.DIV) || (op1 == Token.MOD))
			return (op2 == Token.ADD) || (op2 == Token.SUB);

		return false;
	}
}

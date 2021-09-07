package compiler.postfix;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

/**
 * Defines the static method {@link InfixToPostfix#convertToPostfix(String)
 * convertToPostfix(String)} which converts the {@code infix} expression given
 * as a String to its equivalent {@code postfix} expression represented by a
 * list of {@link compiler.postfix.Token Tokens}.
 *
 * @author Alex Mandelias
 */
public final class InfixToPostfix {

	/* Don't let anyone instantiate this class */
	private InfixToPostfix() {}

	/**
	 * Converts the {@code infix} expression of the form "{@code a + b}", to its
	 * equivalent postfix expression of the form "{@code a b +}". The postfix
	 * expression is represented by a list of {@code Tokens}.
	 * <p>
	 * TODO: error checking
	 *
	 * @param infix the infix expression
	 *
	 * @return the postfix expression
	 *
	 * @see compiler.postfix.Token
	 */
	public static List<Token> convertToPostfix(String infix) {
		final List<Token>  postfix = new ArrayList<>();
		final Stack<Token> stack   = new Stack<>();

		stack.push(Token.LEFT_PAREN);

		final String toTokenise = infix + Token.RIGHT_PAREN.value;

		for (final Token t : InfixToPostfix.tokenise(toTokenise)) {

			if (t == Token.LEFT_PAREN) {
				stack.push(t);

			} else if (t == Token.RIGHT_PAREN) {
				while (stack.peek().isOperator())
					postfix.add(stack.pop());

				stack.pop();

			} else if (t.isOperator()) {
				while (stack.peek().isOperator() && !InfixToPostfix.precedence(t, stack.peek()))
					postfix.add(stack.pop());

				stack.push(t);

			} else
				postfix.add(t);
		}

		return postfix;
	}

	// returns true iff op1 has higher precedence over op2
	private static boolean precedence(Token op1, Token op2) {
		if (op1 == Token.POW)
			return true;

		if ((op1 == Token.MUL) || (op1 == Token.DIV) || (op1 == Token.MOD))
			return (op2 == Token.ADD) || (op2 == Token.SUB);

		return false;
	}

	private static final String splitInfixToSymbolsRegex = "(?<=[^\\.:a-zA-Z\\d])|(?=[^\\.:a-zA-Z\\d])";

	// returns a list of the Tokens found in the infix expression
	private static List<Token> tokenise(String infix) {
		final List<Token> tokenList = new ArrayList<>();

		final String parts[] = infix.replaceAll("\\s+", "")
		        .split(InfixToPostfix.splitInfixToSymbolsRegex);

		for (final String token : parts)
			tokenList.add(Token.of(token));

		return tokenList;
	}
}

package compiler.postfix;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;

/**
 * Wrapper for different tokens (strings) that can appear in infix and postfix
 * expressions. Tokens are constructed with the {@link Token#of(String)
 * of(String)} method to allow reuse of frequently used Tokens, which are also
 * public members of this class.
 *
 * @author Alex Mandelias
 */
public class Token {

	/** Token for Left Parenthesis */
	public static final Token LEFT_PAREN = new Token("(");

	/** Token for Right Parenthesis */
	public static final Token RIGHT_PAREN = new Token(")");

	/** Token for Addition */
	public static final Token ADD = new Token("+");

	/** Token for Subtraction */
	public static final Token SUB = new Token("-");

	/** Token for Multiplication */
	public static final Token MUL = new Token("*");

	/** Token for Division */
	public static final Token DIV = new Token("/");

	/** Token for Exponentiation */
	public static final Token POW = new Token("^");

	/** Token for Modulo Division */
	public static final Token MOD = new Token("%");

	private static final Collection<Token> operators = new HashSet<>(
	        Arrays.asList(Token.ADD, Token.SUB, Token.MUL, Token.DIV, Token.POW, Token.MOD));

	/**
	 * Returns a {@code Token} with a specific {@code value}.
	 *
	 * @param value the value
	 *
	 * @return a Token with that value
	 */
	public static Token of(String value) {
		for (final Token token : Token.operators)
			if (token.value.equals(value))
				return token;

		switch (value) {
		case "(":
			return Token.LEFT_PAREN;
		case ")":
			return Token.RIGHT_PAREN;
		default:
			return new Token(value);
		}
	}

	/** The value the Token encapsulates */
	public final String value;

	/**
	 * Returns whether or not this {@code Token} represents a mathematical operator.
	 *
	 * @return {@code true} if this Token is a mathematical operator, {@code false}
	 *         otherwise
	 */
	public boolean isOperator() {
		return Token.operators.contains(this);
	}

	/**
	 * Returns whether or not this {@code Token} represents a mathematical operator
	 * or a parenthesis.
	 *
	 * @return {@code true} if this Token is a mathematical operator or parenthesis,
	 *         {@code false} otherwise
	 */
	public boolean isOperatorOrParenthesis() {
		return isOperator() || (this == Token.LEFT_PAREN) || (this == Token.RIGHT_PAREN);
	}

	// instantiate only using the `of(String)` method
	private Token(String value) {
		this.value = value;
	}

	@Override
	public String toString() {
		return value;
	}
}

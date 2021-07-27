package postfix;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;

public class Token {

	public static final Token LEFT_PAREN  = new Token("(");
	public static final Token RIGHT_PAREN = new Token(")");
	public static final Token ADD = new Token("+");
	public static final Token SUB = new Token("-");
	public static final Token MUL = new Token("*");
	public static final Token DIV = new Token("/");
	public static final Token POW = new Token("^");
	public static final Token MOD = new Token("%");

	private static final Collection<Token> operators = new HashSet<>(
	        Arrays.asList(ADD, SUB, MUL, DIV, POW, MOD));

	public final String value;

	public static Token of(String s) {
		switch (s) {
		case "(":
			return LEFT_PAREN;
		case ")":
			return RIGHT_PAREN;
		case "+":
			return ADD;
		case "-":
			return SUB;
		case "*":
			return MUL;
		case "/":
			return DIV;
		case "^":
			return POW;
		case "%":
			return MOD;
		default:
			return new Token(s);
		}
	}

	public boolean isOperator() {
		return operators.contains(this);
	}

	private Token(String value) {
		this.value = value;
	}

	@Override
	public String toString() {
		return value;
	}
}

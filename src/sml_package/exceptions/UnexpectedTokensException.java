package sml_package.exceptions;
public class UnexpectedTokensException extends Exception {

	public UnexpectedTokensException(String tokens) {
		super(String.format("Unexpected tokens: '%s'", tokens));
	}
}

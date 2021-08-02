package sml_package.exceptions;

public class UnexpectedTokenException extends Exception {
	public UnexpectedTokenException(String token, String target) {
		super(String.format("Unexpected token %s. Expeted: %s", token, target));
	}
}

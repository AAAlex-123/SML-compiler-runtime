package sml_package.exceptions;

public class NotAVariableException extends Exception {
	public NotAVariableException(String name) {
		super(String.format("'%s' is not a variable", name));
	}
}

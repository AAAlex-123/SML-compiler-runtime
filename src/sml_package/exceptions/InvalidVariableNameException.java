package sml_package.exceptions;

public class InvalidVariableNameException extends Exception {

	public InvalidVariableNameException(String name) {
		super(String.format("'%s' is not a valid variable name", name));
	}
}

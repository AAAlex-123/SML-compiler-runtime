package sml_package.exceptions;

public class VariableAlreadyDeclaredException extends Exception {

	public VariableAlreadyDeclaredException(String variable) {
		super(String.format("variable '%s' already declared", variable));
	}
}

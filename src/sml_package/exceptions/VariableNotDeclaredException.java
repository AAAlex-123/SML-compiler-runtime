package sml_package.exceptions;

public class VariableNotDeclaredException extends Exception {

	public VariableNotDeclaredException(String variable) {
		super(String.format("variable '%s' is not declared", variable));
	}
}

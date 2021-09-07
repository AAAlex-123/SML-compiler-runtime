package compiler.exceptions;

/**
 * Thrown when a Variable that has not been declared is referenced.
 *
 * @author Alex Mandelias
 */
public class VariableNotDeclaredException extends CompilerException {

	/**
	 * Constructs the exception with a {@code variable}.
	 *
	 * @param variable the variable that has not been declared
	 */
	public VariableNotDeclaredException(String variable) {
		super("variable '%s' is not declared", variable);
	}
}

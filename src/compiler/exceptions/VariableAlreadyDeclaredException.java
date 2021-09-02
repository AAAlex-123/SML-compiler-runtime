package compiler.exceptions;

/**
 * Thrown when an already-declared Variable is re-declared.
 *
 * @author Alex Mandelias
 */
public class VariableAlreadyDeclaredException extends CompilerException {

	/**
	 * Constructs the exception with a {@code variable}.
	 *
	 * @param variable the variable that was already declared
	 */
	public VariableAlreadyDeclaredException(String variable) {
		super("variable '%s' already declared", variable);
	}
}

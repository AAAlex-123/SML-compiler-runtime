package compiler.exceptions;

/**
 * Thrown when an already-declared variable is re-declared.
 *
 * @author Alex Mandelias
 */
public class VariableAlreadyDeclaredException extends CompilerException {

	/**
	 * Constructs the exception with the {@code variable} that was already declared.
	 *
	 * @param variable the variable that was already declared
	 */
	public VariableAlreadyDeclaredException(String variable) {
		super("variable '%s' already declared", variable);
	}
}

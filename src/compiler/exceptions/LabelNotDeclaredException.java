package compiler.exceptions;

/**
 * Thrown when a Variable that has not been declared is referenced.
 *
 * @author Alex Mandelias
 */
public class LabelNotDeclaredException extends CompilerException {

	/**
	 * Constructs the exception with the {@code variable} that has not been declared
	 *
	 * @param label the variable that has not been declared
	 */
	public LabelNotDeclaredException(String label) {
		super("variable '%s' is not declared", label);
	}
}

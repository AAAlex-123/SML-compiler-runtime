package compiler.exceptions;

/**
 * Thrown when a Label that has not been declared is referenced.
 *
 * @author Alex Mandelias
 */
public class LabelNotDeclaredException extends CompilerException {

	/**
	 * Constructs the exception with a {@code label}.
	 *
	 * @param label the label that has not been declared
	 */
	public LabelNotDeclaredException(String label) {
		super("label '%s' is not declared", label);
	}
}

package compiler.exceptions;

/**
 * Thrown when an already-declared Label is re-declared.
 *
 * @author Alex Mandelias
 */
public class LabelAlreadyDeclaredException extends CompilerException {

	/**
	 * Constructs the exception with a {@code label}.
	 *
	 * @param label the label that was already declared
	 */
	public LabelAlreadyDeclaredException(String label) {
		super("label '%s' already declared", label);
	}
}

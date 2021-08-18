package compiler.exceptions;

/**
 * Thrown when an already-declared label is re-declared.
 *
 * @author Alex Mandelias
 */
public class LabelAlreadyDeclaredException extends CompilerException {

	/**
	 * Constructs the exception with the {@code label} that was already declared.
	 *
	 * @param symbol the label that was already declared
	 */
	public LabelAlreadyDeclaredException(String symbol) {
		super("label '%s' already declared", symbol);
	}
}

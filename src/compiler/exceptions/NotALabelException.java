package compiler.exceptions;

/**
 * Thrown when a symbol that is expected to be a Label, is not a Label.
 *
 * @author Alex Mandelias
 */
public class NotALabelException extends CompilerException {

	/**
	 * Constructs the exception with a {@code symbol}.
	 *
	 * @param symbol the symbol that isn't a Label
	 */
	public NotALabelException(String symbol) {
		super("'%s' is not a label", symbol);
	}
}

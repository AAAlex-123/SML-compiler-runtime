package compiler.exceptions;

/**
 * Thrown when a symbol that is expected to be a Label, is not a Label.
 *
 * @author Alex Mandelias
 */
public class NotALabel extends CompilerException {

	/**
	 * Constructs the exception with information about the {@code symbol} that isn't
	 * a Label.
	 *
	 * @param symbol the symbol that isn't a label
	 */
	public NotALabel(String symbol) {
		super("'%s' is not a label", symbol);
	}
}

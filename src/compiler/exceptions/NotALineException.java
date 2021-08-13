package compiler.exceptions;

/**
 * Thrown when a symbol that is expected to be a Line, is not a Line.
 *
 * @author Alex Mandelias
 */
public class NotALineException extends CompilerException {

	/**
	 * Constructs the exception with information about the {@code symbol} that isn't
	 * a Line.
	 *
	 * @param symbol the symbol that isn't a line
	 */
	public NotALineException(String symbol) {
		super("'%s' is not a line", symbol);
	}
}

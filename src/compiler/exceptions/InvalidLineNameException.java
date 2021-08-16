package compiler.exceptions;

/**
 * Thrown when a symbol that does not correspond to a Line name was found when
 * declaring a line.
 *
 * @author Alex Mandelias
 */
public class InvalidLineNameException extends CompilerException {

	/**
	 * Constructs the exception with the {@code symbol} that isn't a valid line
	 * name.
	 *
	 * @param symbol the symbol that isn't a valid line name.
	 */
	public InvalidLineNameException(String symbol) {
		super("'%s' is not a valid line name", symbol);
	}

}

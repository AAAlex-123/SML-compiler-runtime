package compiler.exceptions;

/**
 * Thrown when a symbol that does not correspond to a Line name was found when
 * declaring a line.
 *
 * @author Alex Mandelias
 */
public class InvalidLineNameException extends CompilerException {

	/**
	 * Constructs the exception with a {@code symbol}.
	 *
	 * @param line the symbol that isn't a valid Line name.
	 */
	public InvalidLineNameException(String line) {
		super("'%s' is not a valid line name", line);
	}
}

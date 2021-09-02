package compiler.exceptions;

/**
 * Thrown when a symbol that does not correspond to a Label name was found in a
 * label declaration statement.
 *
 * @author Alex Mandelias
 */
public class InvalidLabelNameException extends CompilerException {

	/**
	 * Constructs the exception with a {@code symbol}.
	 *
	 * @param label the symbol that isn't a valid Label name.
	 */
	public InvalidLabelNameException(String label) {
		super("'%s' is not a valid label name", label);
	}
}

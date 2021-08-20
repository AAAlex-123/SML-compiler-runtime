package compiler.exceptions;

/**
 * Thrown when a symbol that does not correspond to a Label name was found in a
 * label declaration statement.
 *
 * @author Alex Mandelias
 */
public class InvalidLabelNameException extends CompilerException {

	/**
	 * Constructs the exception with the {@code symbol} that isn't a valid label
	 * name.
	 *
	 * @param label the symbol that isn't a valid label name.
	 */
	public InvalidLabelNameException(String label) {
		super("'%s' is not a valid label name", label);
	}
}

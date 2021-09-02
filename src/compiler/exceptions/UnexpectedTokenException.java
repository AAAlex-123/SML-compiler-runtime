package compiler.exceptions;

/**
 * Thrown when a Token different that the expected one was found during parsing.
 *
 * @author Alex Mandelias
 */
public class UnexpectedTokenException extends CompilerException {

	/**
	 * Constructs the exception with the {@code found} and the {@code expected}
	 * Tokens. The Strings for the Tokens will be formatted as-is in the exception
	 * message, no quotes will be added around them.
	 *
	 * @param found    the token that was found
	 * @param expected the token that was expected
	 */
	public UnexpectedTokenException(String found, String expected) {
		super("Unexpected token %s. Expeted: %s", found, expected);
	}
}

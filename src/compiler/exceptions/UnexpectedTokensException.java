package compiler.exceptions;

/**
 * Thrown when a series of Tokens was found during parsing that shouldn't be
 * there.
 *
 * @author Alex Mandelias
 */
public class UnexpectedTokensException extends CompilerException {

	/**
	 * Constructs the exception with information about the unexpected
	 * {@code tokens}.
	 *
	 * @param tokens the tokens that shouldn't be there
	 */
	public UnexpectedTokensException(String tokens) {
		super("Unexpected tokens: '%s'", tokens);
	}
}

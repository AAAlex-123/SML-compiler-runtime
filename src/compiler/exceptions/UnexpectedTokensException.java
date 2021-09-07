package compiler.exceptions;

/**
 * Thrown when at least one excessive Token was found during parsing.
 *
 * @author Alex Mandelias
 */
public class UnexpectedTokensException extends CompilerException {

	/**
	 * Constructs the exception with the unexpected {@code Tokens}.
	 *
	 * @param tokens the Tokens that shouldn't be there
	 */
	public UnexpectedTokensException(String tokens) {
		super("Unexpected tokens: '%s'", tokens);
	}
}

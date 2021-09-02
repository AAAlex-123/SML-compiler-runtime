package compiler.exceptions;

/**
 * Thrown when a high-level-program statement was read but its identifier
 * doesn't match any of the Compiler's {@code Statements}.
 *
 * @author Alex Mandelias
 */
public class InvalidStatementException extends CompilerException {

	/**
	 * Constructs the Exception with the {@code identifier}.
	 *
	 * @param identifier the identifier for which there is no {@code Statement}
	 */
	public InvalidStatementException(String identifier) {
		super("Invalid Statement identifier: %s", identifier);
	}
}

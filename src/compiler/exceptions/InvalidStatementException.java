package compiler.exceptions;

/**
 * Thrown when an high-level-program statement was read but its identifier
 * doesn't match any {@link runtime.Statement Statement}.
 *
 * @author Alex Mandelias
 */
public class InvalidStatementException extends CompilerException {

	/**
	 * Constructs the Exception with information about the {@code identifier}.
	 *
	 * @param identifier the identifier for which there is no {@code Statement}
	 */
	public InvalidStatementException(String identifier) {
		super("Invalid Statement identifier: %s", identifier);
	}
}

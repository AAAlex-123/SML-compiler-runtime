package compiler.exceptions;

/**
 * Thrown when a high-level-program condition was read but its identifier
 * doesn't match any of the Compiler's {@code Conditions}.
 *
 * @author Alex Mandelias
 */
public class InvalidConditionException extends CompilerException {

	/**
	 * Constructs the Exception with the {@code identifier}.
	 *
	 * @param identifier the identifier for which there is no {@code Condition}
	 */
	public InvalidConditionException(String identifier) {
		super("Invalid Condition identifier: %s", identifier);
	}
}

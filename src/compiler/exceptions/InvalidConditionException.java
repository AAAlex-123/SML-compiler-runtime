package compiler.exceptions;

/**
 * Thrown when an high-level-program condition was read but its identifier
 * doesn't match any {@link compiler.Condition Condition}.
 *
 * @author Alex Mandelias
 */
public class InvalidConditionException extends CompilerException {

	/**
	 * Constructs the Exception with information about the {@code identifier}.
	 *
	 * @param identifier the identifier for which there is no {@code Condition}
	 */
	public InvalidConditionException(String identifier) {
		super("Invalid Condition identifier: %s", identifier);
	}
}

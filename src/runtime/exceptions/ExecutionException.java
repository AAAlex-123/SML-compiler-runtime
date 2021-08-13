package runtime.exceptions;

/**
 * Superclass of all checked exceptions that may occur during execution of
 * machine code.
 *
 * @author Alex Mandelias
 */
public abstract class ExecutionException extends Exception {

	/**
	 * Constructs the Exception with a message formed by formatting the {@code text}
	 * with the {@code args} as if {@code String.format(text, args)} was called.
	 *
	 * @param text the text to format
	 * @param args the format arguments
	 */
	public ExecutionException(String text, Object... args) {
		super(String.format(text, args));
	}
}

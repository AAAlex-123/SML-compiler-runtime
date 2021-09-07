package compiler.exceptions;

/**
 * A superclass for all of the Exceptions that may occur during compilation of a
 * program written in the high-level language.
 *
 * @author Alex Mandelias
 */
public abstract class CompilerException extends Exception {

	/**
	 * Constructs the {@code CompilerException} by formatting the {@code text} with
	 * the {@code args}, which are provided by the constructors of the subclasses.
	 *
	 * @param text a short and descriptive message
	 * @param args the format arguments
	 */
	public CompilerException(String text, Object... args) {
		super(String.format(text, args));
	}
}

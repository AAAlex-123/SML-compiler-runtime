package compiler.exceptions;

/**
 * Thrown when a symbol that is expected to be a Variable, is not a Variable.
 *
 * @author Alex Mandelias
 */
public class NotAVariableException extends CompilerException {

	/**
	 * Constructs the exception with a {@code symbol}.
	 *
	 * @param symbol the symbol that isn't a Variable
	 */
	public NotAVariableException(String symbol) {
		super("'%s' is not a variable", symbol);
	}
}


package compiler.exceptions;

/**
 * Thrown when a symbol that is expected to be a Variable, is not a Variable.
 *
 * @author Alex Mandelias
 */
public class NotAVariableException extends CompilerException {

	/**
	 * Constructs the exception with information about the {@code symbol} that isn't
	 * a Variable.
	 *
	 * @param symbol the symbol that isn't a variable
	 */
	public NotAVariableException(String symbol) {
		super("'%s' is not a variable", symbol);
	}
}
	

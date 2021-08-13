package compiler.exceptions;

/**
 * Thrown when a symbol that does not correspond to a Variable name was found in
 * a variable declaration statement.
 *
 * @author Alex Mandelias
 */
public class InvalidVariableNameException extends CompilerException {

	/**
	 * Constructs the exception with the {@code symbol} that isn't a valid variable
	 * name.
	 *
	 * @param symbol the symbol that isn't a valid variable name.
	 */
	public InvalidVariableNameException(String symbol) {
		super("'%s' is not a valid variable name", symbol);
	}
}

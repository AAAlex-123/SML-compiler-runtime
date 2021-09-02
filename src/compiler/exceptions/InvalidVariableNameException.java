package compiler.exceptions;

/**
 * Thrown when a symbol that does not correspond to a Variable name was found in
 * a variable declaration statement.
 *
 * @author Alex Mandelias
 */
public class InvalidVariableNameException extends CompilerException {

	/**
	 * Constructs the exception with a {@code symbol}.
	 *
	 * @param variable the symbol that isn't a valid Variable name.
	 */
	public InvalidVariableNameException(String variable) {
		super("'%s' is not a valid variable name", variable);
	}
}

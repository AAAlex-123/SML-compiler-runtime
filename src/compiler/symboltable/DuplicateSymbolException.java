package compiler.symboltable;

/**
 * Thrown when a high-level-language {@code symbol} exists in a
 * {@code Symbol Table} and is added for a second time.
 *
 * @author Alex Mandelias
 */
public class DuplicateSymbolException extends RuntimeException {

	/**
	 * Constructs the Exception with a {@code key}.
	 *
	 * @param key the key of the duplicate symbol
	 */
	public DuplicateSymbolException(SymbolKey key) {
		super(String.format("Symbol '%s' of type '%s' already exists", key.symbol, key.type));
	}
}

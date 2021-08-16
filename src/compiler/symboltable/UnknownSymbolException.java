package compiler.symboltable;

/**
 * Thrown when a high-level-language {@code symbol} is missing from a Symbol
 * Table.
 *
 * @author Alex Mandelias
 */
public class UnknownSymbolException extends RuntimeException {

	/**
	 * Constructs the Exception with the given {@code symbol}. The symbol can be of
	 * any type.
	 *
	 * @param symbol the missing symbol
	 */
	public UnknownSymbolException(String symbol) {
		super(String.format("Can't find Symbol: %s", symbol));
	}

	/**
	 * Constructs the Exception with the given {@code symbol}. The symbol can be
	 * only one of the types provided.
	 *
	 * @param symbol the missing symbol
	 * @param types  the possible types of the symbol
	 */
	public UnknownSymbolException(String symbol, SymbolType... types) {
		super(String.format("Can't find Symbol: %s of Type(s): %s", symbol, types));
	}
}

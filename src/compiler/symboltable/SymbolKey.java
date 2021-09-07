package compiler.symboltable;

/**
 * A key for high-level-language Symbols. If the {@link SymbolInfo information}
 * of two Symbols have the same Key, they are considered identical.
 *
 * @author Alex Mandelias
 */
class SymbolKey {

	/** The Symbol */
	public final String symbol;

	/** The Type of the Symbol */
	public final SymbolType type;

	/**
	 * Creates a SymbolKey with the {@code information} about a Symbol.
	 *
	 * @param info the information
	 */
	public SymbolKey(SymbolInfo info) {
		this(info.symbol, info.type);
	}

	/**
	 * Creates a SymbolKey with a specific {@code symbol} and {@code type}.
	 *
	 * @param symbol the symbol
	 * @param type   the type
	 */
	public SymbolKey(String symbol, SymbolType type) {
		this.symbol = symbol;
		this.type = type;
	}

	@Override
	public boolean equals(Object other) {
		if (!(other instanceof SymbolKey))
			return false;

		final SymbolKey other1 = (SymbolKey) other;
		return (other1.symbol.equals(symbol)) && (other1.type == type);
	}

	@Override
	public int hashCode() {
		return symbol.hashCode() + type.hashCode();
	}
}

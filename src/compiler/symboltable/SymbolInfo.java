package compiler.symboltable;

/**
 * Contains information about a high-level-language Symbol with a specific
 * {@link SymbolType Type} that is saved at a specific {@code location} in
 * memory and, if it is a variable or a constant, has a {@code varType} (e.g.
 * int, string).
 * <p>
 * Two high-level-language Symbols are considered identical if and only if their
 * {@link SymbolKey keys} are equal.
 *
 * @author Alex Mandelias
 */
public class SymbolInfo {

	/** The {@code Symbol} found in a high-level-language statement */
	public final String     symbol;

	/** The {@code Type} of the symbol */
	public final SymbolType type;

	/**
	 * The {@code location} of the symbol in the compiled program, its address in
	 * memory during execution.
	 */
	public final int        location;

	/** The type of the symbol, if it's a variable (int, string, etc.) */
	public final String     varType;

	/**
	 * Constructs the Table Entry.
	 *
	 * @param symbol   the symbol
	 * @param type     the type
	 * @param location the location
	 * @param varType  the type of variable
	 */
	public SymbolInfo(String symbol, SymbolType type, int location, String varType) {
		this.symbol = symbol;
		this.type = type;
		this.location = location;
		this.varType = varType;
	}

	/**
	 * Returns the {@code key} for the Symbol.
	 *
	 * @return the key
	 *
	 * @see SymbolKey
	 */
	public SymbolKey key() {
		return new SymbolKey(this);
	}

	@Override
	public boolean equals(Object other) {
		if (!(other instanceof SymbolInfo))
			return false;

		return this.key().equals(((SymbolInfo) other).key());
	}

	@Override
	public int hashCode() {
		return this.key().hashCode();
	}

	@Override
	public String toString() {
		return String.format("%12s%12s          %02x%12s", symbol, type, location, varType);
	}
}

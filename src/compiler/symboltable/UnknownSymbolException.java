package compiler.symboltable;

import java.util.Arrays;

/**
 * Thrown when a high-level-language {@code symbol} is missing from a
 * {@code Symbol Table}.
 *
 * @author Alex Mandelias
 */
public class UnknownSymbolException extends RuntimeException {

	/**
	 * Constructs the Exception with a {@code symbol} of any Type.
	 *
	 * @param symbol the missing symbol
	 *
	 * @see SymbolType
	 */
	public UnknownSymbolException(String symbol) {
		super(String.format("Can't find Symbol '%s'", symbol));
	}

	/**
	 * Constructs the Exception with a {@code symbol}. The symbol can be only one of
	 * the provided Types.
	 *
	 * @param symbol the missing symbol
	 * @param types  the possible types of the symbol
	 *
	 * @see SymbolType
	 */
	public UnknownSymbolException(String symbol, SymbolType... types) {
		super(String.format("Can't find Symbol '%s' of Type(s): %s", symbol,
		        Arrays.toString(types)));
	}
}

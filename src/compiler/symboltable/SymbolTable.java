package compiler.symboltable;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Encapsulates information about different {@code symbols} found when compiling
 * a high-level-language program. The Symbol Table stores {@link SymbolInfo
 * information} about each Symbol that must be {@code unique}. For the sake of
 * the implementation's completeness, RuntimeExceptions are thrown when
 * information about unknown symbols is requested and when duplicate entries are
 * added.
 *
 * @author Alex Mandelias
 */
public class SymbolTable {

	private final Map<SymbolKey, SymbolInfo> map;

	/** Constructs an empty SymbolTable */
	public SymbolTable() {
		map = new LinkedHashMap<>();
	}

	/**
	 * Creates a {@code TableEntry} with the given parameters and adds it to the
	 * table. A RuntimeException is thrown if another Entry with the same
	 * {@code symbol} and {@code type} already exists.
	 *
	 * @param symbol   the symbol
	 * @param type     the type
	 * @param location the location
	 * @param varType  the varType
	 */
	public void addEntry(String symbol, SymbolType type, int location, String varType) {
		SymbolInfo value = new SymbolInfo(symbol, type, location, varType);
		SymbolKey  key   = value.key();

		if (existsSymbol(key.symbol, key.type))
			throw new DuplicateSymbolException(key);

		map.put(key, value);
	}

	/**
	 * Returns whether or not a {@code symbol} of a specific {@code type} exists in
	 * the Table.
	 *
	 * @param symbol the symbol
	 * @param type   the type
	 *
	 * @return {@code true} if the {@code symbol} of the given {@code type} exists,
	 *         {@code false} otherwise
	 */
	public boolean existsSymbol(String symbol, SymbolType type) {
		return map.containsKey(new SymbolKey(symbol, type));
	}

	/**
	 * Returns {@code information} about the {@code symbol}.
	 *
	 * @param symbol the symbol
	 *
	 * @return information about the symbol.
	 */
	public SymbolInfo getSymbol(String symbol) {
		return getSymbol(symbol, SymbolType.values());
	}

	/**
	 * Returns information about the {@code symbol} if it is one of the specified
	 * {@code types}.
	 *
	 * @param symbol the symbol
	 * @param types  the types
	 *
	 * @return information about the symbol
	 */
	public SymbolInfo getSymbol(String symbol, SymbolType... types) {
		SymbolInfo info = null;

		for (SymbolType type : types)
			if ((info = map.get(new SymbolKey(symbol, type))) != null)
				break;

		if (info == null)
			throw new UnknownSymbolException(symbol, types);

		return info;
	}

	/** Clears the table */
	public void clear() {
		map.clear();
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(
		        String.format("%12s%12s%12s%12s\n", "Symbol", "Type", "Location", "Var_Type"));

		for (SymbolInfo info : map.values())
			sb.append(info).append(System.lineSeparator());

		return sb.toString();
	}
}

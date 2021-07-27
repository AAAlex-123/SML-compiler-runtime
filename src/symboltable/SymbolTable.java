package symboltable;

import static symboltable.SymbolType.LINE;
import static symboltable.SymbolType.VARIABLE;

import java.util.LinkedHashMap;
import java.util.Map;

public class SymbolTable {

	private class MapKey {
		public final String     symbol;
		public final SymbolType type;

		public MapKey(String symbol, SymbolType type) {
			this.symbol = symbol;
			this.type = type;
		}

		@Override
		public boolean equals(Object other) {
			return (((MapKey) other).symbol.equals(symbol)) && (((MapKey) other).type == type);
		}

		@Override
		public int hashCode() {
			return symbol.hashCode() + type.hashCode();
		}
	}

	private final Map<MapKey, TableEntry> map;

	public SymbolTable() {
		map = new LinkedHashMap<>();
	}

	public void addEntry(String symbol, SymbolType type, int location, String var_type) {
		MapKey     key   = new MapKey(symbol, type);
		TableEntry value = new TableEntry(symbol, type, location, var_type);
		map.put(key, value);
	}

	public int getSymbolLocation(String symbol) {//throws UnknownSymbolException {
		TableEntry te = null;
		for (SymbolType type : SymbolType.values()) {
			TableEntry temp = map.get(new MapKey(symbol, type));
			if (temp != null) {
				te = temp;
				break;
			}
		}

		if (te == null)
			throw new UnknownSymbolException(symbol);

		return te.getLocation();
	}

	public int getSymbolLocation(String symbol, SymbolType type) {//throws UnknownSymbolException {
		TableEntry te = map.get(new MapKey(symbol, type));
		if (te == null)
			throw new UnknownSymbolException(symbol, type);

		return te.getLocation();
	}

	public int getSymbolLocation(String symbol, SymbolType... types) {//throws UnknownSymbolException {
		TableEntry te = null;
		for (SymbolType type : types) {
			TableEntry temp = map.get(new MapKey(symbol, type));
			if (temp != null) {
				te = temp;
				break;
			}
		}

		if (te == null)
			throw new UnknownSymbolException(symbol, types);

		return te.getLocation();
	}

	public String getNextLine(String symbol) {//throws UnknownSymbolException {
		boolean found = false;
		for (TableEntry te : map.values()) {
			if (found && (te.getType() == LINE)) {
				return te.getSymbol();
			}

			found = (symbol.equals(te.getSymbol()) && (te.getType() == LINE));
		}

		throw new UnknownSymbolException(symbol);
	}

	public String getVarType(String symbol) {//throws UnknownSymbolException {
		for (SymbolType type : SymbolType.values()) {
			if (type == LINE)
				continue;

			TableEntry temp = map.get(new MapKey(symbol, type));
			if (temp != null)
				return temp.getVarType();
		}

		throw new UnknownSymbolException(symbol);
	}

	public void clear() {
		map.clear();
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(
		        String.format("%12s%12s%12s%12s\n", "Symbol", "Type", "Location", "Var_Type"));

		for (TableEntry te : map.values())
			sb.append(te).append(System.lineSeparator());

		return sb.toString();
	}

	public static void main(String[] args) {
		SymbolTable st = new SymbolTable();
		st.addEntry("5", LINE, 0, "");
		st.addEntry("12", LINE, 1, "");
		st.addEntry("'x'", VARIABLE, 99, "int");
		st.addEntry("'a'", VARIABLE, 99, "int");
		st.addEntry("'z'", VARIABLE, 99, "int");
		System.out.println(st);
	}
}

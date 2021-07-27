package symboltable;

import static symboltable.SymbolType.LINE;

public class TableEntry {

	private final String symbol;
	private final SymbolType type;
	private final int    location;
	private final String var_type;

	public TableEntry(String symbol, SymbolType type, int location, String var_type) {
		this.symbol = symbol;
		this.type = type;
		this.location = location;
		this.var_type = var_type;
	}

	public String getSymbol() {
		return this.symbol;
	}

	public SymbolType getType() {
		return this.type;
	}

	public int getLocation() {
		return this.location;
	}

	public String getVarType() {
		return this.var_type;
	}

	@Override
	public String toString() {
		String symbol_ = (type == LINE ? String.format("%02d", Integer.parseInt(symbol)) : symbol);
		return String.format("%12s%12s          %02x%12s", symbol_, type, location, var_type);
	}
}

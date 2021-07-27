package symboltable;

public class TableEntry {
	private final String symbol;
	private final char type;
	private final int location;
	private final String var_type;
	
	public TableEntry(String symbol, char type, int location, String var_type) {
		this.symbol = symbol;
		this.type = type;
		this.location = location;
		this.var_type = var_type;
	}
	
	public String getSymbol() {return this.symbol;}
	public char getType() {return this.type;}
	public int getLocation() {return this.location;}
	public String getVarType() {return this.var_type;}
	
	public String toString() {
		String symbol_ = (type == 'L' ? String.format("%02d", Integer.parseInt(symbol)) : symbol);
		return String.format("%12s%12c          %02x%12s", symbol_, type, location, var_type);
	}

	public static void main(String[] args) {
		TableEntry te = new TableEntry("10", 'L', 00, "");
		System.out.println(te);
	}
}

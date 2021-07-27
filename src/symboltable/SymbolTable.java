package symboltable;

class SymbolTable {
	private final TableEntry[] arr;
	private int index;	

	public SymbolTable() {
		arr = new TableEntry[100];
		index = 0;
	}
	
	void addEntry(String symbol, char type, int location, String var_type) {
		arr[index++] = new TableEntry(symbol, type, location, var_type);
	}
	int getSymbolLocation(String symbol) {
		for (TableEntry te : arr)
			if (te == null)
				return -1;
			else if (symbol.equals(te.getSymbol()))
				return te.getLocation();
		return -1;
	}
	int getSymbolLocation(String symbol, char type) {
		for (TableEntry te : arr) {
			if (te == null)
				return -1;
			else if (symbol.equals(te.getSymbol()) && type == te.getType())
				return te.getLocation();
		}
		return -1;
	}
	int getSymbolLocation(String symbol, char... types) {
		for (TableEntry te : arr) {
			if (te == null)
				return -1;
			for (char type : types) 
				if (symbol.equals(te.getSymbol()) && type == te.getType())
					return te.getLocation();
		}
		return -1;
	}
	String getNextLine(String symbol) {
		System.out.printf("searching for: %s\n", symbol);
		boolean found = false;
		for (TableEntry te : arr) {
			System.out.printf("curr: %s\n", te);
			if (te == null)
				return "-1";
			if (found) {
				if ('L' == te.getType())
					return te.getSymbol();
			} else
				found = (symbol.equals(te.getSymbol()) && 'L' == te.getType());
			System.out.printf("found: %s\n", found);
		}
		return "-1";
	}
	String getVarType(String symbol) {
		for (TableEntry te : arr)
			if (te == null)
				return "getVarTypeErr";
			else if (symbol.equals(te.getSymbol()) && te.getType() != 'L')
				return te.getVarType();
		return "getVarTypeErr";
	}
	void clear() {
		for (int i=0; i<arr.length; i++) arr[i] = null;
		index = 0;
	}

	public String toString() {
		String s = String.format("%12s%12s%12s%12s\n", "Symbol", "Type", "Location", "Var_Type");
		for (TableEntry te : arr) 
			if (te != null)
				s += te.toString() + "\n";
		return s;
	}

	public static void main(String[] args) {
		SymbolTable st = new SymbolTable();
		st.addEntry("5", 'L', 0, "");
		st.addEntry("12", 'L', 1, "");
		st.addEntry("'x'", 'V', 99, "int");
		st.addEntry("'a'", 'V', 99, "int");
		st.addEntry("'z'", 'V', 99, "int");
		System.out.println(st);
	}

}

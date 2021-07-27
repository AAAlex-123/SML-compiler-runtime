package symboltable;

public enum SymbolType {
	VARIABLE('V'),
	CONSTANT('C'),
	LINE('L');

	private char c;

	private SymbolType(char c) {
		this.c = c;
	}

	@Override
	public String toString() {
		return String.format("%c", c);
	}
}

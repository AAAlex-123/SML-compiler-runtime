package compiler.symboltable;

/**
 * The Type of a {@link SymbolInfo Symbol} found in a high-level-language
 * program. {@code Symbols} may represent Variables, Constants or Lines and have
 * the corresponding type.
 *
 * @author Alex Mandelias
 */
public enum SymbolType {

	/** Symbol for a Variable */
	VARIABLE('V'),

	/** Symbol for a Constant */
	CONSTANT('C'),

	/** Symbol for a Line */
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

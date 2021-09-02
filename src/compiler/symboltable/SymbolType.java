package compiler.symboltable;

import java.lang.reflect.InvocationTargetException;
import java.util.regex.Pattern;

import compiler.exceptions.CompilerException;
import compiler.exceptions.NotALabelException;
import compiler.exceptions.NotAVariableException;
import compiler.exceptions.UnexpectedTokenException;

/**
 * The Type of a {@link SymbolInfo Symbol} found in a high-level-language
 * program. {@code Symbols} may represent Variables, Constants or Labels and
 * have the corresponding type.
 *
 * @author Alex Mandelias
 */
public enum SymbolType {

	/** Symbol for a Variable */
	VARIABLE('V', "[a-zA-Z]\\w*", NotAVariableException.class),

	/** Symbol for a Constant */
	CONSTANT('C', "[1-9]\\d*", null),

	/** Symbol for a Line */
	LABEL('L', ":\\w*", NotALabelException.class);

	private char c;
	private Pattern pattern;
	private Class<? extends CompilerException> exc;

	private SymbolType(char c, String regex, Class<? extends CompilerException> exc) {
		this.c = c;
		pattern = Pattern.compile(regex);
		this.exc = exc;
	}

	public static void assertType(String symbol, SymbolType type) throws NotAVariableException {
		if (!type.pattern.matcher(symbol).find())
			try {
				throw type.exc.getDeclaredConstructor().newInstance();
			} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
					| InvocationTargetException | NoSuchMethodException | SecurityException | CompilerException e) {
				throw new RuntimeException(e);
			}
	}

	public static void assertNot(String symbol, SymbolType type) throws NotAVariableException {
		if (type.pattern.matcher(symbol).find())
			try {
				throw new UnexpectedTokenException(symbol, "not a " + type);
			} catch (IllegalArgumentException | SecurityException | CompilerException e) {
				throw new RuntimeException(e);
			}
	}

	@Override
	public String toString() {
		return String.format("%c", c);
	}
}

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
 * have the corresponding type. The Type is determined using a regular
 * expression that a {@code Symbol} must match. The {@code assert} methods use
 * them and may throw an {@link compiler.exceptions.CompilerException
 * CompmilerException} associated with each type when the assertion fails.
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

	/** The Pattern that Symbols of this Type match */
	public final Pattern pattern;

	private final char                               c;
	private final Class<? extends CompilerException> exc;

	SymbolType(char c, String regex, Class<? extends CompilerException> exc) {
		this.c = c;
		pattern = Pattern.compile(regex);
		this.exc = exc;
	}

	@Override
	public String toString() {
		return String.format("%c", c);
	}

	/**
	 * Asserts that the {@code symbol} matches a specific {@code type}.
	 *
	 * @param symbol the symbol
	 * @param type   the expected type of the symbol
	 *
	 * @throws CompilerException if the symbol doesn't match the type
	 */
	public static void assertType(String symbol, SymbolType type) throws CompilerException {
		if (!type.pattern.matcher(symbol).find())
			try {
				throw type.exc.getDeclaredConstructor(String.class).newInstance();
			} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
			        | InvocationTargetException | NoSuchMethodException | SecurityException e) {
				throw new RuntimeException(e);
			}
	}

	/**
	 * Asserts that the {@code symbol} does not match a specific {@code type}.
	 *
	 * @param symbol the symbol
	 * @param type   the expected type of the symbol
	 *
	 * @throws UnexpectedTokenException if the symbol matches the type
	 */
	public static void assertTypeNot(String symbol, SymbolType type)
	        throws UnexpectedTokenException {
		if (type.pattern.matcher(symbol).find())
			try {
				throw new UnexpectedTokenException(symbol, "not a " + type);
			} catch (IllegalArgumentException | SecurityException e) {
				throw new RuntimeException(e);
			}
	}
}

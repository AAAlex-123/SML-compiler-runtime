package compiler;

import java.util.HashMap;
import java.util.Map;

/**
 * The comparison operators that can appear in the expressions that evaluate the
 * condition of an {@code if} or a {@code while} block.
 *
 * @author Alex Mandelias
 */
enum Condition {

	/** "Less Than" condition */
	LT("<"),

	/** "Greater Than" condition */
	GT(">"),

	/** "Less Than or Equal" condition */
	LE("<="),

	/** "Greater Than or Equal" condition */
	GE(">="),

	/** "Equal" condition */
	EQ("=="),

	/** "Not Equal" condition */
	NE("!=");

	/** The string that this Condition represents */
	public final String value;

	private static final Map<String, Condition> map;

	static {
		map = new HashMap<>();
		for (final Condition c : Condition.values())
			Condition.map.put(c.value, c);
	}

	/**
	 * Returns the {@code Condition} whose {@code value} is the identifier.
	 *
	 * @param identifier the {@code value} of the Condition
	 *
	 * @return the Condition with the identifier as its value
	 */
	public static Condition of(String identifier) {
		return Condition.map.get(identifier);
	}

	Condition(String value) {
		this.value = value;
	}
}

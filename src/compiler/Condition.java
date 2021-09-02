package compiler;

import java.util.HashMap;
import java.util.Map;

import compiler.exceptions.InvalidConditionException;

/**
 * The comparison operators that can appear in the expressions that evaluate the
 * condition of an {@code if-goto}, {@code if} or a {@code while} block.
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
	 *
	 * @throws InvalidConditionException when there is no Condition with the given
	 *                                   identifier
	 */
	public static Condition of(String identifier) throws InvalidConditionException {

		final Condition condition = map.get(identifier);

		if (condition == null)
			throw new InvalidConditionException(identifier);

		return condition;
	}

	Condition(String value) {
		this.value = value;
	}
}

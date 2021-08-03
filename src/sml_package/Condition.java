package sml_package;

import java.util.HashMap;
import java.util.Map;

public enum Condition {

	LT("<"),
	GT(">"),
	LE("<="),
	GE(">="),
	EQ("=="),
	NE("!=");

	public final String value;

	private static final Map<String, Condition> map;

	static {
		map = new HashMap<>();
		for (Condition c : Condition.values())
			map.put(c.value, c);
	}

	public static Condition of(String identifier) {
		return map.get(identifier);
	}

	private Condition(String value) {
		this.value = value;
	}
}

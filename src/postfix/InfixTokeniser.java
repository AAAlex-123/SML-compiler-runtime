package postfix;

import java.util.ArrayList;
import java.util.List;

public final class InfixTokeniser {

	private static final List<Token> tokenList = new ArrayList<>();

	private static final String regex = "(?<=[^\\.a-zA-Z\\d])|(?=[^\\.a-zA-Z\\d])";

	public static List<Token> tokenise(String infix) {

		tokenList.clear();

		String parts[] = infix.replaceAll("\\s+", "").split(regex);

		for (String token : parts)
			tokenList.add(Token.of(token));

		return new ArrayList<>(tokenList);
	}
}

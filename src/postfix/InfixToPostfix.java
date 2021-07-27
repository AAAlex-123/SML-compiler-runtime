package postfix;

public class InfixToPostfix {
	private static final MyStack<Character> stack = new MyStack<Character>();
	
	private static char[] operators = new char[] {'+', '-', '*', '/', '^', '%'};
	private static char[] digits = new char[] {'1', '2', '3', '4', '5', '6', '7', '8', '9', '0'};
	
	public static void main(String[] args) {
		String my_postfix = convertToPostfix("5 + 3 * 2");
		String postfix = convertToPostfix("(6 + 2) * 5 - 8 / 4");
		String postfix_var = convertToPostfix("(6 + a) * 5 - b / c");

		System.out.println("my_postfix: " + my_postfix);
		System.out.println("postfix: " + postfix);
		System.out.println("postfix_var: " + postfix_var);
	}
	
	static String convertToPostfix(String infix) {	
//=====================================================
		//		UN-SPAGHETTIFY PLS
		String postfix = "";
		int start, end, index = 0;
		boolean integer;
		char c = 'z';
		stack.push('(');
		infix += ")";
		while (!stack.isEmpty()) {
			integer = false;
			
			start = index;
			integer = isDigit(infix.charAt(index)) || isVariable(infix.charAt(index));
			while (isDigit(infix.charAt(index)) || isVariable(infix.charAt(index))) {index++;}
			end = index;
			
			if (!integer) {c = infix.charAt(index++);}
//=====================================================
			
			if (integer) {
				postfix += infix.substring(start, end) + " ";
			} else if (c == '(') {
				stack.push(c);
			} else if (isOperator(c)) {
				while(isOperator(stack.peek()) && !precedence(c, stack.peek()))
					postfix += stack.pop() + " ";
				stack.push(c);
			} else if (c == ')') {
				while (isOperator(stack.peek()))
					postfix += stack.pop() + " ";
				stack.pop();
			}
		}
		return postfix;
	}
	private static boolean isDigit(char digit) {
		for (char c : digits)
			if (digit == c)
				return true;
		return false;
	}
	private static boolean isVariable(char var) {
		return (97<=var && var<=122) || var=='_' || var=='"';
	}

	private static boolean isOperator(char operator) {
		for (char c : operators)
			if (operator == c)
				return true;
		return false;
	}

	private static boolean precedence(char op1, char op2) {
		return (op1 == '^' || ((op1 == '*' || op1 == '/' || op1 == '%') && (op2 == '+' || op2 == '-')));
	}
}

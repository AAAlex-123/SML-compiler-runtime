package postfix;

import java.util.Arrays;

public class PostfixEvaluator {
	private static final MyStack<Integer> stack = new MyStack<Integer>();

	private static final String[] keyWords = new String[] {"rem", "input", "if", "goto", "let", "print", "end"};
	private static final String[] operators = new String[] {"+", "-", "*", "/", "^", "%", "(", ")"};

	public static void main(String[] args) {
		String my_postfix = "10 2 2 5 1 * 3 - - * + ";
		String postfix = "6 2 + 5 * 8 4 / - ";
		System.out.println("my_postfix: " + evaluatePostfixMultiDigit(my_postfix));
		System.out.println("result: " + evaluatePostfixMultiDigit(postfix));

	}
	static void evaluatePostfix(String postfix) {
//		System.out.println(postfix);
		postfix += ")";
		int x, y, loc, index = 0;
		String[] tokens = postfix.split(" ");
		String c = tokens[index++];
		
		while (!c.equals(")")) {

			if (isConstant(c)) {
				loc = SML_Compiler.symbolTable.getSymbolLocation(c, 'C');
				if (loc == -1) {
					SML_Compiler.symbolTable.addEntry(c, 'C', SML_Compiler.dataCounter, "");
					SML_Compiler.SMLArray[SML_Compiler.dataCounter--] = Integer.parseInt(c);
					loc = SML_Compiler.dataCounter+1;
				}
				stack.push(loc);
			} else if (isVariable(c)) {
				loc = SML_Compiler.symbolTable.getSymbolLocation(c, 'V');
				if (loc == -1) {
					System.out.printf("%s:%02d: error: variable %s not found\n", SML_Compiler.inputFileName, SML_Compiler.line_count, c);
					SML_Compiler.succ = false;
				}
				stack.push(loc);
				
			} else if (isOperator(c)) {
				x = stack.pop();
				y = stack.pop();
				SML_Compiler.SMLArray[SML_Compiler.instructionCounter++] = SML_Executor.LOAD * 0x100 + y;
				
				SML_Compiler.SMLArray[SML_Compiler.instructionCounter] = x;
				if (c.equals("+")) SML_Compiler.SMLArray[SML_Compiler.instructionCounter] += SML_Executor.ADD * 0x100;
				else if (c.equals("-")) SML_Compiler.SMLArray[SML_Compiler.instructionCounter] += SML_Executor.SUBTRACT * 0x100;
				else if (c.equals("*")) SML_Compiler.SMLArray[SML_Compiler.instructionCounter] += SML_Executor.MULTIPLY * 0x100;
				else if (c.equals("/")) SML_Compiler.SMLArray[SML_Compiler.instructionCounter] += SML_Executor.DIVIDE * 0x100;
				else if (c.equals("%")) SML_Compiler.SMLArray[SML_Compiler.instructionCounter] += SML_Executor.MOD * 0x100;
				else if (c.equals("^")) SML_Compiler.SMLArray[SML_Compiler.instructionCounter] += SML_Executor.POW * 0x100;
				
				SML_Compiler.instructionCounter++;
				
				SML_Compiler.SMLArray[SML_Compiler.instructionCounter++] = SML_Executor.STORE * 0x100 + SML_Compiler.dataCounter;
				
				stack.push(SML_Compiler.dataCounter--);
			}
			c = tokens[index++];
		}
		SML_Compiler.SMLArray[SML_Compiler.instructionCounter++] = SML_Executor.LOAD * 0x100 + stack.pop();
	}

	static double evaluatePostfixMultiDigit(String postfix) {
		postfix += ")";
		int x, y, index = 0;
		String[] tokens = postfix.split(" ");
		String c = tokens[index++];

		while (!c.equals(")")) {
			
			if (c.matches("-?\\d+")) {
				stack.push(Integer.parseInt(c));

			} else if (isOperator(c)){
				x = stack.pop();
				y = stack.pop();
				stack.push(calculate(x, y, c));
			}
			c = tokens[index++];
		}
		return stack.pop();
	}
	private static int calculate(int x, int y, String op) {
		if (op.equals("+")) return y+x;
		else if (op.equals("-")) return y-x;
		else if (op.equals("*")) return y*x;
		else if (op.equals("/")) return y/x;
		else if (op.equals("%")) return y%x;
		else if (op.equals("^")) return (int) Math.pow(y, x);
		else return -1;
	}

	private static boolean isVariable(String var) {
		try {
			Integer.parseInt(var);
			return false;
		} catch (NumberFormatException e) {
			return !Arrays.asList(keyWords).contains(var) && !Arrays.asList(operators).contains(var);
		}
	}
	private static boolean isConstant(String con) {
		if (con.startsWith("\"") && con.endsWith("\"")) return true;
		try {
			Integer.parseInt(con);
			return !Arrays.asList(keyWords).contains(con);
		} catch (NumberFormatException e) {return false;}
	}

	private static boolean isOperator(String operator) {
		for (String c : operators)
			if (operator.equals(c))
				return true;
		return false;
	}
}

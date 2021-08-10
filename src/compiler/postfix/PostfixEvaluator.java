package compiler.postfix;

import static compiler.symboltable.SymbolType.CONSTANT;
import static compiler.symboltable.SymbolType.VARIABLE;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import compiler.SML_Compiler;
import compiler.symboltable.SymbolTable;
import runtime.Instruction;

/**
 * Defines the static method
 * {@link PostfixEvaluator#evaluatePostfix(List, SymbolTable)
 * evaluatePostfix(List, SymbolTable)} which returns the machine language
 * {@link runtime.Instruction instructions} that evaluate the postfix
 * expression. A {@link SymbolTable} is used to access and allocate the correct
 * addresses during the evaluation.
 *
 * @author Alex Mandelias
 */
public final class PostfixEvaluator {

	/* Don't let anyone instantiate this class */
	private PostfixEvaluator() {}

	/**
	 * Generates a list of machine-language {@code Instructions} that, when run,
	 * evaluate the {@code postfix} expression. The {@code SymbolTable} is used in
	 * order to get the correct addresses for variables and constants and to
	 * allocate space for storing the temporary results of the evaluation.
	 * <p>
	 * The evaluation assumes that all constants and variables found in the
	 * expression are already declared, and that all variable names are valid.
	 * <p>
	 * TODO: error checking
	 *
	 * @param postfix     the valid postfix expression whose symbols are all defined
	 * @param symbolTable the SymbolTable containing information about symbols of
	 *                    the postfix expression, which will be used to store the
	 *                    addresses of newly allocated variables
	 *
	 * @return the list of instructions
	 */
	public static List<Integer> evaluatePostfix(List<Token> postfix, SymbolTable symbolTable) {

		final List<Integer>  instructionList = new ArrayList<>();
		final Stack<Integer> stack           = new Stack<>();

		for (final Token token : postfix) {
			if (PostfixEvaluator.isConstant(token)) {
				final int location = symbolTable.getSymbol(token.value, CONSTANT).location;
				stack.push(location);
			} else if (PostfixEvaluator.isVariable(token)) {
				final int location = symbolTable.getSymbol(token.value, VARIABLE).location;
				stack.push(location);
			} else if (token.isOperator()) {
				final int ylocation = stack.pop();
				final int xlocation = stack.pop();

				final Instruction instruction = PostfixEvaluator.getInstructionFromToken(token);

				final int resultLocation = SML_Compiler.addVariable();

				instructionList.add(Instruction.LOAD.opcode() + xlocation);
				instructionList.add(instruction.opcode() + ylocation);
				instructionList.add(Instruction.STORE.opcode() + resultLocation);

				stack.push(resultLocation);
			}
		}

		instructionList.add(Instruction.LOAD.opcode() + stack.pop());
		return instructionList;
	}

	private static boolean isConstant(Token token) {
		return PostfixEvaluator.isNumber(token);
	}

	private static boolean isVariable(Token token) {
		return !PostfixEvaluator.isConstant(token) && !token.isOperator();
	}

	private static boolean isNumber(Token token) {
		try {
			Integer.parseInt(token.value);
			return true;
		} catch (final NumberFormatException e) {
			return false;
		}
	}

	private static Instruction getInstructionFromToken(Token token) {
		// Token is class, not enum, can't use switch
		if (token == Token.ADD)
			return Instruction.ADD;
		if (token == Token.SUB)
			return Instruction.SUBTRACT;
		if (token == Token.MUL)
			return Instruction.MULTIPLY;
		if (token == Token.DIV)
			return Instruction.DIVIDE;
		if (token == Token.MOD)
			return Instruction.MOD;
		if (token == Token.POW)
			return Instruction.POW;
		return null;
	}
}

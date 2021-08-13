package compiler.blocks;

/**
 * Block for code in a {@code while} statement.
 *
 * @author Alex Mandelias
 */
public class WhileBlock extends Block {

	/**
	 * The location of the first instruction of the while loop in memory, the
	 * instruction that starts the calculation of the condition.
	 */
	public int locationOfFirstInstruction;

	@Override
	public String toString() {
		return "While Block";
	}
}

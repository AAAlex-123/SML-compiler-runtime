package compiler.blocks;

/**
 * A collection of variables that contain the information necessary to produce
 * machine code for a block of code that contains jump instructions. These
 * variables typically store the jump location of a branch instruction. When a
 * Block of code is closed (it is popped of the stack) information about jump
 * locations is retrieved from the variables (by casting it to the correct Block
 * type) and is used to fill incomplete branch instructions. Blocks represent,
 * but not limited to, code within {@code if}, {@code else} and {@code while}
 * statements.
 * <p>
 * <b>Note:</b> this compiler was never meant to handle {@code if} and
 * {@code while} loops that require code to be treated as a block that is
 * repeated (as opposed to purely procedural code). This feature was the last to
 * be added and is therefore poorly implemented on top of the rest of the
 * compiler.
 *
 * @author Alex Mandelias
 */
public abstract class Block {

	/**
	 * The location of the instruction in memory that is responsible for jumping to
	 * the end of the block.
	 */
	public int locationOfBranchToEndOfBlock;

	@Override
	public abstract String toString();
}

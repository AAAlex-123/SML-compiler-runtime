package compiler.exceptions;

import compiler.blocks.Block;

/**
 * Thrown when a {@link compiler.blocks.Block Block} is not closed when the end
 * of the program is reached.
 *
 * @author Alex Mandelias
 */
public class UnclosedBlockException extends CompilerException {

	/**
	 * Constructs the exception with information about the unclosed {@code Block}.
	 *
	 * @param block the block that was not closed
	 */
	public UnclosedBlockException(Block block) {
		super("Unclosed %s", block);
	}
}

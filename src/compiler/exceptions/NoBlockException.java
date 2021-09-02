package compiler.exceptions;

import compiler.blocks.Block;

/**
 * Thrown when a {@link compiler.blocks.Block Block} of a specific type was
 * expected but not found.
 *
 * @author Alex Mandelias
 */
public class NoBlockException extends CompilerException {

	/**
	 * Constructs the exception with information about the unclosed {@code Block}.
	 *
	 * @param block the block that was not closed
	 */
	public NoBlockException(Block block) {
		super("No %s found", block);
	}
}

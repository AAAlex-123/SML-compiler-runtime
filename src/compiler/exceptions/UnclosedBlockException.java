package compiler.exceptions;

import compiler.blocks.Block;

/**
 * Thrown when a {@code Block} is not closed when compilation ends.
 *
 * @author Alex Mandelias
 */
public class UnclosedBlockException extends CompilerException {

	/**
	 * Constructs the exception with a {@code Block}.
	 *
	 * @param block the Block that was not closed
	 */
	public UnclosedBlockException(Block block) {
		super("Unclosed %s", block);
	}
}

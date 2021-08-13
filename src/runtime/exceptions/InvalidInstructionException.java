package runtime.exceptions;

/**
 * Thrown when an machine-code instruction was read but its operation code
 * doesn't match any {@link runtime.Instruction Instruction}.
 *
 * @author Alex Mandelias
 */
public class InvalidInstructionException extends ExecutionException {

	/**
	 * Constructs the Exception with information about the {@code operation code}.
	 *
	 * @param opcode the operation code for which there is no {@code Instruction}
	 */
	public InvalidInstructionException(int opcode) {
		super("Invalid Instruction operation code: %02x", opcode);
	}
}

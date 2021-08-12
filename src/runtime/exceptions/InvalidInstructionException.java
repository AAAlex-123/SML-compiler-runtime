package runtime.exceptions;

public class InvalidInstructionException extends ExecutionException {

	public InvalidInstructionException(int opcode) {
		super("Invalid Instruction opcode: %02x", opcode);
	}
}

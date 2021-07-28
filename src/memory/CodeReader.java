package memory;

public interface CodeReader extends RAM {
	public int fetchInstruction();

	public void initialiseForExecution();

	public int getInstructionPointer();

	public void setInstructionPointer(int address);
}

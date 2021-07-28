package memory;

public interface CodeWriter extends RAM {
	public int writeInstruction(int value);

	public int writeConstant(int value);

	public int assignPlaceForVariable();

	public void initializeForWriting();

	public int getInstructionCounter();

	public String list();
}

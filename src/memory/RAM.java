package memory;

public interface RAM {
	public int size();

	public void write(int address, int value);

	public void writeChars(int address, char[] values);

	public int read(int address);

	public char[] readChars(int address);

	public void clear();
}

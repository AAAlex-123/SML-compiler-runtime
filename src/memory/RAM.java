package memory;

public interface RAM {
	int size();

	void write(int address, int value);

	void writeChars(int address, char[] values);

	int read(int address);

	char[] readChars(int address);

	void clear();

	static String sf(String text, Object... args) {
		return String.format(text, args);
	}
}

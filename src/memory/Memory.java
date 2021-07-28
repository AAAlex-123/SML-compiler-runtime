package memory;

import java.util.Arrays;

public class Memory implements CodeReader, CodeWriter {

	private final int[] data;
	private final int   size;

	public Memory(int size) {
		data = new int[size];
		this.size = size;
	}

	@Override
	public void clear() {
		Arrays.fill(data, 0);
	}

	@Override
	public int size() {
		return size;
	}

	@Override
	public void write(int address, int value) {
		data[address] = value;
	}

	@Override
	public void writeChars(int address, char[] values) {
		// write length of array to high bits to know how much to read
		write(address, values.length * 0x100);

		int offset = address;

		for (int i = 0; i < values.length; ++i) {
			// write each byte alternating between high and low bits
			if ((i % 2) == 1) {
				write(offset, values[i] * 0x100);
			} else {
				int previous = read(offset);
				write(offset, previous + values[i]);
				++offset;
			}
		}
	}

	@Override
	public int read(int address) {
		return data[address];
	}

	@Override
	public char[] readChars(int address) {
		// read length of array from high bits to know how much to read
		int length = read(address) / 0x100;

		int    offset = address;
		char[] array  = new char[length];

		for (int i = 0; i < array.length; ++i) {
			// read each byte alternating between high and low bits
			if ((i % 2) == 0)
				array[i] = (char) (read(offset++) % 0x100);
			else
				array[i] = (char) (read(offset) / 0x100);
		}
		return array;
	}

	// ===== CODE READER =====

	private int instructionPointer;

	@Override
	public int fetchInstruction() {
		return read(instructionPointer++);
	}

	@Override
	public void initialiseForExecution() {
		instructionPointer = 0;
	}

	@Override
	public int getInstructionPointer() {
		return instructionPointer;
	}

	@Override
	public void setInstructionPointer(int address) {
		instructionPointer = address;
	}

	// ===== CODE WRITER =====

	private int instructionCounter, dataCounter;

	@Override
	public int writeInstruction(int value) {
		int writeLocation = instructionCounter;
		write(instructionCounter, value);
		++instructionCounter;
		return writeLocation;
	}

	@Override
	public int writeConstant(int value) {
		int writeLocation = dataCounter;
		write(dataCounter, value);
		--dataCounter;
		return writeLocation;
	}

	@Override
	public int assignPlaceForVariable() {
		int writeLocation = dataCounter;
		--dataCounter;
		return writeLocation;
	}

	@Override
	public void initializeForWriting() {
		instructionCounter = 0;
		dataCounter = size - 1;
	}

	@Override
	public int getInstructionCounter() {
		return instructionCounter;
	}

	@Override
	public String list() {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < size; ++i)
			sb.append(String.format("%04x%n", read(i)));

		return sb.toString();
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();

		// TODO: fix hard-coded size 16
		for (int i = 0; i < 16; i++)
			sb.append(sf("     %x", i));
		sb.append("\n");

		for (int i = 0; i < 16; i++) {
			sb.append(sf("%x0", i));
			for (int j = 0; j < 16; j++) {
				int val = read((16 * i) + j);
				sb.append(" ")
				        .append(val < 0 ? "-" : "+")
				        .append(sf("%04x", val));
			}

			sb.append("\n");
		}

		return sb.toString();
	}

	private static String sf(String text, Object... args) {
		return String.format(text, args);
	}
}

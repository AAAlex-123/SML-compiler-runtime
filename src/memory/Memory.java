package memory;

import java.util.Arrays;

/**
 * An implementation of the RAM, CodeReader, CodeWriter interfaces.
 * <p>
 * TODO: some overflow/underflow maybe, or just mod(0x10000) each value written
 *
 * @author Alex Mandelias
 */
public class Memory implements CodeReader, CodeWriter {

	static {
		// System.out.println("calling System.loadLibary() 8D");
		// System.loadLibrary("Main");
	}

	private final int[] data;
	private final int   size;

	/**
	 * Constructs the Memory with the given {@code size}.
	 *
	 * @param size the number of cells
	 */
	public Memory(int size) {
		data = new int[size];
		this.size = size;
	}

	@Override
	public int size() {
		return size;
	}

	@Override
	public void write(int address, int value) {
		data[address] = value;
	}

	/**
	 * @implSpec The number of individual values written is saved in the higher bits
	 *           of the cell at {@code address}. The actual values are 16-bits
	 *           integers, and are stored in pairs in consecutive addresses. The
	 *           {@code odd-indexed} values are written in the stored in the high
	 *           bits and the {@code even-indexed} values in the low bits of the
	 *           cell, starting at the given {@code address}.
	 */
	@Override
	public void writeChars(int address, char[] values) {
		// write length of array to high bits to know how much to read
		write(address, values.length * 0x100);

		int offset = address;

		for (int i = 0, count = values.length; i < count; ++i)
			// write each byte alternating between high and low bits
			if ((i % 2) == 1)
				write(offset, values[i] * 0x100);
			else {
				final int previous = read(offset);
				write(offset, previous + values[i]);
				++offset;
			}
	}

	@Override
	public int read(int address) {
		return data[address];
	}

	/**
	 * @implSpec complies with {@link memory.Memory#writeChars(int, char[])
	 *           writeChars(int, char[])}.
	 */
	@Override
	public char[] readChars(int address) {
		// read length of array from high bits to know how much to read
		final int length = read(address) / 0x100;

		int          offset = address;
		final char[] array  = new char[length];

		for (int i = 0; i < array.length; ++i)
			// read each byte alternating between high and low bits
			if ((i % 2) == 0) {
				array[i] = (char) (read(offset) % 0x100);
				offset++;
			} else
				array[i] = (char) (read(offset) / 0x100);
		return array;
	}

	@Override
	public void clear() {
		Arrays.fill(data, 0);
	}

	@Override
	public int getDumpSize() {
		return 16;
		// return getDumpSizeNative();
	}

	// TODO: implement
	private native int getDumpSizeNative();

	// ===== CODE READER =====

	private int instructionPointer;

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
	public int getInstructionCounter() {
		return instructionCounter;
	}

	@Override
	public void setInstructionCounter(int address) {
		instructionCounter = address;
	}

	@Override
	public int getDataCounter() {
		return dataCounter;
	}

	@Override
	public void setDataCounter(int address) {
		dataCounter = address;
	}

	// ===== TO STRING =====

	@Override
	public String toString() {
		return dump();
	}
}

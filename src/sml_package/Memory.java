package sml_package;

import java.util.Arrays;

public class Memory {

	private final int[] array;
	private int         instructionCounter;

	public Memory(int size) {
		array = new int[size];
	}

	public void clear() {
		Arrays.fill(array, 0);
	}

	public void write(int address, int value) {
		array[address] = value;
	}

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

	public int read(int address) {
		return array[address];
	}

	public char[] readChars(int address) {
		// read length of array from high bits to know how much to read
		int length = read(address) / 0x100;

		int offset = address;
		char[] arr = new char[length];

		for (int i = 0; i < arr.length; ++i) {
			// read each byte alternating between high and low bits
			if ((i % 2) == 0)
				arr[i] = (char) (read(offset++) % 0x100);
			else
				arr[i] = (char) (read(offset) / 0x100);
		}
		return arr;
	}

	public void initialiseForExecution() {
		instructionCounter = 0;
	}

	public int fetchInstruction() {
		return read(instructionCounter++);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
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

package memory;

/**
 * Extension of the RAM interface providing convenience methods for writing
 * machine code to RAM.
 *
 * @implNote The default implementations assume that there exist an instruction
 *           counter and a data counter responsible for writing instructions and
 *           data (variables, constants etc.). Specifically:
 *           <p>
 *           {@code instructions} and the Instruction Counter:
 *           <ul>
 *           <li>are written as 32-bit values</li>
 *           <li>are written in consecutive cells with increasing index</li>
 *           <li>start at location {@code 0}</li>
 *           </ul>
 *           {@code data} and the Data Counter:
 *           <ul>
 *           <li>are written as 32-bit values</li>
 *           <li>are written in consecutive cells with decreasing index</li>
 *           <li>start at location {@code size() - 1}</li>
 *           </ul>
 *
 * @author Alex Mandelias
 */
public interface CodeWriter extends RAM {

	/**
	 * Writes the {@code value} as an instruction in RAM.
	 *
	 * @param value the instruction
	 *
	 * @return the address where the instruction was written
	 *
	 * @see memory.CodeWriter default implementation
	 */
	default int writeInstruction(int value) {
		final int address = getInstructionCounter();
		write(address, value);
		setInstructionCounter(address + 1);
		return address;
	}

	/**
	 * Writes the {@code constant} in RAM.
	 *
	 * @param value the constant
	 *
	 * @return the address where the constant was written
	 *
	 * @see memory.CodeWriter default implementation
	 */
	default int writeConstant(int value) {
		final int address = getDataCounter();
		write(address, value);
		setDataCounter(address - 1);
		return address;
	}

	/**
	 * Allocates place for a new variable in RAM.
	 *
	 * @return the address of the allocated cell in memory
	 *
	 * @see memory.CodeWriter default implementation
	 */
	default int assignPlaceForVariable() {
		final int address = getDataCounter();
		setDataCounter(address - 1);
		return address;
	}

	/**
	 * Initialises the RAM for writing code to memory.
	 *
	 * @see memory.CodeWriter default implementation
	 */
	default void initializeForWriting() {
		setInstructionCounter(0);
		setDataCounter(size() - 1);
	}

	/**
	 * Returns the address of the first empty place to write an instruction.
	 *
	 * @return the address
	 */
	int getInstructionCounter();

	/**
	 * Sets the address where the next instruction will be written.
	 *
	 * @param address the address
	 */
	void setInstructionCounter(int address);

	/**
	 * Returns the address of the first empty place to write data.
	 *
	 * @return the address
	 */
	int getDataCounter();

	/**
	 * Sets the address where the next data will be written.
	 *
	 * @param address the address
	 */
	void setDataCounter(int address);

	/**
	 * Returns a String with the listing of the RAM. The contents of the cells are
	 * written as-is, therefore this method can be used for listing code suitable to
	 * be read for execution.
	 *
	 * @return the String
	 */
	default String list() {
		final StringBuilder sb = new StringBuilder(size() * 6);
		for (int i = 0, size = size(); i < size; ++i)
			sb.append(String.format("%04x%n", read(i)));

		return sb.toString();
	}

	/**
	 * Returns a String with a short listing of the RAM. Consecutive cells
	 * containing '0' are all replaced with a single line containing '** ****',
	 * therefore this method can't be used for listing code suitable for execution.
	 *
	 * @return the String
	 */
	default String listShort() {
		final StringBuilder sb      = new StringBuilder();
		final String        lineSep = System.lineSeparator();

		boolean zeros = false;
		for (int i = 0, size = size(); i < size; ++i) {

			final int val = read(i);

			// replace any number of consecutive zeros with '** ****'
			if ((val == 0) && !zeros) {
				zeros = true;
				sb.append("** ****").append(lineSep);
			} else if (val != 0)
				zeros = false;

			if (!zeros)
				sb.append(String.format("%02x %04x%n", i, val));
		}

		return sb.toString();
	}
}

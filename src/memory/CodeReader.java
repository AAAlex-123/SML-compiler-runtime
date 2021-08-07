package memory;

/**
 * Extension of the RAM interface providing convenience methods for reading
 * machine code from RAM.
 *
 * @implNote The default implementations assume that there exists an instruction
 *           pointer responsible for reading successive instructions from RAM.
 *           Specifically:
 *           <p>
 *           {@code instructions} and the Instruction Pointer:
 *           <ul>
 *           <li>are read as 32-bit values</li>
 *           <li>are read from consecutive cells with increasing index</li>
 *           <li>start at location {@code 0}</li>
 *           </ul>
 *
 * @author Alex Mandelias
 */
public interface CodeReader extends RAM {

	/**
	 * Returns the next instruction written in the RAM. Successive calls to this
	 * method should return successive instructions, independent of their size
	 * and/or location.
	 *
	 * @see memory.CodeReader default implementation
	 *
	 * @return the next instruction
	 */
	default int fetchInstruction() {
		final int address     = getInstructionPointer();
		final int instruction = read(address);
		setInstructionPointer(address + 1);
		return instruction;
	}

	/**
	 * Initialises the RAM for executing the code already loaded
	 *
	 * @see memory.CodeReader default implementation
	 */
	default void initialiseForExecution() {
		setInstructionPointer(0);
	}

	/**
	 * Returns the value of the instruction pointer, the address of the next
	 * instruction that will be executed.
	 *
	 * @return the value of the instruction pointer
	 */
	int getInstructionPointer();

	/**
	 * Moves the instruction pointer to the new {@code address}, changing the next
	 * instruction that will be executed.
	 *
	 * @param address the address of the next instruction to be executed
	 */
	void setInstructionPointer(int address);

	/**
	 * Returns the number {@code a} for which exists number {@code b} such that:
	 *
	 * <pre>
	 *  -  a > b
	 *  -  a * b = size()
	 *  -  |a - b| is minimum among all {(a, b) : a * b = size()}
	 * </pre>
	 *
	 * @return the number a
	 */
	int getDumpSize();

	/**
	 * Returns a String with a memory dump in the following format:
	 *
	 * <pre>
	 *	0	1	2	...
	 *00	a	b	c	...
	 *10	d	e	f	...
	 *20	g	h	i	...
	 *...	...	...	...	...
	 * </pre>
	 *
	 * where {@code a}, {@code b}, {@code c}, ... are the values written in the
	 * cells at the address formed by adding the {@code x} and {@code y} coordinate.
	 * <p>
	 * <b>Note:</b> all numbers are displayed in hex.
	 *
	 * @return the String with the memory dump
	 */
	default String dump() {
		final StringBuilder sb      = new StringBuilder();
		final String        lineSep = System.lineSeparator();

		final int x = getDumpSize();
		final int y = size() / x;

		// first row
		sb.append("  ");
		for (int j = 0; j < y; ++j)
			sb.append(String.format("     %x", j));
		sb.append(lineSep);

		// i = row
		for (int i = 0; i < x; ++i) {
			// write first column of the row
			sb.append(String.format("%x0", i));

			// j = column
			// write values from each column of the row
			for (int j = 0; j < y; ++j) {

				final int val = read((16 * i) + j);
				sb.append(" ")
				        .append(val < 0 ? "-" : "+")
				        .append(String.format("%04x", val));
			}

			// end of row
			sb.append(lineSep);
		}

		return sb.toString();
	}
}

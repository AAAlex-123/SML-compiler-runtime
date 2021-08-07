package memory;

/**
 * Interface for a Random Access Memory object which is, in essence, responsible
 * for storing and retrieving values. Since the methods have {@code int}
 * arguments and return values, each cell holds, at most 32-bit values. However,
 * it is possible to store only a 16-bit value per cell, or even two 16-bit
 * values per cell, one in the low and one in the high bits of each cell.
 * <p>
 * <b>Note:</b> this interface is rather limited and poorly designed as the need
 * for it became apparent at the later stages of development and was therefore
 * tailored to the specific needs of this application. It may or may not be
 * improved in the future.
 *
 * @author Alex Mandelias
 */
public interface RAM {

	/**
	 * Returns the size of the RAM, the number of 32-bit values it can store.
	 *
	 * @return the size
	 */
	int size();

	/**
	 * Writes the {@code value} in the {@code address}.
	 *
	 * @param address the address in RAM
	 * @param value   the value to write in the address
	 */
	void write(int address, int value);

	/**
	 * Writes an array of {@code char values} starting at {@code address}.
	 *
	 * @implNote implementations must ensure that all the metadata surrounding the
	 *           {@code values} (e.g. size) is saved when writing and that it can be
	 *           properly retrieved when reading. The protocol used in this method
	 *           must be consistent with the one used in the
	 *           {@link RAM#readChars(int) readChars(int)} method.
	 *
	 * @param address the address of the first value
	 * @param values  the array of values to write
	 */
	void writeChars(int address, char[] values);

	/**
	 * Reads a {@code value} from RAM.
	 *
	 * @param address the address in RAM to read from
	 *
	 * @return the value in that address
	 */
	int read(int address);

	/**
	 * Reads an array of {@code values} from RAM.
	 *
	 * @implNote implementations must ensure that the correct values are read
	 *           according to the metadata stored by the
	 *           {@link RAM#writeChars(int, char[]) writeChars(int, char[])} method.
	 *
	 * @param address the address of the first value
	 *
	 * @return the array of the values read
	 */
	char[] readChars(int address);

	/** Clears the RAM, resetting every value to 0 */
	default void clear() {
		for (int i = 0, size = size(); i < size; ++i)
			write(i, 0);
	}
}

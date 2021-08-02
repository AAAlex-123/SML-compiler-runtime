package memory;

public interface CodeWriter extends RAM {
	int writeInstruction(int value);

	int writeConstant(int value);

	int assignPlaceForVariable();

	void initializeForWriting();

	int getInstructionCounter();

	default String list() {
		final StringBuilder sb = new StringBuilder(size() * 6);
		for (int i = 0; i < size(); ++i)
			sb.append(String.format("%04x%n", read(i)));

		return sb.toString();
	}

	default String listShort() {
		final StringBuilder sb = new StringBuilder();

		boolean zeros = false;
		for (int i = 0, size = size(); i < size; i++) {

			final int val = read(i);

			// replace any number of consecutive zeros with '** ****'
			if ((val == 0) && !zeros) {
				zeros = true;
				sb.append(RAM.sf("** ****"));
			} else if (val != 0)
				zeros = false;

			if (!zeros)
				sb.append(RAM.sf("%02x %04x\n", i, val));
		}

		return sb.toString();
	}
}

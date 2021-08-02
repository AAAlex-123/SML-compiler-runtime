package memory;

public interface CodeReader extends RAM {
	int fetchInstruction();

	void initialiseForExecution();

	int getInstructionPointer();

	void setInstructionPointer(int address);

	default String dump() {
		final StringBuilder sb = new StringBuilder();

		// TODO: fix hard-coded size 16
		for (int i = 0; i < 16; i++)
			sb.append(RAM.sf("     %x", i));
		sb.append("\n");

		for (int i = 0; i < 16; i++) {
			sb.append(RAM.sf("%x0", i));
			for (int j = 0; j < 16; j++) {
				final int val = read((16 * i) + j);
				sb.append(" ")
				        .append(val < 0 ? "-" : "+")
				        .append(RAM.sf("%04x", val));
			}

			sb.append("\n");
		}

		return sb.toString();
	}
}

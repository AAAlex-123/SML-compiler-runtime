package sml_package;

import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public enum Command {

	READ_INT(0x10) {
		@Override
		public void execute() {
			@SuppressWarnings("resource")
			Scanner scanner = new Scanner(System.in);

			boolean valid = false;
			int     input = 0;

			while (!valid) {
				SML_Executor.out("> ");
				String userInput = scanner.nextLine();
				try {
					input = Integer.parseInt(userInput, 16);
					valid = (-0xffff <= input) && (input <= 0xffff);
					if (!valid)
						SML_Executor.err("%s is out of range (-0xffff to 0xffff).%n", userInput);
				} catch (NumberFormatException exc) {
					valid = false;
					SML_Executor.err("%s is not a valid int.%n", userInput);
				}
			}
			SML_Executor.memory.write(operand, input);
		}
	},

	READ_STRING(0x11) {
		@Override
		public void execute() {
			@SuppressWarnings("resource")
			Scanner scanner = new Scanner(System.in);

			SML_Executor.out("> ");
			String input = scanner.nextLine();
			char[] array = input.toCharArray();
			SML_Executor.memory.writeChars(operand, array);
		}
	},

	WRITE(0x12) {
		@Override
		public void execute() {
			SML_Executor.out("SML: %04x", SML_Executor.memory.read(operand));
		}
	},

	WRITE_NL(0x13) {
		@Override
		public void execute() {
			SML_Executor.out("SML: %04x\n", SML_Executor.memory.read(operand));
		}
	},

	WRITE_STRING(0x14) {
		@Override
		public void execute() {
			char[] chars = SML_Executor.memory.readChars(operand);

			for (char c : chars)
				System.out.print(c);
		}
	},

	WRITE_STRING_NL(0x15) {
		@Override
		public void execute() {
			char[] chars = SML_Executor.memory.readChars(operand);

			for (char c : chars)
				System.out.print(c);
			System.out.println();
		}
	},

	LOAD(0x20) {
		@Override
		public void execute() {
			SML_Executor.accumulator = SML_Executor.memory.read(operand);
		}
	},

	STORE(0x21) {
		@Override
		public void execute() {
			SML_Executor.memory.write(operand, SML_Executor.accumulator);
		}
	},
	ADD(0x30) {
		@Override
		public void execute() {
			int sum = SML_Executor.accumulator + SML_Executor.memory.read(operand);
			if ((sum < -0xffff) || (sum > 0xffff)) {
				System.out.println("*** Overflow Error\t\t ***");
				System.out.println("\n*** Program execution abnormally terminated ***");
				HALT.execute();
			}
			SML_Executor.accumulator = sum;
		}
	},
	SUBTRACT(0x31) {
		@Override
		public void execute() {
			int difference = SML_Executor.accumulator - SML_Executor.memory.read(operand);
			if ((difference < -0xffff) || (difference > 0xffff)) {
				System.out.println("*** Overflow Error\t\t ***");
				System.out.println("\n*** Program execution abnormally terminated ***");
				HALT.execute();
			}
			SML_Executor.accumulator = difference;
		}
	},
	DIVIDE(0x32) {
		@Override
		public void execute() {
			int divisor = SML_Executor.memory.read(operand);
			if (divisor == 0) {
				System.out.println("*** Attempt to divide by zero\t\t ***");
				System.out.println("\n*** Program execution abnormally terminated ***");
				HALT.execute();
			}

			int quotient = SML_Executor.accumulator / divisor;
			if ((quotient < -0xffff) || (quotient > 0xffff)) {
				System.out.println("*** Overflow Error\t\t ***");
				System.out.println("\n*** Program execution abnormally terminated ***");
				HALT.execute();
			}
			SML_Executor.accumulator = quotient;
		}
	},
	MULTIPLY(0x33) {
		@Override
		public void execute() {
			int product = SML_Executor.accumulator * SML_Executor.memory.read(operand);
			if ((product < -0xffff) || (product > 0xffff)) {
				System.out.println("*** Overflow Error\t\t ***");
				System.out.println("\n*** Program execution abnormally terminated ***");
				HALT.execute();
			}
			SML_Executor.accumulator = product;
		}
	},
	MOD(0x34) {
		@Override
		public void execute() {
			int divisor = SML_Executor.memory.read(operand);
			if (divisor == 0) {
				System.out.println("*** Attempt to divide by zero\t\t ***");
				System.out.println("\n*** Program execution abnormally terminated ***");
				HALT.execute();
			}

			int remainder = SML_Executor.accumulator % divisor;
			SML_Executor.accumulator = remainder;
		}
	},
	POW(0x35) {
		@Override
		public void execute() {
			double power = Math.pow(SML_Executor.accumulator, SML_Executor.memory.read(operand));
			if ((power < -0xffff) || (power > 0xffff)) {
				System.out.println("*** Overflow Error\t\t ***");
				System.out.println("\n*** Program execution abnormally terminated ***");
				HALT.execute();
			}
			SML_Executor.accumulator = (int) power;
		}
	},
	BRANCH(0x40) {
		@Override
		public void execute() {
			SML_Executor.memory.setInstructionPointer(operand);
		}
	},
	BRANCHNEG(0x41) {
		@Override
		public void execute() {
			if (SML_Executor.accumulator < 0)
				SML_Executor.memory.setInstructionPointer(operand);
		}
	},
	BRANCHZERO(0x42) {
		@Override
		public void execute() {
			if (SML_Executor.accumulator == 0)
				SML_Executor.memory.setInstructionPointer(operand);
		}
	},
	HALT(0x43) {
		@Override
		public void execute() {
			SML_Executor.halt();
			System.out.println("*** Program execution terminated\t ***");
		}
	},
	DUMP(0xf0) {
		@Override
		public void execute() {
			SML_Executor.out("%s", SML_Executor.memory);
		}
	},
	NOOP(0xf1) {
		@Override
		public void execute() {
			;
		}
	};

	private final int operationCode;
	protected int     operand;

	private static final Map<Integer, Command> opcodeMap;

	private Command(int operationCode) {
		this.operationCode = operationCode;
	}

	public int opcode() {
		return operationCode * 0x100;
	}

	public abstract void execute();

	public static Command from(int operationCode, int operand) {
		Command command = opcodeMap.get(operationCode);
		if (command == null) {
			System.out.printf("*** Invalid Operation Code: %02x\t ***\n", operationCode);
			System.out.println("\n*** Program execution abnormally terminated ***");
			HALT.execute();
			return null;
		}
		command.operand = operand;
		return command;
	}

	static {
		opcodeMap = new HashMap<>();
		for (Command command : Command.values())
			opcodeMap.put(command.operationCode, command);
	}
}

package sml_package;

import java.util.Scanner;

public enum Command {

	READ_INT {
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

	READ_STRING {
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

	WRITE {
		@Override
		public void execute() {
			SML_Executor.out("SML: %04x", SML_Executor.memory.read(operand));
		}
	},

	WRITE_NL {
		@Override
		public void execute() {
			SML_Executor.out("SML: %04x\n", SML_Executor.memory.read(operand));
		}
	},

	WRITE_STRING {
		@Override
		public void execute() {
			char[] chars = SML_Executor.memory.readChars(operand);

			for (char c : chars)
				System.out.print(c);
		}
	},

	WRITE_STRING_NL {
		@Override
		public void execute() {
			char[] chars = SML_Executor.memory.readChars(operand);

			for (char c : chars)
				System.out.print(c);
			System.out.println();
		}
	},

	LOAD {
		@Override
		public void execute() {
			SML_Executor.accumulator = SML_Executor.memory.read(operand);
		}
	},

	STORE {
		@Override
		public void execute() {
			SML_Executor.memory.write(operand, SML_Executor.accumulator);
		}
	},
	ADD {
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
	SUBTRACT {
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
	DIVIDE {
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
	MULTIPLY {
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
	MOD {
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
	POW {
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
	BRANCH {
		@Override
		public void execute() {
			SML_Executor.instructionCounter = operand;
		}
	},
	BRANCHNEG {
		@Override
		public void execute() {
			if (SML_Executor.accumulator < 0)
				SML_Executor.instructionCounter = operand;
		}
	},
	BRANCHZERO {
		@Override
		public void execute() {
			if (SML_Executor.accumulator == 0)
				SML_Executor.instructionCounter = operand;
		}
	},
	HALT {
		@Override
		public void execute() {
			SML_Executor.halt = true;
			System.out.println("*** Program execution terminated\t ***");
		}
	},
	DUMP {
		@Override
		public void execute() {
			SML_Executor.out("%s", SML_Executor.memory);
		}
	},
	NOOP {
		@Override
		public void execute() {
			;
		}
	};

	protected int operand;

	public static Command from(int operationCode, int operand) {
		Command command = from0(operationCode);
		if (command == null) {
			System.out.printf("*** Invalid Operation Code: %02x\t ***\n", operationCode);
			System.out.println("\n*** Program execution abnormally terminated ***");
			HALT.execute();
			return null;
		}
		command.operand = operand;
		return command;
	}

	private static Command from0(int operationCode) {
		switch (operationCode) {
		case SML_Executor.READ_INT:
			return READ_INT;
		case SML_Executor.READ_STRING:
			return READ_STRING;
		case SML_Executor.WRITE:
			return WRITE;
		case SML_Executor.WRITE_NL:
			return WRITE_NL;
		case SML_Executor.WRITE_STRING:
			return WRITE_STRING;
		case SML_Executor.WRITE_STRING_NL:
			return WRITE_STRING_NL;
		case SML_Executor.LOAD:
			return LOAD;
		case SML_Executor.STORE:
			return STORE;
		case SML_Executor.ADD:
			return ADD;
		case SML_Executor.SUBTRACT:
			return SUBTRACT;
		case SML_Executor.DIVIDE:
			return DIVIDE;
		case SML_Executor.MULTIPLY:
			return MULTIPLY;
		case SML_Executor.MOD:
			return MOD;
		case SML_Executor.POW:
			return POW;
		case SML_Executor.BRANCH:
			return BRANCH;
		case SML_Executor.BRANCHNEG:
			return BRANCHNEG;
		case SML_Executor.BRANCHZERO:
			return BRANCHZERO;
		case SML_Executor.HALT:
			return HALT;
		case SML_Executor.DUMP:
			return DUMP;
		case SML_Executor.NOOP:
			return NOOP;
		default:
			return null;
		}
	}

	public abstract void execute();
}

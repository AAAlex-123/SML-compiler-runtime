package runtime;

import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import runtime.exceptions.InvalidInstructionException;

/**
 * An Instruction that is executed in the context of an {@link SML_Executor}.
 * Instructions are identified by their {@link Instruction#operationCode
 * operation code} and also use an {@link Instruction#operand operand} as the
 * address of memory they will operate on.
 *
 * @author Alex Mandelias
 */
public enum Instruction {

	/**
	 * Instruction for reading an Integer
	 * <p>
	 * Operand: the address to store the Integer
	 */
	READ_INT(0x10) {
		@Override
		protected void execute(SML_Executor executor) {
			@SuppressWarnings("resource")
			final Scanner scanner = new Scanner(System.in);

			executor.prompt();
			final String input0 = scanner.nextLine();
			final int    input;

			try {
				input = Integer.parseInt(input0, 16);
			} catch (NumberFormatException e) {
				throw new NumberFormatException(input0);
			}

			executor.write(operand, input);
		}
	},

	/**
	 * Instruction for reading a String
	 * <p>
	 * Operand: the address to store the String
	 */
	READ_STRING(0x11) {
		@Override
		protected void execute(SML_Executor executor) {
			@SuppressWarnings("resource")
			final Scanner scanner = new Scanner(System.in);

			executor.prompt();
			final char[] array = scanner.nextLine().toCharArray();

			executor.writeChars(operand, array);
		}
	},

	/**
	 * Instruction for writing an Integer
	 * <p>
	 * Operand: the address to read the Integer
	 */
	WRITE(0x12) {
		@Override
		protected void execute(SML_Executor executor) {
			executor.output();
			executor.message("%04x", executor.read(operand));
		}
	},

	/**
	 * Instruction for writing an Integer and a newline character
	 * <p>
	 * Operand: the address to read the Integer
	 */
	WRITE_NL(0x13) {
		@Override
		protected void execute(SML_Executor executor) {
			executor.output();
			executor.message("%04x%n", executor.read(operand));
		}
	},

	/**
	 * Instruction for writing a String
	 * <p>
	 * Operand: the address to read the String
	 */
	WRITE_STRING(0x14) {
		@Override
		protected void execute(SML_Executor executor) {
			final char[] chars = executor.readChars(operand);

			executor.output();
			for (final char c : chars)
				executor.message("%c", c);
		}
	},

	/**
	 * Instruction for writing a String and a newline character
	 * <p>
	 * Operand: the address to read the String
	 */
	WRITE_STRING_NL(0x15) {
		@Override
		protected void execute(SML_Executor executor) {
			final char[] chars = executor.readChars(operand);

			executor.output();
			for (final char c : chars)
				executor.message("%c", c);
			executor.message("%n");
		}
	},

	/**
	 * Instruction for loading a value into the accumulator from memory
	 * <p>
	 * Operand: the address to load into the accumulator
	 */
	LOAD(0x20) {
		@Override
		protected void execute(SML_Executor executor) {
			executor.setAccumulator(executor.read(operand));
		}
	},

	/**
	 * Instruction for storing a value from the accumulator to memory
	 * <p>
	 * Operand: the address to store the accumulator
	 */
	STORE(0x21) {
		@Override
		protected void execute(SML_Executor executor) {
			executor.write(operand, executor.getAccumulator());
		}
	},

	/**
	 * Instruction for adding a value to the accumulator from memory
	 * <p>
	 * Operand: the address whose value to add to the accumulator
	 */
	ADD(0x30) {
		@Override
		protected void execute(SML_Executor executor) {
			final int sum = executor.getAccumulator() + executor.read(operand);
			executor.setAccumulator(sum);
		}
	},

	/**
	 * Instruction for subtracting a value from the accumulator from memory
	 * <p>
	 * Operand: the address whose value to subtract from the accumulator
	 */
	SUBTRACT(0x31) {
		@Override
		protected void execute(SML_Executor executor) {
			final int difference = executor.getAccumulator() - executor.read(operand);
			executor.setAccumulator(difference);
		}
	},

	/**
	 * Instruction for dividing the accumulator with a value from memory
	 * <p>
	 * Operand: the address with the value of which the accumulator will be divided
	 */
	DIVIDE(0x32) {
		@Override
		protected void execute(SML_Executor executor) {
			final int divisor = executor.read(operand);
			if (divisor == 0)
				throw new ArithmeticException("Division By 0");

			final int quotient = executor.getAccumulator() / divisor;
			executor.setAccumulator(quotient);
		}
	},

	/**
	 * Instruction for multiplying the accumulator with a value from memory
	 * <p>
	 * Operand: the address with the value of which the accumulator will be
	 * multiplied
	 */
	MULTIPLY(0x33) {
		@Override
		protected void execute(SML_Executor executor) {
			final int product = executor.getAccumulator() * executor.read(operand);
			executor.setAccumulator(product);
		}
	},

	/**
	 * Instruction for setting the accumulator to the remainder of its division with
	 * a value from memory
	 * <p>
	 * Operand: the address with the value of which the accumulator will be divided
	 */
	MOD(0x34) {
		@Override
		protected void execute(SML_Executor executor) {
			final int divisor = executor.read(operand);
			if (divisor == 0)
				throw new ArithmeticException("Division By 0");

			final int remainder = executor.getAccumulator() % divisor;
			executor.setAccumulator(remainder);
		}
	},

	/**
	 * Instruction for exponentiating the accumulator with a value from memory
	 * <p>
	 * Operand: the address with the value of which the accumulator will be
	 * exponentiated
	 */
	POW(0x35) {
		@Override
		protected void execute(SML_Executor executor) {
			final double power = Math.pow(executor.getAccumulator(), executor.read(operand));
			executor.setAccumulator((int) power);
		}
	},

	/**
	 * Instruction for unconditional jump
	 * <p>
	 * Operand: the address to jump to
	 */
	BRANCH(0x40) {
		@Override
		protected void execute(SML_Executor executor) {
			executor.setInstructionPointer(operand);
		}
	},

	/**
	 * Instruction for branch if the accumulator is negative
	 * <p>
	 * Operand: the address to jump to
	 */
	BRANCHNEG(0x41) {
		@Override
		protected void execute(SML_Executor executor) {
			if (executor.getAccumulator() < 0)
				executor.setInstructionPointer(operand);
		}
	},

	/**
	 * Instruction for branch if the accumulator is zero
	 * <p>
	 * Operand: the address to jump to
	 */
	BRANCHZERO(0x42) {
		@Override
		protected void execute(SML_Executor executor) {
			if (executor.getAccumulator() == 0)
				executor.setInstructionPointer(operand);
		}
	},

	/**
	 * Instruction for halting execution
	 * <p>
	 * Operand: not used
	 */
	HALT(0x43) {
		@Override
		protected void execute(SML_Executor executor) {
			executor.halt();
		}
	},

	/**
	 * Instruction for dumping memory contents to screen
	 * <p>
	 * Operand: not used
	 */
	DUMP(0xf0) {
		@Override
		protected void execute(SML_Executor executor) {
			executor.dump();
		}
	},

	/**
	 * Instruction for performing no operation
	 * <p>
	 * Operand: not used
	 */
	NOOP(0xf1) {
		@Override
		protected void execute(SML_Executor executor) {

		}
	};

	/** The byte that identifies this Instruction */
	private final int operationCode;

	/** The memory address this Instruction will use during execution */
	protected int operand;

	private static final Map<Integer, Instruction> map;

	static {
		map = new HashMap<>();
		for (final Instruction instruction : Instruction.values())
			Instruction.map.put(instruction.operationCode, instruction);
	}

	/**
	 * Executes the {@code Instruction}, altering the state of the
	 * {@code SML_Executor}.
	 *
	 * @param executor the SML_Executor
	 */
	protected abstract void execute(SML_Executor executor);

	/**
	 * Returns the Instruction identified by the {@link Instruction#operationCode
	 * operationCode}, while also specifying its {@link Instruction#operand
	 * operand}.
	 *
	 * @param operationCode the Instruction's operation code
	 * @param operand       the Instruction's new operand
	 *
	 * @return the Instruction with the operation code and the operand
	 *
	 * @throws InvalidInstructionException if the {@code operationCode} doesn't
	 *                                     correspond to an Instruction
	 */
	public static Instruction of(int operationCode, int operand)
			throws InvalidInstructionException {

		final Instruction instruction = Instruction.map.get(operationCode);
		if (instruction == null)
			throw new InvalidInstructionException(operationCode);

		instruction.operand = operand;
		return instruction;
	}

	/**
	 * Returns the {@code operationCode} of the Instruction multiplied by 0x100 so
	 * that it matches the format of {@code opCode * 0x100 + operand}.
	 *
	 * @return the operationCode
	 */
	public int opcode() {
		return operationCode * 0x100;
	}

	Instruction(int operationCode) {
		this.operationCode = operationCode;
	}
}

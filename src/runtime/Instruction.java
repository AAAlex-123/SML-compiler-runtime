package runtime;

import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import runtime.exceptions.InvalidInstructionException;

/**
 * An Instruction that is executed in the context of a SML_Executor.
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
		protected void execute() {
			@SuppressWarnings("resource")
			final Scanner scanner = new Scanner(System.in);

			SML_Executor.prompt();
			final int input = Integer.parseInt(scanner.nextLine(), 16);

			SML_Executor.write(operand, input);
		}
	},

	/**
	 * Instruction for reading a String
	 * <p>
	 * Operand: the address to store the String
	 */
	READ_STRING(0x11) {
		@Override
		protected void execute() {
			@SuppressWarnings("resource")
			final Scanner scanner = new Scanner(System.in);

			SML_Executor.prompt();
			final char[] array = scanner.nextLine().toCharArray();

			SML_Executor.writeChars(operand, array);
		}
	},

	/**
	 * Instruction for writing an Integer
	 * <p>
	 * Operand: the address to read the Integer
	 */
	WRITE(0x12) {
		@Override
		protected void execute() {
			SML_Executor.output();
			SML_Executor.message("%04x", SML_Executor.read(operand));
		}
	},

	/**
	 * Instruction for writing an Integer and a newline character
	 * <p>
	 * Operand: the address to read the Integer
	 */
	WRITE_NL(0x13) {
		@Override
		protected void execute() {
			SML_Executor.output();
			SML_Executor.message("%04x%n", SML_Executor.read(operand));
		}
	},

	/**
	 * Instruction for writing a String
	 * <p>
	 * Operand: the address to read the String
	 */
	WRITE_STRING(0x14) {
		@Override
		protected void execute() {
			final char[] chars = SML_Executor.readChars(operand);

			SML_Executor.output();
			for (final char c : chars)
				SML_Executor.message("%c", c);
		}
	},

	/**
	 * Instruction for writing a String and a newline character
	 * <p>
	 * Operand: the address to read the String
	 */
	WRITE_STRING_NL(0x15) {
		@Override
		protected void execute() {
			final char[] chars = SML_Executor.readChars(operand);

			SML_Executor.output();
			for (final char c : chars)
				SML_Executor.message("%c", c);
			SML_Executor.message("%n");
		}
	},

	/**
	 * Instruction for loading a value into the accumulator from memory
	 * <p>
	 * Operand: the address to load into the accumulator
	 */
	LOAD(0x20) {
		@Override
		protected void execute() {
			SML_Executor.setAccumulator(SML_Executor.read(operand));
		}
	},

	/**
	 * Instruction for storing a value from the accumulator to memory
	 * <p>
	 * Operand: the address to store the accumulator
	 */
	STORE(0x21) {
		@Override
		protected void execute() {
			SML_Executor.write(operand, SML_Executor.getAccumulator());
		}
	},

	/**
	 * Instruction for adding a value to the accumulator from memory
	 * <p>
	 * Operand: the address whose value to add to the accumulator
	 */
	ADD(0x30) {
		@Override
		protected void execute() {
			final int sum = SML_Executor.getAccumulator() + SML_Executor.read(operand);
			SML_Executor.setAccumulator(sum);
		}
	},

	/**
	 * Instruction for subtracting a value from the accumulator from memory
	 * <p>
	 * Operand: the address whose value to subtract from the accumulator
	 */
	SUBTRACT(0x31) {
		@Override
		protected void execute() {
			final int difference = SML_Executor.getAccumulator() - SML_Executor.read(operand);
			SML_Executor.setAccumulator(difference);
		}
	},

	/**
	 * Instruction for dividing the accumulator with a value from memory
	 * <p>
	 * Operand: the address with the value of which the accumulator will be divided
	 */
	DIVIDE(0x32) {
		@Override
		protected void execute() {
			final int divisor = SML_Executor.read(operand);
			if (divisor == 0)
				throw new ArithmeticException("Division By 0");

			final int quotient = SML_Executor.getAccumulator() / divisor;
			SML_Executor.setAccumulator(quotient);
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
		protected void execute() {
			final int product = SML_Executor.getAccumulator() * SML_Executor.read(operand);
			SML_Executor.setAccumulator(product);
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
		protected void execute() {
			final int divisor = SML_Executor.read(operand);
			if (divisor == 0)
				throw new ArithmeticException("Division By 0");

			final int remainder = SML_Executor.getAccumulator() % divisor;
			SML_Executor.setAccumulator(remainder);
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
		protected void execute() {
			final double power = Math.pow(SML_Executor.getAccumulator(),
			        SML_Executor.read(operand));
			SML_Executor.setAccumulator((int) power);
		}
	},

	/**
	 * Instruction for unconditional jump
	 * <p>
	 * Operand: the address to jump to
	 */
	BRANCH(0x40) {
		@Override
		protected void execute() {
			SML_Executor.setInstructionPointer(operand);
		}
	},

	/**
	 * Instruction for branch if the accumulator is negative
	 * <p>
	 * Operand: the address to jump to
	 */
	BRANCHNEG(0x41) {
		@Override
		protected void execute() {
			if (SML_Executor.getAccumulator() < 0)
				SML_Executor.setInstructionPointer(operand);
		}
	},

	/**
	 * Instruction for branch if the accumulator is zero
	 * <p>
	 * Operand: the address to jump to
	 */
	BRANCHZERO(0x42) {
		@Override
		protected void execute() {
			if (SML_Executor.getAccumulator() == 0)
				SML_Executor.setInstructionPointer(operand);
		}
	},

	/**
	 * Instruction for halting execution
	 * <p>
	 * Operand: not used
	 */
	HALT(0x43) {
		@Override
		protected void execute() {
			SML_Executor.halt();
		}
	},

	/**
	 * Instruction for dumping memory contents to screen
	 * <p>
	 * Operand: not used
	 */
	DUMP(0xf0) {
		@Override
		protected void execute() {
			SML_Executor.dump();
		}
	},

	/**
	 * Instruction for performing no operation
	 * <p>
	 * Operand: not used
	 */
	NOOP(0xf1) {
		@Override
		protected void execute() {

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

	/** Executes the Instruction */
	protected abstract void execute();

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

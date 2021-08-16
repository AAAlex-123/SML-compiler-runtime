package runtime;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;

import memory.CodeReader;
import memory.Memory;
import requirement.requirements.AbstractRequirement;
import requirement.requirements.Requirements;
import requirement.requirements.StringType;
import runtime.exceptions.InvalidInstructionException;

/**
 * An Executor for machine code. It defines the {@code static} method
 * {@link SML_Executor#execute execute} which loads machine-code instructions,
 * executes them and outputs the results. The Executor is {@code stateless}
 * meaning that no information is stored between executions and that an instance
 * of an Executor is not necessary to execute a program. Before each call to
 * {@code execute} the Executor is automatically reset.
 * <p>
 * The execution uses {@link requirement.requirements.AbstractRequirement
 * Requirements} in order to specify different parameters. They can be obtained
 * with the {@link SML_Executor#getRequirements() getRequriements()} method,
 * which contains more information about each individual Requirement.
 *
 * @author Alex Mandelias
 */
public class SML_Executor {

	private static final CodeReader memory = new Memory(256);

	/**
	 * The Executor's accumulator, used to load a single value from memory, operate
	 * on it, and then save it back to memory. A single accumulator is used (instead
	 * of multiple registers or directly operating on memory) to greatly simplify
	 * compilation and execution.
	 */
	private static int     accumulator;
	private static int     instructionRegister;
	private static int     operationCode;
	private static int     operand;
	private static boolean halt;

	/* Don't let anyone instantiate this class */
	private SML_Executor() {}

	/**
	 * Uses the command line arguments to specify the parameters necessary to
	 * execute a machine-code program, and then executes it. Parameters starting
	 * with a single dash '-' are set to {@code true}. Parameters starting with a
	 * double dash '--' are set to whatever the next argument is.
	 * <p>
	 * The different parameters are documented in the
	 * {@link SML_Executor#getRequirements() getRequirements()} method.
	 *
	 * @param args the command line arguments
	 */
	public static void main(String[] args) {

		final Requirements reqs = SML_Executor.getRequirements();

		for (int i = 0, count = args.length; i < count; ++i)
			if (args[i].startsWith("--"))
				reqs.fulfil(args[i].substring(2), args[++i]);
			else if (args[i].startsWith("-"))
				reqs.fulfil(args[i].substring(1), true);
			else
				SML_Executor.err(
				        "Invalid parameter: %s. Parameters must start with either one '-' or two '--' dashes.",
				        args[i]);

		SML_Executor.execute(reqs);
	}

	/**
	 * Returns the {@code Requirements} needed for execution. They have their
	 * default values and can be used as-is for execution.
	 *
	 * <pre>
	 * | Value   | Default | Explanation          | Command Line |
	 * |---------|---------|----------------------|--------------|
	 * | input   | out.sml | "stdin" or filename  | --           |
	 * | output  | res.txt | "stdout" or filename | --           |
	 * | screen  | false   | output to stdout too | -            |
	 * | verbose | false   | output all messages  | -            |
	 * </pre>
	 *
	 * @return the Requirements
	 */
	public static Requirements getRequirements() {
		final Requirements reqs = new Requirements();

		reqs.add("input", StringType.ANY);
		reqs.add("output", StringType.ANY);
		reqs.add("screen");
		reqs.add("verbose");

		reqs.fulfil("input", "out.sml");
		reqs.fulfil("output", "res.txt");
		reqs.fulfil("screen", false);
		reqs.fulfil("verbose", false);

		return reqs;
	}

	/**
	 * Uses the parameters from the {@code requirements} in order to load the
	 * program, execute it and output the results.
	 * <p>
	 * The different Requirements are documented in the
	 * {@link SML_Executor#getRequirements() getRequirements()} method.
	 *
	 * @param requirements the parameters needed to compile
	 */
	public static void execute(Requirements requirements) {
		if (!requirements.fulfilled()) {
			for (final AbstractRequirement r : requirements)
				if (!r.fulfilled())
					SML_Executor.err("No value for parameter '%s' found", r.key());

			SML_Executor.err("Execution couldn't start due to missing parameters");
			return;
		}

		SML_Executor.reset();

		final String  input   = (String) requirements.getValue("input");
		final String  output  = (String) requirements.getValue("output");
		final boolean screen  = (boolean) requirements.getValue("screen");
		final boolean verbose = (boolean) requirements.getValue("verbose");

		if (!verbose) {

			// === SILENT EXECUTION ===

			if (input.equals("stdin"))
				SML_Executor.loadToMemoryFromStdin();
			else
				SML_Executor.loadToMemoryFromFile(new File(input));

			SML_Executor.executeInstructionsFromMemory();

			if (screen || output.equals("stdout"))
				SML_Executor.writeResultsToStdout();
			if (!output.equals("stdout"))
				SML_Executor.writeResultsToFile(new File(output));

		} else {

			// === VERBOSE EXECUTION ===

			if (input.equals("stdin")) {
				SML_Executor.out("Loading progarm from Standard Input");
				SML_Executor.out("The memory address for each instruction will be printed");
				SML_Executor.out("All numbers are interpreted as hex");
				SML_Executor.out("Type '-ffff' to stop inputting code");
				SML_Executor.loadToMemoryFromStdin();
			} else {
				SML_Executor.out("Loading program from file: %s", input);
				SML_Executor.loadToMemoryFromFile(new File(input));
			}
			SML_Executor.out("Progarm loading completed");

			SML_Executor.out("Execution started");
			SML_Executor.executeInstructionsFromMemory();
			SML_Executor.out("Execution ended");

			if (screen || output.equals("stdout"))
				SML_Executor.out("Executor State:");
				SML_Executor.writeResultsToStdout();
			if (!output.equals("stdout")) {
				SML_Executor.out("Writing results to file: %s", output);
				SML_Executor.writeResultsToFile(new File(output));
			}
		}
	}

	private static void executeInstructionsFromMemory() {
		SML_Executor.memory.initialiseForExecution();

		while (!SML_Executor.halt) {
			SML_Executor.instructionRegister = SML_Executor.memory.fetchInstruction();
			SML_Executor.operationCode = SML_Executor.instructionRegister / 0x100;
			SML_Executor.operand = SML_Executor.instructionRegister % 0x100;

			try {
				Instruction.of(SML_Executor.operationCode, SML_Executor.operand).execute();
			} catch (final NumberFormatException e) {
				SML_Executor.err("%s is not a valid integer", e.getMessage());
			} catch (InvalidInstructionException | ArithmeticException e) {
				SML_Executor.err("%s", e.getMessage());
			}
		}
	}

	// --- 5 methods for input, output and reset ---

	private static void loadToMemoryFromStdin() {
		@SuppressWarnings("resource")
		final Scanner scanner = new Scanner(System.in);

		boolean valid;
		int     input     = 0;
		String  userInput = "";
		int     lineCount = 0;

		while (!userInput.equals("-ffff")) {
			valid = false;
			while (!valid) {
				SML_Executor.message("%02x ? ", lineCount);
				userInput = scanner.nextLine();

				try {
					input = Integer.parseInt(userInput, 16);
					valid = (-0xffff <= input) && (input <= 0xffff);
					if (!valid)
						SML_Executor.err("%s is out of range (-0xffff to 0xffff)", userInput);
				} catch (final NumberFormatException exc) {
					valid = false;
					SML_Executor.err("%s is not a valid integer", userInput);
				}
			}
			SML_Executor.write(lineCount, input);
			++lineCount;
		}
	}

	private static void loadToMemoryFromFile(File file) {
		try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
			int lineCount = 0;

			for (String line = reader.readLine(); line != null; line = reader.readLine()) {
				SML_Executor.write(lineCount, Integer.parseInt(line, 16));
				++lineCount;
			}

		} catch (final FileNotFoundException e) {
			SML_Executor.err("Couldn't find file %s", file);
		} catch (final NumberFormatException e) {
			SML_Executor.err("%s", e.getMessage());
		} catch (final IOException e) {
			SML_Executor.err("Unexpected error while reading from file %s", file);
		}
	}

	private static void writeResultsToStdout() {
		SML_Executor.out("%s", SML_Executor.getDumpString());
	}

	private static void writeResultsToFile(File file) {
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
			writer.write(SML_Executor.getDumpString());

		} catch (final IOException e) {
			SML_Executor.err("Unexpected error while writing to file %s", file);
		}
	}

	private static void reset() {
		SML_Executor.memory.clear();
		SML_Executor.accumulator = 0;
		SML_Executor.halt = false;
	}

	// --- 5 method for uniform message printing ---

	private static void out(String text, Object... args) {
		System.out.printf("Runtime Info:  %s%n", String.format(text, args));
	}

	private static void err(String text, Object... args) {
		System.err.printf("Runtime Error: %s%n", String.format(text, args));
	}

	/**
	 * Delegates to {@code System.out.printf(String, Object...)}
	 *
	 * @param text the text
	 * @param args the format arguments
	 */
	static void message(String text, Object... args) {
		System.out.printf(text, args);
	}

	/** Prints a prompt for input to standard out */
	static void prompt() {
		SML_Executor.message("> ");
	}

	/** Prints a prefix for another message to standard out */
	static void output() {
		SML_Executor.message("SML: ");
	}

	// --- 7 memory wrapper-delegate methods

	/** Halts execution */
	static void halt() {
		SML_Executor.halt = true;
	}

	/**
	 * Returns the {@link SML_Executor#accumulator accumulator}.
	 *
	 * @return the accumulator
	 */
	static int getAccumulator() {
		return SML_Executor.accumulator;
	}

	/**
	 * Sets the {@link SML_Executor#accumulator accumulator} to the {@code value}.
	 *
	 * @param value the new value for the accumulator
	 */
	static void setAccumulator(int value) {
		SML_Executor.accumulator = value;
	}

	/**
	 * Delegate method.
	 *
	 * @param address the address to read
	 *
	 * @return the value read
	 *
	 * @see memory.RAM#read(int)
	 */
	static int read(int address) {
		return SML_Executor.memory.read(address);
	}

	/**
	 * Delegate method.
	 *
	 * @param address the address of the first value to read
	 *
	 * @return the values read
	 *
	 * @see memory.RAM#readChars(int)
	 */
	static char[] readChars(int address) {
		return SML_Executor.memory.readChars(address);
	}

	/**
	 * Delegate method.
	 *
	 * @param address the address to write
	 * @param value   the value to write
	 *
	 * @see memory.RAM#write(int, int)
	 */
	static void write(int address, int value) {
		SML_Executor.memory.write(address, value);
	}

	/**
	 * Delegate method.
	 *
	 * @param address the address to of the first value to write
	 * @param values  the values to write
	 *
	 * @see memory.RAM#writeChars(int, char[])
	 */
	static void writeChars(int address, char[] values) {
		SML_Executor.memory.writeChars(address, values);
	}

	/**
	 * Delegate method.
	 *
	 * @param address the address to set the instruction pointer to
	 *
	 * @see memory.CodeReader#setInstructionPointer(int)
	 */
	static void setInstructionPointer(int address) {
		SML_Executor.memory.setInstructionPointer(address);
	}

	/**
	 * Delegate method.
	 *
	 * @return the dump String
	 *
	 * @see memory.CodeReader#dump()
	 */
	static String dump() {
		return SML_Executor.memory.dump();
	}

	// d u m p

	private static String getDumpString() {
		final StringBuilder sb = new StringBuilder();
		sb.append("REGISTERES:")
		        .append("\naccumulator:            " + SML_Executor.accumulator)
		        .append("\ninstruction counter:    " + SML_Executor.memory.getInstructionPointer())
		        .append("\ninstruction register:   " + SML_Executor.instructionRegister)
		        .append("\noperation code:         " + SML_Executor.operationCode)
		        .append("\noperand:                " + SML_Executor.operand)
		        .append(String.format("\n\n\nMEMORY:\n"))
		        .append(SML_Executor.dump());

		return sb.toString();
	}
}

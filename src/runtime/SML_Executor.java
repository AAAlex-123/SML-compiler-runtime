package runtime;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.Scanner;

import memory.CodeReader;
import memory.Memory;
import requirement.requirements.AbstractRequirement;
import requirement.requirements.Requirements;
import requirement.requirements.StringType;
import runtime.exceptions.InvalidInstructionException;
import utility.StreamSet;

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

	private final InputStream inputStream;
	private final PrintStream outputStream, errorStream;

	private final CodeReader memory;

	/**
	 * The Executor's accumulator, used to load a single value from memory, operate
	 * on it, and then save it back to memory. A single accumulator is used (instead
	 * of multiple registers or directly operating on memory) to greatly simplify
	 * compilation and execution.
	 */
	private int accumulator;
	private int instructionRegister;
	private int operationCode;
	private int operand;
	private boolean halt;

	/** Constructs an Executor with the "standard" in, out and error streams */
	public SML_Executor() {
		this(new StreamSet());
	}

	/**
	 * Constructs an Executor using a {@code StreamSet}, which can be obtained by
	 * calling the static {@link #streams()} method.
	 *
	 * @param streamset the set of Streams with which to construct the Executor
	 *
	 * @see StreamSet
	 */
	public SML_Executor(StreamSet streamset) {
		this(streamset.in, streamset.out, streamset.err);
	}

	/**
	 * Constructs an Executor using the streams provided.
	 *
	 * @param in  the Executor's Input Stream
	 * @param out the Executor's Output Stream
	 * @param err the Executor's Error Stream
	 */
	public SML_Executor(InputStream in, PrintStream out, PrintStream err) {
		inputStream = in;
		outputStream = out;
		errorStream = err;

		memory = new Memory(256);
		accumulator = 0;
		halt = false;
	}

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

		final SML_Executor executor = new SML_Executor();

		for (int i = 0, count = args.length; i < count; ++i)
			if (args[i].startsWith("--"))
				reqs.fulfil(args[i].substring(2), args[++i]);
			else if (args[i].startsWith("-"))
				reqs.fulfil(args[i].substring(1), true);
			else
				executor.err(
						"Invalid parameter: %s. Parameters must start with either one '-' or two '--' dashes.",
						args[i]);

		executor.execute(reqs);
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
	 * Returns a {@code StreamSet} that can be passed as a parameter to construct an
	 * Executor. The {@code StreamSet} can be configured with different input,
	 * output and error Streams for the Executor to use instead of the "standard"
	 * in, out and err Streams of the {@code System} class.
	 *
	 * @return the StreamSet
	 *
	 * @see StreamSet
	 */
	public static StreamSet streams() {
		return new StreamSet();
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
	public void execute(Requirements requirements) {
		if (!requirements.fulfilled()) {
			for (final AbstractRequirement r : requirements)
				if (!r.fulfilled())
					err("No value for parameter '%s' found", r.key());

			err("Execution couldn't start due to missing parameters");
			return;
		}

		final String  input   = (String) requirements.getValue("input");
		final String  output  = (String) requirements.getValue("output");
		final boolean screen  = (boolean) requirements.getValue("screen");
		final boolean verbose = (boolean) requirements.getValue("verbose");

		if (!verbose) {

			// === SILENT EXECUTION ===

			if (input.equals("stdin"))
				loadToMemoryFromStdin();
			else
				loadToMemoryFromFile(new File(input));

			executeInstructionsFromMemory();

			if (screen || output.equals("stdout"))
				writeResultsToStdout();
			if (!output.equals("stdout"))
				writeResultsToFile(new File(output));

		} else {

			// === VERBOSE EXECUTION ===

			if (input.equals("stdin")) {
				out("Loading progarm from Standard Input");
				out("The memory address for each instruction will be printed");
				out("All numbers are interpreted as hex");
				out("Type '-ffff' to stop inputting code");
				loadToMemoryFromStdin();
			} else {
				out("Loading program from file: %s", input);
				loadToMemoryFromFile(new File(input));
			}
			out("Progarm loading completed");

			out("Execution started");
			executeInstructionsFromMemory();
			out("Execution ended");

			if (screen || output.equals("stdout"))
				out("Executor State:");
			writeResultsToStdout();
			if (!output.equals("stdout")) {
				out("Writing results to file: %s", output);
				writeResultsToFile(new File(output));
			}
		}
	}

	private void executeInstructionsFromMemory() {
		memory.initialiseForExecution();

		try {
			while (!halt) {
				instructionRegister = memory.fetchInstruction();
				operationCode = instructionRegister / 0x100;
				operand = instructionRegister % 0x100;

				Instruction.of(operationCode, operand).execute(this);
			}
		} catch (final NumberFormatException e) {
			// This assumes that the exception's message is the number that isn't an integer
			err("'%s' is not a valid base-16 integer", e.getMessage());
		} catch (InvalidInstructionException | ArithmeticException e) {
			err("%s", e.getMessage());
		}
	}

	// --- 4 methods for input, output ---

	private void loadToMemoryFromStdin() {
		@SuppressWarnings("resource")
		final Scanner scanner = new Scanner(inputStream);

		boolean valid;
		int     input     = 0;
		String  userInput = "";
		int     lineCount = 0;

		while (!userInput.equals("-ffff")) {
			valid = false;
			while (!valid) {
				message("%02x ? ", lineCount);
				userInput = scanner.nextLine();

				try {
					input = Integer.parseInt(userInput, 16);
					valid = (-0xffff <= input) && (input <= 0xffff);
					if (!valid)
						err("%s is out of range (-0xffff to 0xffff)", userInput);
				} catch (final NumberFormatException exc) {
					valid = false;
					err("%s is not a valid integer", userInput);
				}
			}
			write(lineCount, input);
			++lineCount;
		}
	}

	private void loadToMemoryFromFile(File file) {
		String line = "";
		try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
			int lineCount = 0;

			for (line = reader.readLine(); line != null; line = reader.readLine()) {
				write(lineCount, Integer.parseInt(line, 16));
				++lineCount;
			}

		} catch (final FileNotFoundException e) {
			err("Couldn't find file %s", file);
		} catch (final NumberFormatException e) {
			err("'%s' is not a valid base-16 integer", line);
		} catch (final IOException e) {
			err("Unexpected error while reading from file %s", file);
		}
	}

	private void writeResultsToStdout() {
		out("%s", getDumpString());
	}

	private void writeResultsToFile(File file) {
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
			writer.write(getDumpString());

		} catch (final IOException e) {
			err("Unexpected error while writing to file %s", file);
		}
	}

	// --- 5 method for uniform message printing ---

	private void out(String format, Object... args) {
		outputStream.printf("Runtime Info:  %s%n", String.format(format, args));
		outputStream.flush();
	}

	private void err(String format, Object... args) {
		errorStream.printf("Runtime Error: %s%n", String.format(format, args));
		errorStream.flush();
	}

	/**
	 * Delegates to {@code outputStream.printf(format, args)}
	 *
	 * @param format the text
	 * @param args   the format arguments
	 */
	void message(String format, Object... args) {
		outputStream.printf(format, args);
	}

	/** Prints a prompt for input to standard out */
	void prompt() {
		message("> ");
	}

	/** Prints a prefix for another message to standard out */
	void output() {
		message("SML: ");
	}

	// --- 7 memory wrapper-delegate methods

	/** Halts execution */
	void halt() {
		halt = true;
	}

	/**
	 * Returns the {@link SML_Executor#accumulator accumulator}.
	 *
	 * @return the accumulator
	 */
	int getAccumulator() {
		return accumulator;
	}

	/**
	 * Sets the {@link SML_Executor#accumulator accumulator} to the {@code value}.
	 *
	 * @param value the new value for the accumulator
	 */
	void setAccumulator(int value) {
		accumulator = value;
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
	int read(int address) {
		return memory.read(address);
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
	char[] readChars(int address) {
		return memory.readChars(address);
	}

	/**
	 * Delegate method.
	 *
	 * @param address the address to write
	 * @param value   the value to write
	 *
	 * @see memory.RAM#write(int, int)
	 */
	void write(int address, int value) {
		memory.write(address, value);
	}

	/**
	 * Delegate method.
	 *
	 * @param address the address to of the first value to write
	 * @param values  the values to write
	 *
	 * @see memory.RAM#writeChars(int, char[])
	 */
	void writeChars(int address, char[] values) {
		memory.writeChars(address, values);
	}

	/**
	 * Delegate method.
	 *
	 * @param address the address to set the instruction pointer to
	 *
	 * @see memory.CodeReader#setInstructionPointer(int)
	 */
	void setInstructionPointer(int address) {
		memory.setInstructionPointer(address);
	}

	/**
	 * Delegate method.
	 *
	 * @return the dump String
	 *
	 * @see memory.CodeReader#dump()
	 */
	String dump() {
		return memory.dump();
	}

	// d u m p

	private String getDumpString() {
		final StringBuilder sb = new StringBuilder();
		sb.append("REGISTERES:")
		.append("\naccumulator:            " + accumulator)
		.append("\ninstruction counter:    " + memory.getInstructionPointer())
		.append("\ninstruction register:   " + instructionRegister)
		.append("\noperation code:         " + operationCode)
		.append("\noperand:                " + operand)
		.append(String.format("\n\n\nMEMORY:\n"))
		.append(dump());

		return sb.toString();
	}
}

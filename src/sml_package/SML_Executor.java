package sml_package;

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

public class SML_Executor {

	private SML_Executor() {}

	static final CodeReader memory = new Memory(256);

	static final int        READ_INT        = 0x10;
	static final int        READ_STRING     = 0x11;
	static final int        WRITE           = 0x12;
	static final int        WRITE_NL        = 0x13;
	static final int        WRITE_STRING    = 0x14;
	static final int        WRITE_STRING_NL = 0x15;
	public static final int LOAD            = 0x20;
	public static final int STORE           = 0x21;
	public static final int ADD             = 0x30;
	public static final int SUBTRACT        = 0x31;
	public static final int DIVIDE          = 0x32;
	public static final int MULTIPLY        = 0x33;
	public static final int MOD             = 0x34;
	public static final int POW             = 0x35;
	static final int        BRANCH          = 0x40;
	static final int        BRANCHNEG       = 0x41;
	static final int        BRANCHZERO      = 0x42;
	static final int        HALT            = 0x43;
	static final int        DUMP            = 0xf0;
	static final int        NOOP            = 0xf1;

	private static int lineCount;
	static int         accumulator;
	static int         instructionCounter;
	private static int instructionRegister;
	private static int operationCode;
	private static int operand;
	static boolean     halt;

	public static void main(String[] args) {
		System.out.println("*** Welcome to Program!\t\t\t ***");

		Requirements reqs = getRequirements();

		for (int i = 0; i < args.length; ++i) {
			if (args[i].startsWith("--"))
				reqs.fulfil(args[i], args[++i]);
			else if (args[i].startsWith("-"))
				reqs.fulfil(args[i], true);
			else
				err("Error: invalid argument: %s", args[i]);
		}

		execute(reqs);
	}

	public static Requirements getRequirements() {
		Requirements reqs = new Requirements();

		reqs.add("input", StringType.ANY);
		reqs.add("output", StringType.ANY);
		reqs.add("screen");

		reqs.fulfil("input", "stdin");
		reqs.fulfil("output", "#");
		reqs.fulfil("screen", false);

		return reqs;
	}

	static void execute(Requirements reqs) {
		if (!reqs.fulfilled()) {
			for (AbstractRequirement r : reqs) {
				if (!r.fulfilled())
					err("No value for parameter %s found.%n", r.key());
			}
			err("Execution terminated due to above erros.%n");
			return;
		}

		reset();

		String input = (String) reqs.getValue("input");

		if (input.equals("stdin"))
			loadToMemoryFromStdin();
		else
			loadToMemoryFromFile(new File(input));

		executeInstructionsFromMemory();

		String  output = (String) reqs.getValue("output");
		boolean screen = (boolean) reqs.getValue("screen");

		if (screen)
			writeResultsToStdout();
		if (!output.equals("#"))
			writeResultsToFile(new File(output));
	}

	private static void executeInstructionsFromMemory() {
		System.out.println("*** Program execution begins\t\t ***");
		memory.initialiseForExecution();
		instructionCounter = instructionRegister = operationCode = operand = accumulator = 0;
		halt = false;

		while (!halt) {
			instructionRegister = memory.fetchInstruction();
			operationCode = instructionRegister / 0x100;
			operand = instructionRegister % 0x100;

			Command.from(operationCode, operand).execute();
		}
	}

	private static void loadToMemoryFromStdin() {
		System.out.println("*** Please enter your program one instruction\t ***");
		System.out.println("*** (or data word) at a time. I will display\t ***");
		System.out.println("*** the location number and a quesiton mark (?). ***");
		System.out.println("*** You then type the word for that location.\t ***");
		System.out.println("*** Type -ffff to stop entering your program.\t ***");
		System.out.println("*** Note: all numbers are interpreted as hex.\t ***");

		@SuppressWarnings("resource")
		Scanner scanner = new Scanner(System.in);

		boolean valid;
		int     input     = 0;
		String  userInput = "";

		while (!userInput.equals("-ffff")) {
			valid = false;
			while (!valid) {
				out("%02d ? ", lineCount);
				userInput = scanner.nextLine();

				try {
					input = Integer.parseInt(userInput, 16);
					valid = (-0xffff <= input) && (input <= 0xffff);
					if (!valid)
						err("%s is out of range (-0xffff to 0xffff).%n",
						        userInput);
				} catch (NumberFormatException exc) {
					valid = false;
					err("%s is not a valid int.%n", userInput);
				}
			}
			memory.write(lineCount++, input);
		}
		out("*** Program loading completed\t\t ***");
	}

	private static void loadToMemoryFromFile(File file) {
		try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
			String line;
			while ((line = reader.readLine()) != null)
				memory.write(lineCount++, Integer.parseInt(line));

			System.out.println("*** Program loading completed\t\t ***");
		} catch (FileNotFoundException e) {
			err("Couldn't find file %s.%n", file);
		} catch (NumberFormatException e) {
			err("%s", e.getMessage());
		} catch (IOException e) {
			err("Unexpected error while reading from file %s.%n", file);
		}
	}

	private static void writeResultsToStdout() {
		out(getDumpString());
	}

	private static void writeResultsToFile(File file) {
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
			writer.write(getDumpString());
		} catch (IOException e) {
			err("Unexpected error while writing to file %s.%n", file);
		}
	}

	static void out(String text, Object... args) {
		System.out.printf("EXEC: " + text, args);
	}

	static void err(String text, Object... args) {
		System.err.printf("EXECERROR: " + text, args);
	}

	private static String getDumpString() {
		StringBuilder sb = new StringBuilder();
		sb.append("REGISTERES:")
		        .append("\naccumulator:            " + accumulator)
		        .append("\ninstruction counter:    " + instructionCounter)
		        .append("\ninstruction register:   " + instructionRegister)
		        .append("\noperation code:         " + operationCode)
		        .append("\noperand:                " + operand)
		        .append(String.format("\n\n\nMEMORY:\n"))
		        .append(memory);

		return sb.toString();
	}

	private static void reset() {
		memory.clear();
		lineCount = 0;

		accumulator = 0;
		instructionCounter = 0;
		instructionRegister = 0;
		operationCode = 0;
		operand = 0;
	}
}

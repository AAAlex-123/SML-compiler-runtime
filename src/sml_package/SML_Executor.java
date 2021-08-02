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

	static int         accumulator;
	private static int instructionRegister;
	private static int operationCode;
	private static int operand;
	private static boolean halt;

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
		reqs.fulfil("output", "stdout");
		reqs.fulfil("screen", false);

		return reqs;
	}

	static void halt() {
		halt = true;
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

		if (screen || output.equals("stdout"))
			writeResultsToStdout();
		if (!output.equals("stdout"))
			writeResultsToFile(new File(output));
	}

	private static void executeInstructionsFromMemory() {
		System.out.println("*** Program execution begins\t\t ***");
		memory.initialiseForExecution();
		accumulator = 0;
		halt = false;

		while (!halt) {
			instructionRegister = memory.fetchInstruction();
			operationCode = instructionRegister / 0x100;
			operand = instructionRegister % 0x100;

			System.out.printf("from: %02x %02x%n", operationCode, operand);
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
		int     lineCount = 0;

		while (!userInput.equals("-ffff")) {
			valid = false;
			while (!valid) {
				out("%02x ? ", lineCount);
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
			memory.write(lineCount, input);
			++lineCount;
		}
		out("*** Program loading completed\t\t ***");
	}

	private static void loadToMemoryFromFile(File file) {
		System.out.println(file);
		try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
			int    lineCount = 0;
			String line = reader.readLine();

			while (line != null) {
				memory.write(lineCount, Integer.parseInt(line, 16));
				++lineCount;
				line = reader.readLine();
			}

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
		        .append("\ninstruction counter:    " + memory.getInstructionPointer())
		        .append("\ninstruction register:   " + instructionRegister)
		        .append("\noperation code:         " + operationCode)
		        .append("\noperand:                " + operand)
		        .append(String.format("\n\n\nMEMORY:\n"))
		        .append(memory.dump());

		return sb.toString();
	}

	private static void reset() {
		memory.clear();
		accumulator = 0;
	}
}

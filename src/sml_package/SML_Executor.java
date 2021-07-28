package sml_package;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Scanner;

public class SML_Executor {

	static final Memory memory = new Memory(256);

	static final int READ_INT = 0x10;
	static final int READ_STRING = 0x11;
	static final int WRITE = 0x12;
	static final int WRITE_NL = 0x13;
	static final int WRITE_STRING = 0x14;
	static final int WRITE_STRING_NL = 0x15;
	public static final int LOAD            = 0x20;
	public static final int STORE           = 0x21;
	public static final int ADD             = 0x30;
	public static final int SUBTRACT        = 0x31;
	public static final int DIVIDE          = 0x32;
	public static final int MULTIPLY        = 0x33;
	public static final int MOD             = 0x34;
	public static final int POW             = 0x35;
	static final int BRANCH = 0x40;
	static final int BRANCHNEG = 0x41;
	static final int BRANCHZERO = 0x42;
	static final int HALT = 0x43;
	static final int DUMP = 0xf0;
	static final int NOOP = 0xf1;

	private static boolean ok = false;
	private static int lineCount = 00;
	private static String userInput = "";

	private static int accumulator;
	private static int instructionCounter;
	private static int instructionRegister;
	private static int operationCode;
	private static int operand;
	private static boolean halt;

	private static Scanner scanner;
	private static FileWriter fileWriter;

	// "C:\\Users\\Danae\\Desktop\\projects\\Java\\SML\\SML.txt";
	// "C:\\Users\\Danae\\Desktop\\projects\\Java\\SML\\out.txt";

	public static void main(String[] args) throws IOException {
		scanner = new Scanner(System.in);
		try {
			System.out.println("*** Welcome to Program!\t\t\t ***");
			scanner = new Scanner(new File(args[0]));
		} catch (IndexOutOfBoundsException ioob) {
			System.out.println("*** Welcome to Program!\t\t\t ***");
			System.out.println("*** Please enter your program one instruction\t ***");
			System.out.println("*** (or data word) at a time. I will display\t ***");
			System.out.println("*** the location number and a quesiton mark (?). ***");
			System.out.println("*** You then type the word for that location.\t ***");
			System.out.println("*** Type -ffff to stop entering your program.\t ***");
			System.out.println("*** Note: all numbers are interpreted as hex.\t ***");
			manualRead();
		}
		executeInstructions();
		writeToScreen();
		try {
			fileWriter = new FileWriter(args[1]);
			writeToFile();
			System.out.printf("Results stored in file:\n%s\n", args[1]);
		} catch (IndexOutOfBoundsException ioob) {;}
		scanner.close();
		fileWriter.close();
	}

	static int execute(HashMap<String, String> options) throws IOException  {
		reset();

		int res = 0;
		try {scanner = new Scanner(new File(options.get("--input")));}
		catch (FileNotFoundException e) {;}

		try {fileWriter = new FileWriter(new File(options.get("--output")));}
		catch (IOException e) {;}

		if (options.get("-manual").equals("true"))
			scanner = new Scanner(System.in);

		if ((scanner == null) && options.get("-manual").equals("false")) {
			System.out.println("Executor Error: no input stream found");
			res = 1;
		}
//		if (fileWriter == null && options.get("-screen").equals("false")) {
//			System.out.println("Executor Error: no output stream found");
//			res = 1;
//		}

		if (res == 1) return res;

		if (scanner != null)
			read();
		else
			manualRead();

		int execRes = executeInstructions();
		if (execRes == 1) return 1;

		if (fileWriter != null)
			writeToFile();
		else
			if (options.get("-d").equals("true"))
				writeToScreen();

		return 0;
	}


	private static void manualRead() {
		scanner = new Scanner(System.in);
		int int_input = -1;
		while (!userInput.equals("-ffff")) {
			ok = false;
			while (!ok) {
				System.out.printf("%02d ? ", lineCount);
				userInput = scanner.nextLine();

				ok = true;
				try {int_input = Integer.parseInt(userInput, 16);}
				catch (NumberFormatException exc) {ok = false; int_input=0x10000;}

				ok = (-0xffff <= int_input) && (int_input <= 0xffff);
			}
			memory.write(lineCount++, int_input);
		}
		System.out.println("*** Program loading completed\t\t ***");
	}

	static void read() {
		while (scanner.hasNext())
			memory.write(lineCount++, Integer.parseInt(scanner.nextLine(), 16);
		System.out.println("*** Program loading completed\t\t ***");
	}

	static int executeInstructions() {
		scanner = new Scanner(System.in);
		System.out.println("*** Program execution begins\t\t ***");
		instructionCounter = instructionRegister = operationCode = operand = accumulator = 0;
		halt = false;

		while (!halt) {
			instructionRegister = memory.read(instructionCounter++);
			operationCode = instructionRegister / 0x100;
			operand = instructionRegister % 0x100;

			if (operationCode == READ_INT) {						// READ_INT
				int int_input = 0;
				ok = false;
				while (!ok) {
					System.out.print("> ");
					userInput = scanner.nextLine();

					ok = true;
					try {int_input = Integer.parseInt(userInput, 16);}
					catch (NumberFormatException exc) {ok = false; int_input=0x10000;}

					ok = (-0xffff <= int_input) && (int_input <= 0xffff);
				};
				memory.write(operand, int_input);
			} else if (operationCode == READ_STRING) { 				// READ_STRING
				System.out.print("> ");
				String s = scanner.nextLine();
				char[] arr = s.toCharArray();
				memory.writeChars(operand, arr);
			} else if (operationCode == WRITE)						// WRITE_INT
				System.out.printf("SML: %04x", memory.read(operand));
			else if (operationCode == WRITE_NL)						// WRITE_INT_NL
				System.out.printf("SML: %04x\n", memory.read(operand));
			else if (operationCode == WRITE_STRING) {				// WRITE_STRING
				char[] arr = memory.readChars(operand);
				for (char c : arr) System.out.print(c);
			} else if (operationCode == WRITE_STRING_NL) {			// WRITE_STRING_NL
				char[] arr = memory.readChars(operand);
				for (char c : arr) System.out.print(c);
				System.out.println();
			} else if (operationCode == LOAD)						// LOAD
				accumulator = memory.read(operand);
			else if (operationCode == STORE)						// STORE
				memory.write(operand, accumulator);
			else if (operationCode == ADD) {						// ADD
				accumulator += memory.read(operand);
				if ((accumulator < -0xffff) || (accumulator > 0xffff)) {
					System.out.println("*** Overflow Error\t\t ***");

					accumulator -= memory.read(operand);
					System.out.println("\n*** Program execution abnormally terminated ***");
					return 1;
				}
			} else if (operationCode == SUBTRACT) {					// SUBTRACT
				accumulator -= memory.read(operand);
				if ((accumulator < -0xffff) || (accumulator > 0xffff)) {
					System.out.println("*** Overflow Error\t\t ***");
					accumulator += memory.read(operand);
					System.out.println("\n*** Program execution abnormally terminated ***");
					return 1;
				}
			} else if (operationCode == DIVIDE) {					// DIVIDE
				if (memory.read(operand); == 0) {
					System.out.println("*** Attempt to divide by zero\t\t ***");
					System.out.println("\n*** Program execution abnormally terminated ***");
					return 1;
				}
				accumulator /= memory.read(operand);
				if ((accumulator < -0xffff) || (accumulator > 0xffff)) {
					System.out.println("*** Overflow Error\t\t ***");
					accumulator *= memory.read(operand);
					System.out.println("\n*** Program execution abnormally terminated ***");
					return 1;
				}
			} else if (operationCode == MULTIPLY)	{				// MULTIPLY
				accumulator *= memory.read(operand);
				if ((accumulator < -0xffff) || (accumulator > 0xffff)) {
					System.out.println("*** Overflow Error\t\t ***");
					accumulator /= memory.read(operand);
					System.out.println("\n*** Program execution abnormally terminated ***");
					return 1;
				}
			} else if (operationCode == MOD)	{					// REMAINDER
				if (memory.read(operand); == 0) {
					System.out.println("*** Attempt to divide by zero\t\t ***");
					System.out.println("\n*** Program execution abnormally terminated ***");
					return 1;
				}
				accumulator %= memory.read(operand);
			} else if (operationCode == POW) {
				double temp = Math.pow(accumulator, memory.read(operand););
				if ((temp < -0xffff) || (temp > 0xffff)) {
					System.out.println("*** Overflow Error\t\t ***");
					System.out.println("\n*** Program execution abnormally terminated ***");
					return 1;
				}
				accumulator = (int) temp;
			} else if (operationCode == BRANCH)	{					// BRANCH
				instructionCounter = operand;
			} else if (operationCode == BRANCHNEG) {				// BRANCHNEG
				if (accumulator < 0)
					instructionCounter = operand;
			} else if (operationCode == BRANCHZERO) {				// BRANCHZERO
				if (accumulator == 0)
					instructionCounter = operand;
			} else if (operationCode == HALT) {						// HALT
				halt = true;
				System.out.println("*** Program execution terminated\t ***");
				return 0;
			} else if (operationCode == DUMP) {
				writeToScreen();
			} else if (operationCode == NOOP) {
				;
			} else {
				System.out.printf("*** Invalid Operation Code: %02x\t ***\n", operationCode);
				System.out.println("\n*** Program execution abnormally terminated ***");
				return 1;
			}
		}
		System.out.println(":(");
		return -1;
	}

	static void writeToFile() throws IOException {
		String s = generateString();
		fileWriter.write(s);
		fileWriter.close();
	}
	static void writeToScreen() {
		String s = generateString();
		System.out.println(s);
	}
	private static String generateString() {
		String s = "";
		s += String.format("REGISTERS:\r\n");
		s += String.format("accumulator\t\t\t%s\r\n", accumulator);
		s += String.format("instructionCounter\t%s\r\n", instructionCounter);
		s += String.format("instructionRegister\t%s\r\n", instructionRegister);
		s += String.format("operationCode\t\t%s\r\n", operationCode);
		s += String.format("operand\t\t\t\t%s\r\n", operand);

		s += String.format("\r\n\r\nMEMORY:\r\n  ");
		s += memory.toString();

		return s;
	}

	private static void reset() {
		memory.clear();
		ok = false;
		lineCount = 00;
		userInput = "";

		accumulator = 0;
		instructionCounter = 0;
		instructionRegister = 0;
		operationCode = 0;
		operand = 0;

		scanner = null;
		fileWriter = null;
	}
}

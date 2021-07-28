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

	static void out(String text, Object... args) {
		System.out.printf(text, args);
	}

	static void err(String text, Object... args) {
		System.err.printf(text, args);
	}

	static int executeInstructions() {
		scanner = new Scanner(System.in);
		System.out.println("*** Program execution begins\t\t ***");
		instructionCounter = instructionRegister = operationCode = operand = accumulator = 0;
		halt = false;

		while (!halt) {
			instructionRegister = memory.fetchInstruction();
			operationCode = instructionRegister / 0x100;
			operand = instructionRegister % 0x100;

			Command.from(operationCode, operand).execute();
		}
		return 0;
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

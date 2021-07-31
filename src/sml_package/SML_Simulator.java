package sml_package;

import java.util.HashMap;
import java.util.Scanner;

import requirement.requirements.Requirements;

public class SML_Simulator {

	private static final Scanner scanner = new Scanner(System.in);

	private static final String msg = "Usage:\n" +
	        "       help    [command]\n" +
	        "   or  compile [options]\n" +
	        "   or  execute [options]\n" +
	        "   or  com_exe [options]\n" +
	        "   or  exit\n" +
	        "\n" +
	        "where options include:\n" +
	        "   -screen\n" +
	        "       write SML and/or program output to screen, independent from file\n" +
	        "\n" +
	        "   -st\n" +
	        "       only used on when compiling, show the SymbolTable at the end.\n" +
	        "\n" +
	        "   -manual\n" +
	        "       write Simple code or SML code by hand, overwrites file\n" +
	        "\n" +
	        "   --input <filename>\n" +
	        "       where the data for compilation or execution are stored\n" +
	        "\n" +
	        "   --output <filename>\n" +
	        "       where the results after compilation or execution are stored\n" +
	        "\n" +
	        "   --inter <filename>\n" +
	        "       only used at com_exe, the file where SML stored after compilation and read for execution\n"
	        +
	        "\n" +
	        "where command is one of:\n" +
	        "   compile, execute, com_exe, exit, help\n";

	public static void main(String[] args) {

		System.out.println(msg);

		String   userInput;
		String[] tokens;
		String   command = "";

		Requirements compileReqs = SML_Compiler.getRequirements();
		Requirements executeReqs = SML_Executor.getRequirements();

		while (!command.equals("exit")) {
			compileReqs.clear();
			executeReqs.clear();

			System.out.print("=> ");

			userInput = scanner.nextLine().trim();
			tokens = userInput.split(" ");
			command = tokens[0];

			HashMap<String, String> options = new HashMap<>(7, 1.0f);
			options.put("-screen", "false");
			options.put("-manual", "false");
			options.put("-st", "false");
			options.put("-d", "false");
			options.put("--input", "");
			options.put("--output", "");
			options.put("--inter", "");

			if (command.equals("help")) {
				if (tokens.length == 1)
					System.out.println("Use help [command] to get help on a specific command");
				else
					printHelpForCommand(tokens[1]);

			} else {

				// parse args
				for (int i = 1; i < tokens.length; ++i) {
					if (tokens[i].startsWith("--"))
						options.put(tokens[i], tokens[++i]);
					else if (tokens[i].startsWith("-"))
						options.put(tokens[i], "true");
					else
						System.out.println("Error: invalid option: " + tokens[i]);
				}

				// do stuff according to command
				if (command.equals("compile")) { // COMPILE

					compileReqs.fulfil("input", options.get("--input"));
					compileReqs.fulfil("output", options.get("--output"));
					compileReqs.fulfil("screen", options.get("-screen").equals("true"));
					compileReqs.fulfil("st", options.get("-st").equals("true"));

					SML_Compiler.compile(compileReqs);

				} else if (command.equals("execute")) {

					executeReqs.fulfil("input", options.get("--input"));
					executeReqs.fulfil("output", options.get("--output"));
					executeReqs.fulfil("screen", options.get("-screen").equals("true"));

					SML_Executor.execute(executeReqs);

				} else if (command.equals("com_exe")) { // COM_EXE

					boolean screen = options.get("-screen").equals("true");

					// compile
					compileReqs.fulfil("input", options.get("--input"));
					compileReqs.fulfil("output", options.get("--inter"));
					compileReqs.fulfil("screen", screen);
					SML_Compiler.compile(compileReqs);

					// execute
					executeReqs.fulfil("input", options.get("--inter"));
					executeReqs.fulfil("output", options.get("--output"));
					executeReqs.fulfil("screen", screen);
					SML_Executor.execute(executeReqs);

				} else if (command.equals("exit") || command.equals("")) {
					;
				} else {
					System.out.println("Simulator Error: Unknown command");
				}
			}
		}
		System.out.println("good bye :(");
	}

	private static void printHelpForCommand(String command) {
		if (command.equals("compile"))
			System.out.println("Use this command to compile");

		else if (command.equals("execute"))
			System.out.println("Use this command to execute");

		else if (command.equals("com_exe"))
			System.out.println("Use this command to compile and execute");

		else if (command.equals("exit"))
			System.out.println("Use this command to exit");

		else if (command.equals("help"))
			System.out.println("Use this command to get some help");

		else
			System.out.println("Unknown command: " + command);
	}
}

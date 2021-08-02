package sml_package;

import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import requirement.requirements.Requirements;

public class SML_Simulator {

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

		System.out.println(SML_Simulator.msg);

		String command;

		final Requirements compileReqs = SML_Compiler.getRequirements();
		final Requirements executeReqs = SML_Executor.getRequirements();

		do {
			compileReqs.clear();
			executeReqs.clear();

			System.out.print("=> ");

			final Map<String, String> options = SML_Simulator.getNextCommand();

			command = options.get("command");

			if (command.equals("help"))
				SML_Simulator.printHelpForCommand(options.get("_help_for"));

			else if (command.equals("compile")) {

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

			} else if (command.equals("com_exe")) {

				final boolean screen = options.get("-screen").equals("true");

				compileReqs.fulfil("input", options.get("--input"));
				compileReqs.fulfil("output", options.get("--inter"));
				compileReqs.fulfil("screen", screen);
				SML_Compiler.compile(compileReqs);

				executeReqs.fulfil("input", options.get("--inter"));
				executeReqs.fulfil("output", options.get("--output"));
				executeReqs.fulfil("screen", screen);
				SML_Executor.execute(executeReqs);

			} else if (command.equals("exit") || command.equals("")) {

			} else
				System.out.println("Simulator Error: Unknown command");
		} while (!command.equals("exit"));
	}

	private static Map<String, String> getNextCommand() {

		@SuppressWarnings("resource")
		final Scanner  scanner = new Scanner(System.in);
		final String[] tokens  = scanner.nextLine().split(" ");

		final HashMap<String, String> options = new HashMap<>(8, 1.0f);
		options.put("_command", tokens[0]);
		options.put("_help_for", "");
		options.put("-screen", "false");
		options.put("-manual", "false");
		options.put("-st", "false");
		options.put("--input", "");
		options.put("--output", "");
		options.put("--inter", "");

		if (tokens.length > 1)
			options.put("_help_for", tokens[1]);

		for (int i = 1, size = tokens.length; i < size; ++i) {

			final String key = tokens[i];

			if (!options.containsKey(key))
				System.err.println("Error: invalid option: " + key);
			else if (key.startsWith("--")) {
				++i;
				options.put(key, tokens[i]);
			} else if (key.startsWith("-"))
				options.put(key, "true");
			else
				System.out.println("Error: invalid option: " + key);
		}

		return options;
	}

	private static void printHelpForCommand(String command) {
		if (command.equals(""))
			System.out.println("Use help [command] to get help on a specific command");

		else if (command.equals("compile"))
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

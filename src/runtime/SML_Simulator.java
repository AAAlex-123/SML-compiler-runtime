package runtime;

import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import compiler.SML_Compiler;
import requirement.requirements.AbstractRequirement;
import requirement.requirements.Requirements;

/**
 * A Simulator that executes commands to both compile a high-level-language
 * program to machine code and to execute machine code. It defines the
 * {@code static} method {@link SML_Simulator#run run} which reads commands from
 * standard in, interprets them and then executes them. The Simulator is
 * {@code stateless} meaning that no information is stored between between 'run'
 * sessions and that an instance of a Simulator is not necessary to run.
 * <p>
 * The 'run' process uses {@link requirement.requirements.AbstractRequirement
 * Requirements} in order to specify different parameters. They can be obtained
 * with the {@link SML_Simulator#getRequirements() getRequriements()} method,
 * which contains more information about each individual Requirement.
 *
 * @author Alex Mandelias
 */
public class SML_Simulator {

	/** Informs the user about the different commands available in the Simulator */
	private static final String msg = "\n\nUsage (parameters in [] are optional):\n"
	        + "       help    [command]\n"
	        + "   or  compile [options]\n"
	        + "   or  execute [options]\n"
	        + "   or  com_exe [options]\n"
	        + "\n"
	        + "where <command> is one of:\n"
	        + "  'compile', 'execute', 'com_exe', 'exit', 'help' or blank\n"
	        + "\n"
	        + "and <options> include:\n"
	        + "  --input <filename or 'stdin'>\n"
	        + "      where to input the code for compilation or execution from\n"
	        + "  --output <filename or 'stdout'>\n"
	        + "      where to output the results of compilation or execution\n"
	        + "  -screen\n"
	        + "      output user-friendly results to stdout as well, independent from --output\n"
	        + "  -verbose\n"
	        + "      show all output, not only error messages\n"
	        + "  -st\n"
	        + "      show the Symbol Table at the end of compilation\n"
	        + "\n"
	        + "Compiling with no options is equivalent to:\n"
	        + "  compile --input stdin --output out.sml\n"
	        + "\n"
	        + "Executing with no options is equivalent to:\n"
	        + "  execute --input out.sml --output res.txt\n"
	        + "\n"
	        + "Compiling and Executing with no options is equivalent to:\n"
	        + "  com_exe --input stdin --output res.txt";

	/**
	 * Uses the command line arguments to specify the parameters necessary to
	 * compile and/or execute a program, and then compiles and/or executes it.
	 * Parameters starting with a single dash '-' are set to {@code true}.
	 * Parameters starting with a double dash '--' are set to whatever the next
	 * argument is.
	 * <p>
	 * The different parameters are documented in the
	 * {@link SML_Compiler#getRequirements() getRequirements()} and
	 * {@link SML_Executor#getRequirements() getRequirements()} methods.
	 *
	 * @param args the command line arguments
	 */
	public static void main(String[] args) {

		final Requirements reqs = SML_Simulator.getRequirements();

		for (int i = 0, count = args.length; i < count; ++i)
			if (args[i].startsWith("--"))
				reqs.fulfil(args[i].substring(2), args[++i]);
			else if (args[i].startsWith("-"))
				reqs.fulfil(args[i].substring(1), true);
			else
				SML_Simulator.err(
				        "Invalid parameter: %s. Parameters must start with either one '-' or two '--' dashes.",
				        args[i]);

		SML_Simulator.run(reqs);
	}

	/**
	 * Returns the {@code Requirements} needed for simulation. They have their
	 * default values and can be used as-is for simulation.
	 *
	 * <pre>
	 * | Value   | Default | Explanation         | Command Line |
	 * |---------|---------|---------------------|--------------|
	 * | verbose | false   | output all messages | -            |
	 * </pre>
	 *
	 * @return the Requirements
	 */
	public static Requirements getRequirements() {
		final Requirements reqs = new Requirements();

		reqs.add("verbose");

		reqs.fulfil("verbose", false);

		return reqs;
	}

	/**
	 * Uses the parameters from the {@code requirements} in order to repeatedly
	 * prompt the user for a command to execute, parse it, and then execute it.
	 * <p>
	 * Information about the different commands the user may write can be found in
	 * the {@link SML_Simulator#msg message} which is optionally printed once at the
	 * start of simulation (only with -verbose flag) and may be printed again using
	 * the 'help' command.
	 * <p>
	 * The different Requirements are documented in the
	 * {@link SML_Compiler#getRequirements() getRequirements()} method.
	 *
	 * @param requirements the parameters needed to compile
	 */
	public static void run(Requirements requirements) {

		if (!requirements.fulfilled()) {
			for (final AbstractRequirement r : requirements)
				if (!r.fulfilled())
					SML_Simulator.err("No value for parameter '%s' found", r.key());

			SML_Simulator.err("Simulation couldn't start due to missing parameters");
			return;
		}

		final boolean verboseSimulator = (boolean) requirements.getValue("verbose");

		if (verboseSimulator)
			SML_Simulator.out(SML_Simulator.msg);

		String command = "";

		Requirements compileReqs, executeReqs;
		Map<String, String> options;

		next_command:
		while (!command.equals("exit")) {
			compileReqs = SML_Compiler.getRequirements();
			executeReqs = SML_Executor.getRequirements();

			SML_Simulator.prompt();

			// get options from next command the user types
			options = SML_Simulator.getNextCommand();

			if (options == null)
				continue next_command;

			// put options into variables
			command = options.get("_command");
			final String  input   = options.get("--input");
			final String  output  = options.get("--output");
			final String  inter   = ".inter.sml";
			final boolean screen  = options.get("-screen").equals("true");
			final boolean verbose = options.get("-verbose").equals("true");
			final boolean st      = options.get("-st").equals("true");

			// fulfil compilation requirements
			if (!input.isEmpty())
				compileReqs.fulfil("input", input);
			if (!output.isEmpty())
				compileReqs.fulfil("output", output);
			compileReqs.fulfil("screen", screen);
			compileReqs.fulfil("st", st);
			compileReqs.fulfil("verbose", verbose);

			// fulfil compilation requirements
			if (!input.isEmpty())
				executeReqs.fulfil("input", input);
			if (!output.isEmpty())
				executeReqs.fulfil("output", output);
			executeReqs.fulfil("screen", screen);
			executeReqs.fulfil("verbose", verbose);

			// do stuff according to command
			if (command.equals("help"))
				SML_Simulator.printHelpForCommand(options.get("_help_for"));

			else if (command.equals("compile"))
				SML_Compiler.compile(compileReqs);
			else if (command.equals("execute"))
				SML_Executor.execute(executeReqs);
			else if (command.equals("com_exe")) {

				if (!inter.isEmpty()) {
					compileReqs.fulfil("output", inter);
					executeReqs.fulfil("input", inter);
				}

				SML_Compiler.compile(compileReqs);
				SML_Executor.execute(executeReqs);

			} else if (command.equals("exit") || command.equals("")) {

			} else
				SML_Simulator.err("Unknown command: " + command);
		}
	}

	// --- 3 methods for uniform message printing ---

	private static void prompt() {
		System.out.print("=> ");
		System.out.flush();
	}

	private static void out(String text, Object... args) {
		System.out.printf("Simulator Info: %s%n", String.format(text, args));
		System.out.flush();
	}

	private static void err(String text, Object... args) {
		System.err.printf("Simulator Error: %s%n", String.format(text, args));
		System.err.flush();
	}

	// --- 2 helper methods ---

	private static Map<String, String> getNextCommand() {

		boolean valid = true;

		@SuppressWarnings("resource")
		final Scanner  scanner = new Scanner(System.in);
		final String[] tokens  = scanner.nextLine().split(" ");

		final Map<String, String> options = new HashMap<>(6, 1.0f);
		options.put("_command", "");
		options.put("_help_for", "");
		options.put("--input", "");
		options.put("--output", "");
		options.put("-screen", "");
		options.put("-verbose", "");
		options.put("-st", "");

		options.put("_command", tokens[0]);

		if (tokens[0].equals("help")) {
			if (tokens.length > 1)
				options.put("_help_for", tokens[1]);
			return options;
		}

		for (int i = 1, size = tokens.length; i < size; ++i) {

			final String key = tokens[i];

			if (!options.containsKey(key)) {
				SML_Simulator.err("Unknown option: %s", key);
				valid = false;

			} else if (key.startsWith("--")) {
				options.put(key, tokens[++i]);

			} else if (key.startsWith("-")) {
				options.put(key, "true");

			} else {
				SML_Simulator.err(
						"Invalid option: %s. Option must start with either one '-' or two '--' dashes.",
						key);
				valid = false;
			}
		}

		return valid ? options : null;
	}

	private static void printHelpForCommand(String command) {
		if (command.equals(""))
			SML_Simulator.out(SML_Simulator.msg);

		else if (command.equals("compile"))
			SML_Simulator.out("Use this command to compile\n"
			        + "  Usage: compile [--input <filename or 'stdin'>] [--output <filename or 'stdout'>] [-screen] [-verbose] [-st]\n"
			        + "  No options is equivalent to: compile --input stdin --output out.sml");

		else if (command.equals("execute"))
			SML_Simulator.out("Use this command to execute\n"
			        + "  Usage: execute [--input <filename or 'stdin'>] [--output <filename or 'stdout'>] [-screen] [-verbose] \n"
			        + "  No options is equivalent to: execute --input out.sml --output res.txt");

		else if (command.equals("com_exe"))
			SML_Simulator.out("Use this command to compile and execute\n"
			        + "  Usage: com_exe [--input <filename or 'stdin'>] [--output <filename or 'stdout'>] [-screen] [-verbose] [-st]\n"
			        + "  No options is equivalent to: com_exe --input stdin --output res.txt");

		else if (command.equals("exit"))
			SML_Simulator.out("Use this command to exit the simulator\n"
			        + "  Usage: exit");

		else if (command.equals("help"))
			SML_Simulator.out(
			        "Use this command to get help for a specific command or for the simulator as a whole\n"
			                + "  Usage: help [compile|execute|com_exe|exit|help]");

		else
			SML_Simulator.err("Unknown command: " + command);
	}
}

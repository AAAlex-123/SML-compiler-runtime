package sml_package;

import java.io.IOException;
import java.util.HashMap;
import java.util.Scanner;

public class SML_Simulator {
	static final Scanner scanner = new Scanner(System.in);

	static final String SimpleFile = "C:\\Users\\Danae\\Desktop\\projects\\Java\\SML_new\\Simple.txt";
	static final String SMLFile = "C:\\Users\\Danae\\Desktop\\projects\\Java\\SML_new\\SML.txt";
	static final String OutputFile = "C:\\Users\\Danae\\Desktop\\projects\\Java\\SML_new\\out.txt";
//	static final String SimpleFile = "C:\\Users\\JIM\\Desktop\\java stuff\\SML\\Simple.txt";
//	static final String SMLFile = "C:\\Users\\JIM\\Desktop\\java stuff\\SML\\SML.txt";
//	static final String OutputFile = "C:\\Users\\JIM\\Desktop\\java stuff\\SML\\out.txt";

	static final String msg = 
			"Usage:\n" +
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
			"       only used at com_exe, the file where SML stored after compilation and read for execution\n" +
            "\n" +
			"where command is one of:\n" +
			"   compile, execute, com_exe, exit, help\n" + 
			"compile --input src.txt -screen\n\n";

	static String user_input, command="", helpCommand, sourceFile, machineFile, outputFile;
	static String[] tokens;
	
	static void zzz() {while(true) {;}}
	
	static void dummy(Class<?> c) {
		System.out.println(c);
	}
	
	public static void main(String[] args) throws IOException {
		
		
		
		System.out.println(msg);
//		System.out.println(System.getProperty("user.dir"));
		
		while (!command.equals("exit")) {
			System.out.print("=> ");
//			"com_exe --input Simple.txt --inter SML.txt --output out.txt -screen";
			user_input = scanner.nextLine();
			tokens = user_input.split(" ");
			
			command = tokens[0];
			HashMap<String, String> options = new HashMap<String, String>(5, 1.0f);
			options.put("-screen", "false");
			options.put("-manual", "false");
			options.put("-st", "false");
			options.put("-d", "false");
			options.put("--input", "");
			options.put("--output", "");
			options.put("--inter", "");
			
			if (command.equals("help")) {
				try {
					helpCommand = tokens[1];
					if (helpCommand.equals("compile"))
						System.out.println("Use this command to compile");
					
					else if (helpCommand.equals("execute"))
						System.out.println("Use this command to execute");
					
					else if (helpCommand.equals("com_exe"))
						System.out.println("Use this command to compile and execute");

					else if (helpCommand.equals("exit"))
						System.out.println("Use this command to exit");
					
					else if (helpCommand.equals("help"))
						System.out.println("Use this command to get some help");
					
					else if (helpCommand.equals("owo"))
						System.out.println("uwu");
					
					else System.out.println("Unknown command");

				} catch (IndexOutOfBoundsException ioob) {
					System.out.println("Use help [command] to get help on a specific command");
				}

			} else {
				int i=1;
				while (i < tokens.length) {
					if (tokens[i].startsWith("--")) options.put(tokens[i], tokens[++i]);
					else if (tokens[i].startsWith("-")) options.put(tokens[i], "true");
					else System.out.println("Error: invalid option: " + tokens[i]);
					i++;
				}
				
				if (command.equals("compile")) {											// COMPILE
					int result = SML_Compiler.compile(options);
					if (result == 1)
						System.out.println("\nDue to above errors compilation failed :(\n");
					else 
						System.out.println("\nCompilation successful :)\n");
					
				} else if (command.equals("execute")) {										// EXECUTE
					int result = SML_Executor.execute(options);
					if (result == 1)
						System.out.println("\nDue to above errors execution failed :(\n");
					else 
						System.out.println("\nExecution successful :)\n");
					
				} else if (command.equals("com_exe")) {										// COM_EXE
					// compile
					String out = options.get("--output");
					options.put("--output", options.get("--inter"));
	
					int result1 = SML_Compiler.compile(options);
					if (result1 == 1)
						System.out.println("\nDue to above errors compilation failed :(\n");
					else 
						System.out.println("\nCompilation successful :)\n");
	
					// execute
					options.put("-manual", "false");
					options.put("--input", options.get("--output"));
					options.put("--output", out);
					int result2 = SML_Executor.execute(options);
					if (result2 == 1)
						System.out.println("\nDue to above errors execution failed :(\n");
					else 
						System.out.println("\nExecution successful :)\n");
					
				} else if (command.equals("exit")) {										// EXIT
					;
					
				} else if (command.equals("dump")) {										// DUMP
					System.out.println(options);
					System.out.println(SML_Compiler.symbolTable);
					SML_Executor.writeToScreen();
				} else if (command.equals("")) {
					;
				} else {
					System.out.println("Simulator Error: Unknown command");					// UNKNOWN
				}
			}
		}
		System.out.println("good bye :(");
	}
}

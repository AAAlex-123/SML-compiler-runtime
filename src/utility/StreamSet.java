package utility;

import java.io.InputStream;
import java.io.PrintStream;

/**
 * Set of Streams that is used by the Compiler and the Executor to change their
 * Input and redirect their Output and Error streams to the desired Streams.
 * <p>
 * Usage example (without exception handling):
 *
 * <pre>
 * StreamSet set = new StreamSet()
 *         .in(System.in)
 *         .err(new PrintStream(new File("log.txt")));
 * </pre>
 *
 * @author Alex Mandelias
 */
public final class StreamSet {

	/** The Input Stream */
	public InputStream in;

	/** The Output Stream */
	public PrintStream out;

	/** The Error Stream */
	public PrintStream err;

	/** Constructs a Stream Set with the "standard" in, out and err streams */
	public StreamSet() {
		in = System.in;
		out = System.out;
		err = System.err;
	}

	/**
	 * Sets this StreamSet's Input Stream.
	 *
	 * @param inputStream the new Input Stream
	 *
	 * @return this StreamSet
	 */
	public StreamSet in(InputStream inputStream) {
		in = inputStream;
		return this;
	}

	/**
	 * Sets this StreamSet's Output Stream.
	 *
	 * @param outputStream the new Output Stream
	 *
	 * @return this StreamSet
	 */
	public StreamSet out(PrintStream outputStream) {
		out = outputStream;
		return this;
	}

	/**
	 * Sets this StreamSet's Error Stream.
	 *
	 * @param errorStream the new Error Stream
	 *
	 * @return this StreamSet
	 */
	public StreamSet err(PrintStream errorStream) {
		err = errorStream;
		return this;
	}
}

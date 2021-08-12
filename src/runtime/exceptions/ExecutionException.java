package runtime.exceptions;

public abstract class ExecutionException extends Exception {

	public ExecutionException(String text, Object... args) {
		super(String.format(text, args));
	}
}

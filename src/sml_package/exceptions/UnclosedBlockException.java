package sml_package.exceptions;

public class UnclosedBlockException extends Exception {
	public UnclosedBlockException() {
		super("close your goddamn blocks");
	}
}

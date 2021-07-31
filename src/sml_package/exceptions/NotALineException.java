package sml_package.exceptions;

public class NotALineException extends Exception {
	public NotALineException(String name) {
		super(String.format("'%s' is not a line", name));
	}
}

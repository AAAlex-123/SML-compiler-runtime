/**
 * Defines the checked Exceptions that may occur during compilation. All of them
 * are a subclass of the {@link compiler.exceptions.CompilerException
 * CompilerException}. Exceptions are used (instead of simple error messages) to
 * allow for uniform handling of errors during parsing and to allow for multiple
 * error to be displayed during compilation, since compilation doesn't stop
 * after the first error.
 *
 * @author Alex Mandelias
 */
package compiler.exceptions;

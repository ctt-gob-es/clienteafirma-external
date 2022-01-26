package com.aowagie.text.exceptions;

/**
 * Typed exception used when trying to access to a incorrect page number.
 */
public class InvalidPageNumberException extends IllegalArgumentException {

	/**
	 * A serial UID version
	 */
	private static final long serialVersionUID = -2367234922874752908L;

	/**
	 * Creates an instance of a InvalidPageNumberException.
	 * @param	message	the reason why the page is incorrect.
	 */
	public InvalidPageNumberException(final String message) {
		super(message);
	}
}

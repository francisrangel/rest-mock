package org.simulatest.restmock.internal.response;

/**
 * A {@code ${name}} that nothing in the request answered. It has a type of its
 * own so that whoever knows more about the request can add it to the message.
 */
public final class UnresolvedPlaceholderException extends IllegalStateException {

	private static final long serialVersionUID = 1L;

	UnresolvedPlaceholderException(String message) {
		super(message);
	}

	private UnresolvedPlaceholderException(String message, UnresolvedPlaceholderException cause) {
		super(message, cause);
	}

	/** The same failure, with a reason the name may be missing added to what it says. */
	public UnresolvedPlaceholderException because(String reason) {
		return new UnresolvedPlaceholderException(getMessage() + ". " + reason, this);
	}

}

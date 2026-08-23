package org.simulatest.restmock.internal.utils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

/**
 * Loads response fixtures from the test classpath.
 *
 * Failures are unchecked. A fixture that is missing or unreadable is a mistake
 * in the test, not a condition worth writing a catch block for, and forcing
 * {@code throws Exception} onto every stubbing line contradicts the point of
 * the library. It also matches how the rest of the API reports IO trouble.
 */
public final class Resource {

	private Resource() {}

	public static String dataFromResource(String resource) {
		return new String(bytesFromResource(resource), StandardCharsets.UTF_8).strip();
	}

	public static byte[] bytesFromResource(String resource) {
		try (InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(resource)) {
			if (is == null) {
				throw new UncheckedIOException(
					new FileNotFoundException(resource + " was not found at resources folder!"));
			}
			return is.readAllBytes();
		} catch (IOException unreadable) {
			throw new UncheckedIOException("Could not read " + resource, unreadable);
		}
	}

}

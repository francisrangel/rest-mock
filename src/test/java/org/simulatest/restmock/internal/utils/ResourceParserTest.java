package org.simulatest.restmock.internal.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.FileNotFoundException;
import java.io.UncheckedIOException;

import org.junit.jupiter.api.Test;

public class ResourceParserTest {

	@Test
	public void simpleParseSample() {
		String expected = "rest-mock rock! :-)";
		assertEquals(expected, Resource.dataFromResource("example.txt"));
	}

	/** A missing fixture is a test bug, so it fails unchecked rather than forcing a catch. */
	@Test
	public void unexistentFile() {
		UncheckedIOException failure =
			assertThrows(UncheckedIOException.class, () -> Resource.dataFromResource("unexistent.file"));

		assertInstanceOf(FileNotFoundException.class, failure.getCause());
	}

}

package org.simulatest.restmock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Locale;

import org.junit.jupiter.api.Test;

public class HttpMethodTest {

	@Test
	public void byStringResolvesEveryMethodIgnoringCase() {
		for (HttpMethod method : HttpMethod.values()) {
			assertEquals(method, HttpMethod.byString(method.name()));
			assertEquals(method, HttpMethod.byString(method.name().toLowerCase(Locale.ROOT)));
		}
	}

	@Test
	public void byStringRejectsAnUnknownMethodName() {
		assertThrows(IllegalArgumentException.class, () -> HttpMethod.byString("foo"));
	}

}

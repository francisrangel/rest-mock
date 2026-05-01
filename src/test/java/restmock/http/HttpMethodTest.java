package restmock.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

public class HttpMethodTest {

	@Test
	public void byString() {
		assertEquals(HttpMethod.GET, HttpMethod.byString("get"));
		assertEquals(HttpMethod.GET, HttpMethod.byString("GET"));

		assertEquals(HttpMethod.POST, HttpMethod.byString("post"));
		assertEquals(HttpMethod.POST, HttpMethod.byString("POST"));

		assertEquals(HttpMethod.PUT, HttpMethod.byString("put"));
		assertEquals(HttpMethod.PUT, HttpMethod.byString("PUT"));

		assertEquals(HttpMethod.DELETE, HttpMethod.byString("delete"));
		assertEquals(HttpMethod.DELETE, HttpMethod.byString("DELETE"));

		assertEquals(HttpMethod.PATCH, HttpMethod.byString("patch"));
		assertEquals(HttpMethod.PATCH, HttpMethod.byString("PATCH"));

		assertEquals(HttpMethod.HEAD, HttpMethod.byString("head"));
		assertEquals(HttpMethod.HEAD, HttpMethod.byString("HEAD"));

		assertEquals(HttpMethod.OPTIONS, HttpMethod.byString("options"));
		assertEquals(HttpMethod.OPTIONS, HttpMethod.byString("OPTIONS"));
	}

	@Test
	public void invalidMethod() {
		assertThrows(IllegalArgumentException.class, () -> HttpMethod.byString("foo"));
	}

}

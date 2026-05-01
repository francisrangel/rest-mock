package restmock.http;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

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

	@Test(expected = IllegalArgumentException.class)
	public void invalidMethod() {
		HttpMethod.byString("foo");
	}

}

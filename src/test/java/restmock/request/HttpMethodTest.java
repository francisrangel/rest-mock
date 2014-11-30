package restmock.request;

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
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void invalidMethod() {
		HttpMethod.byString("foo");
	}

}

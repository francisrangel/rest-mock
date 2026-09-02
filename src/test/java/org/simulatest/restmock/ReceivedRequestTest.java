package org.simulatest.restmock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

public class ReceivedRequestTest {

	private ReceivedRequest requestWith(Map<String, List<String>> headers) {
		return new ReceivedRequest(HttpMethod.GET, "/users", null, headers, "", Instant.now());
	}

	@Test
	public void headersAreCopiedSoLaterChangesToTheSourceAreNotVisible() {
		Map<String, List<String>> source = new HashMap<>();
		source.put("Content-Type", List.of("application/json"));

		ReceivedRequest request = requestWith(source);
		source.put("X-Added-Later", List.of("nope"));

		assertEquals(1, request.headers().size());
		assertEquals("application/json", request.headers().get("Content-Type").get(0));
		assertNull(request.headers().get("X-Added-Later"));
	}

	@Test
	public void headersCannotBeModifiedByCallers() {
		ReceivedRequest request = requestWith(new HashMap<>());

		assertThrows(UnsupportedOperationException.class,
			() -> request.headers().put("X-Trace", List.of("abc")));
	}

	@Test
	public void headersMayBeEmpty() {
		assertTrue(requestWith(new HashMap<>()).headers().isEmpty());
	}

	private ReceivedRequest requestWithQuery(String query) {
		return new ReceivedRequest(HttpMethod.GET, "/users", query, Map.of(), "", Instant.now());
	}

	/**
	 * The JDK server stores a header sent as Content-Type under "Content-type",
	 * so an exact lookup for the name the client sent finds nothing. HTTP header
	 * names are case-insensitive and this accessor has to be too.
	 */
	@Test
	public void headersAreFoundWhateverTheServerDidToTheirCase() {
		ReceivedRequest request = requestWith(Map.of("Content-type", List.of("application/json")));

		assertEquals("application/json", request.header("Content-Type").orElseThrow());
		assertEquals("application/json", request.header("content-type").orElseThrow());
		assertEquals("application/json", request.header("CONTENT-TYPE").orElseThrow());
	}

	/** The map is case-insensitive too, so nobody has to know how the server spelled a name. */
	@Test
	public void theHeadersMapIsCaseInsensitive() {
		ReceivedRequest request = requestWith(Map.of("Content-type", List.of("application/json")));

		assertEquals(List.of("application/json"), request.headers().get("Content-Type"));
		assertEquals(List.of("application/json"), request.headers().get("CONTENT-TYPE"));
	}

	@Test
	public void aHeaderThatWasNotSentIsEmptyRatherThanNull() {
		assertTrue(requestWith(Map.of()).header("X-Tenant").isEmpty());
	}

	@Test
	public void aRepeatedHeaderExposesItsFirstValueAndAllOfThem() {
		ReceivedRequest request = requestWith(Map.of("Accept", List.of("text/html", "application/json")));

		assertEquals("text/html", request.header("accept").orElseThrow());
		assertEquals(List.of("text/html", "application/json"), request.headerValues("ACCEPT"));
	}

	@Test
	public void queryParametersAreDecoded() {
		ReceivedRequest request = requestWithQuery("name=Bob+Smith&city=S%C3%A3o+Paulo");

		assertEquals("Bob Smith", request.queryParam("name").orElseThrow());
		assertEquals("São Paulo", request.queryParam("city").orElseThrow());
	}

	/** Query parameter names, unlike header names, are case-sensitive. */
	@Test
	public void queryParametersAreMatchedExactly() {
		ReceivedRequest request = requestWithQuery("id=42");

		assertEquals("42", request.queryParam("id").orElseThrow());
		assertTrue(request.queryParam("ID").isEmpty());
	}

	@Test
	public void aRepeatedQueryParameterExposesItsFirstValueAndAllOfThem() {
		ReceivedRequest request = requestWithQuery("tag=a&tag=b");

		assertEquals("a", request.queryParam("tag").orElseThrow());
		assertEquals(List.of("a", "b"), request.queryParamValues("tag"));
	}

	@Test
	public void aRequestWithNoQueryStringHasNoQueryParameters() {
		assertTrue(requestWithQuery(null).queryParam("id").isEmpty());
		assertTrue(requestWithQuery(null).queryParamValues("id").isEmpty());
	}

}

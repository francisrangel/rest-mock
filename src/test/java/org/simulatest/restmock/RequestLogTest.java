package org.simulatest.restmock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import java.lang.reflect.Modifier;

import org.junit.jupiter.api.Test;

public class RequestLogTest {

	private RequestLog log;

	@BeforeEach
	public void setUp() {
		log = new RequestLog();
	}

	private ReceivedRequest request(HttpMethod method, String path) {
		return new ReceivedRequest(method, path, null, Map.of(), "", Instant.now());
	}

	private ReceivedRequest request(HttpMethod method, String path, String query, String body) {
		return new ReceivedRequest(method, path, query, Map.of(), body, Instant.now());
	}

	@Test
	public void emptyByDefault() {
		assertTrue(log.isEmpty());
		assertEquals(0, log.count());
		assertTrue(log.all().isEmpty());
		assertTrue(log.last().isEmpty());
	}

	@Test
	public void capturesSingleRequest() {
		log.add(request(HttpMethod.GET, "/users"));

		assertFalse(log.isEmpty());
		assertEquals(1, log.count());
		assertEquals("/users", log.all().get(0).path());
	}

	@Test
	public void allReturnsInInsertionOrder() {
		log.add(request(HttpMethod.GET, "/first"));
		log.add(request(HttpMethod.GET, "/second"));
		log.add(request(HttpMethod.GET, "/third"));

		assertEquals(3, log.count());
		assertEquals("/first", log.all().get(0).path());
		assertEquals("/third", log.all().get(2).path());
	}

	@Test
	public void forPathMatchesLiterally() {
		log.add(request(HttpMethod.GET, "/users"));
		log.add(request(HttpMethod.GET, "/users/42"));
		log.add(request(HttpMethod.GET, "/users"));

		assertEquals(2, log.forPath("/users").size());
		assertEquals(1, log.forPath("/users/42").size());
		assertEquals(0, log.forPath("/posts").size());
	}

	@Test
	public void forMethodFiltersByVerb() {
		log.add(request(HttpMethod.GET, "/a"));
		log.add(request(HttpMethod.POST, "/b"));
		log.add(request(HttpMethod.GET, "/c"));

		assertEquals(2, log.forMethod(HttpMethod.GET).size());
		assertEquals(1, log.forMethod(HttpMethod.POST).size());
		assertEquals(0, log.forMethod(HttpMethod.DELETE).size());
	}

	@Test
	public void forRouteMatchesMethodAndPath() {
		log.add(request(HttpMethod.GET, "/users"));
		log.add(request(HttpMethod.POST, "/users"));
		log.add(request(HttpMethod.GET, "/users"));
		log.add(request(HttpMethod.GET, "/posts"));

		assertEquals(2, log.forRoute(HttpMethod.GET, "/users").size());
		assertEquals(1, log.forRoute(HttpMethod.POST, "/users").size());
		assertEquals(0, log.forRoute(HttpMethod.DELETE, "/users").size());
	}

	@Test
	public void countForPathMatchesLiterally() {
		log.add(request(HttpMethod.GET, "/a"));
		log.add(request(HttpMethod.GET, "/b"));
		log.add(request(HttpMethod.GET, "/a"));

		assertEquals(2, log.countForPath("/a"));
		assertEquals(1, log.countForPath("/b"));
		assertEquals(0, log.countForPath("/c"));
	}

	@Test
	public void countForRouteMatchesMethodAndPath() {
		log.add(request(HttpMethod.GET, "/a"));
		log.add(request(HttpMethod.POST, "/a"));

		assertEquals(1, log.countForRoute(HttpMethod.GET, "/a"));
		assertEquals(1, log.countForRoute(HttpMethod.POST, "/a"));
		assertEquals(0, log.countForRoute(HttpMethod.PUT, "/a"));
	}

	@Test
	public void lastReturnsLastAdded() {
		log.add(request(HttpMethod.GET, "/first"));
		log.add(request(HttpMethod.GET, "/second"));

		assertEquals("/second", log.last().orElseThrow().path());
	}

	@Test
	public void lastForPathReturnsTheMostRecentMatch() {
		log.add(request(HttpMethod.GET, "/users", "page=1", ""));
		log.add(request(HttpMethod.GET, "/posts", null, ""));
		log.add(request(HttpMethod.GET, "/users", "page=2", ""));

		assertEquals("page=2", log.lastForPath("/users").orElseThrow().query());
		assertTrue(log.lastForPath("/missing").isEmpty());
	}

	@Test
	public void clearRemovesEverything() {
		log.add(request(HttpMethod.GET, "/a"));
		log.add(request(HttpMethod.GET, "/b"));

		log.clear();

		assertTrue(log.isEmpty());
		assertEquals(0, log.count());
		assertTrue(log.last().isEmpty());
	}

	@Test
	public void bodyAndQueryArePreserved() {
		log.add(request(HttpMethod.POST, "/api", "key=val", "{\"name\":\"Bob\"}"));

		ReceivedRequest req = log.last().orElseThrow();

		assertEquals("key=val", req.query());
		assertEquals("{\"name\":\"Bob\"}", req.body());
	}

	/**
	 * Recording is the server's job. When these were public, user code could
	 * forge entries into the log every assertion here reads from.
	 */
	@Test
	public void recordingAndClearingAreNotPublicApi() throws Exception {
		assertFalse(Modifier.isPublic(RequestLog.class.getDeclaredMethod("add", ReceivedRequest.class).getModifiers()),
			"RequestLog.add must not be public");
		assertFalse(Modifier.isPublic(RequestLog.class.getDeclaredMethod("clear").getModifiers()),
			"RequestLog.clear must not be public");
	}

}
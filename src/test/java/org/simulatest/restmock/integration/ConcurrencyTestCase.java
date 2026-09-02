package org.simulatest.restmock.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.http.HttpResponse;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

import org.simulatest.restmock.HttpMethod;
import org.simulatest.restmock.RestMock;

public class ConcurrencyTestCase extends IntegrationTestBase {

	private static final long DELAY = 2000;

	/**
	 * The regression this guards: with the JDK server's default executor every
	 * exchange ran on one thread, so a delayed route stalled the whole server
	 * and "client times out on the slow endpoint but not the fast one" was
	 * impossible to test.
	 */
	@Test
	public void aDelayedRouteDoesNotBlockOtherRoutes() throws Exception {
		RestMock.whenGet("/slow").thenReturnText("slow").withDelay(DELAY);
		RestMock.whenGet("/fast").thenReturnText("fast");

		Thread slowCaller = new Thread(() -> sendQuietly("/slow"));
		slowCaller.start();
		awaitRequestRecorded("/slow");

		long start = System.currentTimeMillis();
		HttpResponse<String> fast = sendRequest("/fast", HttpMethod.GET);
		long elapsed = System.currentTimeMillis() - start;

		assertEquals("fast", fast.body());
		assertTrue(elapsed < DELAY / 2,
			"/fast waited " + elapsed + "ms behind the delayed route; the server is still serialized");

		slowCaller.join();
	}

	@Test
	public void concurrentRequestsToTheSameRouteAllSucceed() throws Exception {
		RestMock.whenGet("/users/{id}").thenReturnText("user ${id}");

		ExecutorService pool = Executors.newFixedThreadPool(8);
		try {
			List<Callable<String>> calls = IntStream.range(0, 50)
				.<Callable<String>>mapToObj(i -> () -> sendRequest("/users/" + i, HttpMethod.GET).body())
				.toList();

			List<Future<String>> results = pool.invokeAll(calls);

			for (int i = 0; i < results.size(); i++) {
				assertEquals("user " + i, results.get(i).get(), "response " + i + " was crossed with another request");
			}
		} finally {
			pool.shutdownNow();
		}

		assertEquals(50, RestMock.requests().count());
	}

	/** Registering routes while requests are in flight must not corrupt the route table. */
	@Test
	public void routesCanBeRegisteredWhileRequestsAreInFlight() throws Exception {
		RestMock.whenGet("/steady").thenReturnText("steady");

		ExecutorService pool = Executors.newSingleThreadExecutor();
		try {
			Future<?> hammer = pool.submit(() -> {
				for (int i = 0; i < 300; i++) {
					assertEquals("steady", sendQuietly("/steady"));
				}
			});

			for (int i = 0; i < 300; i++) RestMock.whenGet("/noise/" + i).thenReturnText("noise");

			hammer.get();
		} finally {
			pool.shutdownNow();
		}
	}

	private String sendQuietly(String path) {
		try {
			return sendRequest(path, HttpMethod.GET).body();
		} catch (Exception e) {
			throw new IllegalStateException("request to " + path + " failed", e);
		}
	}

	/**
	 * The front controller records a request before it applies the route's delay,
	 * so this returns as soon as the slow handler is actually sleeping. Polling
	 * beats a fixed sleep: no wall-clock guess, no flakiness on a loaded machine.
	 */
	private void awaitRequestRecorded(String path) throws InterruptedException {
		long deadline = System.currentTimeMillis() + DELAY;
		while (RestMock.requests().countForPath(path) == 0) {
			if (System.currentTimeMillis() > deadline) throw new IllegalStateException("no request recorded for " + path);
			Thread.sleep(5);
		}
	}

}

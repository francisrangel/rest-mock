package org.simulatest.restmock.integration;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.simulatest.restmock.HttpMethod;
import org.simulatest.restmock.RestMock;
import org.simulatest.restmock.internal.http.HttpHeader;

/**
 * Guards the two README values the server derives rather than states.
 *
 * The Allow header comes from the order methods are declared in HttpMethod, and
 * the preflight's Allow-Methods includes HEAD because HEAD is answered wherever
 * GET is. Both had drifted: the README promised "GET, HEAD, DELETE, OPTIONS"
 * where the server sends "GET, DELETE, HEAD, OPTIONS", and "GET, OPTIONS" where
 * it sends "GET, HEAD, OPTIONS". Nothing failed, because prose cannot run.
 *
 * These cases fail the build if the server and the README disagree again, and
 * say what the server actually sends so the fix is a copy and paste.
 */
public class ReadmeClaimsTestCase extends IntegrationTestBase {

	private static final List<String> README = readReadme();

	@Test
	public void theDocumentedAllowHeaderIsTheOneTheServerSends() throws Exception {
		RestMock.whenGet("/users/1").thenReturnJSON("{\"name\":\"Bob\"}");
		RestMock.whenDelete("/users/1").thenReturnText("gone");

		String allow = sendRequest("/users/1", HttpMethod.OPTIONS)
			.headers().firstValue(HttpHeader.ALLOW).orElseThrow();

		assertDocumented("Allow: " + allow, "the OPTIONS Allow header");
	}

	@Test
	public void theDocumentedPreflightMethodsAreTheOnesTheServerSends() throws Exception {
		RestMock.whenGet("/api/data").thenReturnJSON("{\"ok\":true}");

		HttpResponse<String> preflight = send(request("/api/data")
			.header(HttpHeader.ORIGIN, "http://localhost:3000")
			.header(HttpHeader.ACCESS_CONTROL_REQUEST_METHOD, "GET")
			.method(HttpMethod.OPTIONS.name(), HttpRequest.BodyPublishers.noBody()));

		String methods = preflight.headers()
			.firstValue(HttpHeader.ACCESS_CONTROL_ALLOW_METHODS).orElseThrow();

		assertDocumented(HttpHeader.ACCESS_CONTROL_ALLOW_METHODS + ": " + methods, "the preflight methods");
	}

	private static void assertDocumented(String expectedLine, String what) {
		assertTrue(README.stream().anyMatch(line -> line.contains(expectedLine)),
			"README.md does not document " + what + " the server actually sends.\n"
				+ "  expected to find a line containing: " + expectedLine + "\n"
				+ "  update README.md to match, or change the server deliberately");
	}

	private static List<String> readReadme() {
		Path readme = Path.of("README.md");

		try {
			return Files.readAllLines(readme);
		} catch (IOException e) {
			throw new UncheckedIOException("Could not read " + readme.toAbsolutePath(), e);
		}
	}

}

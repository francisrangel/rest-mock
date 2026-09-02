package org.simulatest.restmock.integration;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/** One HTTP client for every integration test, including the ones that manage their own server. */
final class TestHttp {

	static final HttpClient CLIENT = HttpClient.newBuilder()
		.connectTimeout(Duration.ofSeconds(5))
		.build();

	private TestHttp() { }

	static HttpResponse<String> send(HttpRequest.Builder request) throws Exception {
		return CLIENT.send(request.build(), HttpResponse.BodyHandlers.ofString());
	}

	/** A GET of a full URL, for tests whose point is which address the server answers on. */
	static HttpResponse<String> get(String url) throws Exception {
		return send(HttpRequest.newBuilder().uri(URI.create(url)).GET());
	}

}

package org.simulatest.restmock;

import java.time.Duration;
import java.util.Objects;
import java.util.function.BiConsumer;

import org.simulatest.restmock.internal.response.Binary;
import org.simulatest.restmock.internal.response.ContentType;
import org.simulatest.restmock.internal.response.Html;
import org.simulatest.restmock.internal.response.JSON;
import org.simulatest.restmock.internal.response.NotConfigured;
import org.simulatest.restmock.internal.response.Response;
import org.simulatest.restmock.internal.response.TextPlain;
import org.simulatest.restmock.internal.response.XML;
import org.simulatest.restmock.internal.routing.Route;
import org.simulatest.restmock.internal.routing.RouteManager;
import org.simulatest.restmock.internal.utils.Resource;

/**
 * Configures the responses for a stubbed route.
 *
 * Every {@code thenReturn*} sets a body and content type and returns this, so
 * {@link #withStatus}, {@link #withHeader} and {@link #withDelay} can follow
 * and apply to that response. A further {@code thenReturn*} queues the next
 * response for the same route: the route serves them in order and repeats the
 * last, which is how a test says "the upstream failed once".
 *
 * Instances come from the library; there is no public constructor.
 */
public class ResponseOptions implements RestMockResponse {

	private final Route route;
	private final RouteManager routeManager;
	private Response current;

	/** Registers a placeholder for {@code route} that the first {@code thenReturn*} replaces. */
	ResponseOptions(Route route, RouteManager routeManager) {
		this.route = route;
		this.routeManager = routeManager;
		this.current = new NotConfigured(route.getUri());
		routeManager.registerRoute(route, current);
	}

	/** A builder that registers nothing: {@link Answer} reads what it built through {@link #current()}. */
	ResponseOptions(Route route) {
		this.route = route;
		this.routeManager = null;
		this.current = new NotConfigured(route.getUri());
	}

	@Override
	public ResponseOptions thenReturnXML(Object object) {
		return register(new XML(object));
	}

	@Override
	public ResponseOptions thenReturnXML(String value) {
		return register(new XML(value));
	}

	@Override
	public ResponseOptions thenReturnXMLFromResource(String path) {
		return thenReturnXML(Resource.dataFromResource(path));
	}

	@Override
	public ResponseOptions thenReturnHTML(String value) {
		return register(new Html(value));
	}

	@Override
	public ResponseOptions thenReturnHTMLFromResource(String path) {
		return thenReturnHTML(Resource.dataFromResource(path));
	}

	@Override
	public ResponseOptions thenReturnText(String value) {
		return register(new TextPlain(value));
	}

	@Override
	public ResponseOptions thenReturnTextFromResource(String path) {
		return thenReturnText(Resource.dataFromResource(path));
	}

	@Override
	public ResponseOptions thenReturnJSON(String value) {
		return register(new JSON(value));
	}

	@Override
	public ResponseOptions thenReturnJSON(Object object) {
		return register(new JSON(object));
	}

	@Override
	public ResponseOptions thenReturnJSONFromResource(String path) {
		return thenReturnJSON(Resource.dataFromResource(path));
	}

	@Override
	public ResponseOptions thenReturnFile(byte[] bytes) {
		return register(new Binary(bytes, ContentType.APPLICATION_OCTET_STREAM));
	}

	@Override
	public ResponseOptions thenReturnFile(byte[] bytes, String contentType) {
		return register(new Binary(bytes, new ContentType(contentType)));
	}

	@Override
	public ResponseOptions thenReturnFileFromResource(String path) {
		return register(new Binary(Resource.bytesFromResource(path), ContentType.guessFrom(path)));
	}

	@Override
	public ResponseOptions thenReturnFileFromResource(String path, String contentType) {
		return thenReturnFile(Resource.bytesFromResource(path), contentType);
	}

	@Override
	public ResponseOptions thenReturnErrorCodeWithMessage(int errorCode, String message) {
		return thenReturnText(message).withStatus(errorCode);
	}

	@Override
	public void thenAnswer(BiConsumer<ReceivedRequest, RestMockResponse> answer) {
		register(new Answer(route, Objects.requireNonNull(answer, "answer")));
	}

	/** Sets a response header. Replaces any previous value for the same header name. */
	public ResponseOptions withHeader(String name, String value) {
		current.addHeader(name, value);
		return this;
	}

	/** Overrides the HTTP status code. Defaults to 200, or to the code passed to {@code thenReturnErrorCodeWithMessage}. Last call wins. */
	public ResponseOptions withStatus(int status) {
		current.setResponseStatus(status);
		return this;
	}

	/**
	 * Sleeps for {@code millis} before sending the response. Useful for simulating
	 * slow upstreams. The delay applies to this route only: requests to other
	 * routes are served concurrently and are not held up. Last call wins.
	 */
	public ResponseOptions withDelay(long millis) {
		if (millis < 0) throw new IllegalArgumentException("Delay must not be negative, but was " + millis + " ms.");

		current.setDelayMillis(millis);
		return this;
	}

	/**
	 * The same delay, said out loud: {@code withDelay(Duration.ofSeconds(2))}
	 * cannot be misread the way {@code withDelay(2000)} can.
	 */
	public ResponseOptions withDelay(Duration delay) {
		return withDelay(Objects.requireNonNull(delay, "delay").toMillis());
	}

	/** The response the last {@code thenReturn*} built, or the placeholder if none was called. */
	Response current() {
		return current;
	}

	/** The first response replaces the placeholder the constructor registered; later ones queue behind it. */
	private ResponseOptions register(Response body) {
		if (routeManager != null) {
			if (current instanceof NotConfigured) routeManager.registerRoute(route, body);
			else routeManager.appendRoute(route, body);
		}

		current = body;
		return this;
	}

}

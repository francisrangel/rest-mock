package org.simulatest.restmock;

import java.util.Map;
import java.util.function.BiConsumer;

import org.simulatest.restmock.internal.response.ContentType;
import org.simulatest.restmock.internal.response.Response;
import org.simulatest.restmock.internal.routing.Route;
import org.simulatest.restmock.internal.routing.RouteManager;

/**
 * A response decided per request by the test's own callback.
 *
 * The callback is handed a fresh {@link ResponseOptions} backed by a scratch
 * route table, so it builds its answer with the same {@code thenReturn*} calls
 * as any stub and nothing new to learn. Whatever it registered last is served;
 * if it registered nothing, the usual "no response was configured" 501 is.
 */
final class Answer extends Response {

	private final Route route;
	private final BiConsumer<ReceivedRequest, RestMockResponse> answer;

	Answer(Route route, BiConsumer<ReceivedRequest, RestMockResponse> answer) {
		this.route = route;
		this.answer = answer;
	}

	@Override
	public Response resolve(ReceivedRequest request) {
		RouteManager scratch = new RouteManager();
		answer.accept(request, new ResponseOptions(route, scratch));
		return scratch.get(route);
	}

	@Override
	public ContentType getContentType() {
		return ContentType.TEXT_PLAIN;
	}

	@Override
	public boolean isTextual() {
		return true;
	}

	@Override
	public byte[] render(Map<String, String> parameters) {
		throw new IllegalStateException("An answer is resolved against a request before it is rendered");
	}

}

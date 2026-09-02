package org.simulatest.restmock;

import java.util.Map;
import java.util.function.BiConsumer;

import org.simulatest.restmock.internal.response.ContentType;
import org.simulatest.restmock.internal.response.Response;
import org.simulatest.restmock.internal.routing.Route;

/**
 * A response decided per request by the test's own callback.
 *
 * The callback is handed a fresh {@link ResponseOptions} that registers
 * nothing, so it builds its answer with the same {@code thenReturn*} calls as
 * any stub. Whatever it built last is served; if it built nothing, the usual
 * "no response was configured" 501 is.
 *
 * Nothing else here is ever called: the server resolves an answer against the
 * request before it reads a status, a content type or a body.
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
		ResponseOptions builder = new ResponseOptions(route);
		answer.accept(request, builder);
		return builder.current();
	}

	@Override
	public ContentType getContentType() {
		throw unresolved();
	}

	@Override
	public boolean isTextual() {
		throw unresolved();
	}

	@Override
	public byte[] render(Map<String, String> parameters) {
		throw unresolved();
	}

	private static IllegalStateException unresolved() {
		return new IllegalStateException("An answer is resolved against a request before it is served");
	}

}

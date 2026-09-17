package org.simulatest.restmock.internal.response;

import org.simulatest.restmock.ReceivedRequest;

/**
 * What a stubbed route answers a request with. A {@link Response} answers with
 * itself every time; {@code thenAnswer} builds a fresh one per request.
 */
@FunctionalInterface
public interface Responder {

	Response respond(ReceivedRequest request);

}

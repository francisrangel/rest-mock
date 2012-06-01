package br.com.frs.server;

import br.com.frs.server.response.Response;

public interface MockResponse {

	MockResponse thenReturn(Response response);

}

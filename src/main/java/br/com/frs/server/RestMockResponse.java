package br.com.frs.server;

import br.com.frs.server.response.Response;

public interface RestMockResponse {

	RestMockResponse thenReturn(Response response);

}

package restmock;

import restmock.response.Response;

public interface RestMockResponse {

	void thenReturn(Response response);

}

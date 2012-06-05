package restmock;

import restmock.response.Response;

public interface RestMockResponse {

	RestMockResponse thenReturn(Response response);

}

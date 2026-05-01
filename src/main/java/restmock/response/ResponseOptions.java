package restmock.response;

public class ResponseOptions {

	private final Response response;

	public ResponseOptions(Response response) {
		this.response = response;
	}

	public ResponseOptions withHeader(String property, String value) {
		response.addHeader(property, value);
		return this;
	}

}

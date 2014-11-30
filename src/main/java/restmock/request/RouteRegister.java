package restmock.request;

import java.io.IOException;

import restmock.RestMockResponse;
import restmock.response.Html;
import restmock.response.JSON;
import restmock.response.Response;
import restmock.response.ResponseOptions;
import restmock.response.TextPlain;
import restmock.response.XML;
import restmock.utils.Resource;

public class RouteRegister implements RestMockResponse {

	private final Route route;

	public RouteRegister(Route route) {
		this.route = route;
	}
	
	@Override
	public ResponseOptions thenReturnXML(Object object) {
		return registerRoute(new XML(object));
	}

	@Override
	public ResponseOptions thenReturnXML(String value) {
		return registerRoute(new XML(value));
	}

	@Override
	public ResponseOptions thenReturnHTML(String value) {
		return registerRoute(new Html(value));
	}

	@Override
	public ResponseOptions thenReturnText(String value) {
		return registerRoute(new TextPlain(value));
	}
	
	@Override
	public ResponseOptions thenReturnJSON(String value) {
		return registerRoute(new JSON(value));
	}
	
	@Override
	public ResponseOptions thenReturnJSON(Object object) {
		return registerRoute(new JSON(object));
	}
	
	@Override
	public ResponseOptions thenReturnErrorCodeWithMessage(int errorCode, String message) {
		Response response = new TextPlain(message);
		response.setResponseStatus(errorCode);
		
		return registerRoute(response);
	}
	
	private ResponseOptions registerRoute(Response body) {
		RouteManager.getInstance().registerRoute(route, body);
		return new ResponseOptions(route);
	}

	@Override
	public ResponseOptions thenReturnJSONFromResource(String path) throws IOException {
		return thenReturnJSON(Resource.dataFromResource(path));
	}

	@Override
	public ResponseOptions thenReturnXMLFromResource(String path) throws IOException {
		return thenReturnXML(Resource.dataFromResource(path));
	}

	@Override
	public ResponseOptions theReturnHTMLFromResource(String path) throws IOException {
		return thenReturnHTML(Resource.dataFromResource(path));
	}

	@Override
	public ResponseOptions thenReturnTextFromResource(String path) throws IOException {
		return thenReturnText(Resource.dataFromResource(path));
	}
	
}

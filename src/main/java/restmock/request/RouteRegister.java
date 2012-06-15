package restmock.request;

import restmock.RestMockResponse;
import restmock.response.Html;
import restmock.response.JSON;
import restmock.response.Response;
import restmock.response.TextPlain;
import restmock.response.XML;

public class RouteRegister implements RestMockResponse {

	private final Route route;

	public RouteRegister(Route route) {
		this.route = route;
	}
	
	@Override
	public void thenReturnXML(Object object) {
		registerRoute(new XML(object));
	}

	@Override
	public void thenReturnXML(String value) {
		registerRoute(new XML(value));
	}

	@Override
	public void thenReturnHtml(String value) {
		registerRoute(new Html(value));
	}

	@Override
	public void thenReturnText(String value) {
		registerRoute(new TextPlain(value));
	}
	
	@Override
	public void thenReturnJSON(String value) {
		registerRoute(new JSON(value));
	}
	
	@Override
	public void thenReturnJSON(Object object) {
		try {
			registerRoute(new JSON(object));
		} catch (Exception exception) {
			throw new RuntimeException(exception);
		}
	}
	
	private void registerRoute(Response body) {
		RouteManager.getInstance().registerRoute(route, body);
	}
	
}

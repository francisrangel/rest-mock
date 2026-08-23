package org.simulatest.restmock.internal.routing;

import org.simulatest.restmock.ResponseOptions;
import org.simulatest.restmock.RestMockResponse;
import org.simulatest.restmock.internal.response.Binary;
import org.simulatest.restmock.internal.response.ContentType;
import org.simulatest.restmock.internal.response.Html;
import org.simulatest.restmock.internal.response.JSON;
import org.simulatest.restmock.internal.response.NotConfigured;
import org.simulatest.restmock.internal.response.Response;
import org.simulatest.restmock.internal.response.TextPlain;
import org.simulatest.restmock.internal.response.XML;
import org.simulatest.restmock.internal.utils.Resource;

public class RouteRegister implements RestMockResponse {

	private final Route route;
	private final RouteManager routeManager;

	public RouteRegister(Route route, RouteManager routeManager) {
		this.route = route;
		this.routeManager = routeManager;
		routeManager.registerRoute(route, new NotConfigured(route.getUri()));
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
		routeManager.registerRoute(route, body);
		return new ResponseOptions(body);
	}

	@Override
	public ResponseOptions thenReturnJSONFromResource(String path) {
		return thenReturnJSON(Resource.dataFromResource(path));
	}

	@Override
	public ResponseOptions thenReturnXMLFromResource(String path) {
		return thenReturnXML(Resource.dataFromResource(path));
	}

	@Override
	public ResponseOptions thenReturnHTMLFromResource(String path) {
		return thenReturnHTML(Resource.dataFromResource(path));
	}

	@Override
	public ResponseOptions thenReturnTextFromResource(String path) {
		return thenReturnText(Resource.dataFromResource(path));
	}

	@Override
	public ResponseOptions thenReturnFile(byte[] bytes) {
		return registerBinary(bytes, ContentType.APPLICATION_OCTET_STREAM);
	}

	@Override
	public ResponseOptions thenReturnFile(byte[] bytes, String contentType) {
		return registerBinary(bytes, new ContentType(contentType));
	}

	@Override
	public ResponseOptions thenReturnFileFromResource(String path) {
		return registerBinary(Resource.bytesFromResource(path), ContentType.guessFrom(path));
	}

	@Override
	public ResponseOptions thenReturnFileFromResource(String path, String contentType) {
		return thenReturnFile(Resource.bytesFromResource(path), contentType);
	}

	private ResponseOptions registerBinary(byte[] bytes, ContentType contentType) {
		return registerRoute(new Binary(bytes, contentType));
	}

}

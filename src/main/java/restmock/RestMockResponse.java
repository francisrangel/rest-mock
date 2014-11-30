package restmock;

import java.io.IOException;

import restmock.response.ResponseOptions;

public interface RestMockResponse {

	ResponseOptions thenReturnJSON(Object object);
	ResponseOptions thenReturnJSON(String json);
	ResponseOptions thenReturnJSONFromResource(String path) throws IOException;
	
	ResponseOptions thenReturnXML(Object object);
	ResponseOptions thenReturnXML(String xml);
	ResponseOptions thenReturnXMLFromResource(String path) throws IOException;
	
	ResponseOptions thenReturnHTML(String html);
	ResponseOptions theReturnHTMLFromResource(String path) throws IOException;
	
	ResponseOptions thenReturnText(String txt);
	ResponseOptions thenReturnTextFromResource(String path) throws IOException;
	
	ResponseOptions thenReturnErrorCodeWithMessage(int errorCode, String message);
	
}

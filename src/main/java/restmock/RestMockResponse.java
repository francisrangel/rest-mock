package restmock;

import java.io.FileNotFoundException;

import restmock.response.ResponseOptions;

public interface RestMockResponse {

	ResponseOptions thenReturnJSON(Object object);
	ResponseOptions thenReturnJSON(String json);
	ResponseOptions thenReturnJSONFromResource(String path) throws FileNotFoundException;
	
	ResponseOptions thenReturnXML(Object object);
	ResponseOptions thenReturnXML(String xml);
	ResponseOptions thenReturnXMLFromResource(String path) throws FileNotFoundException;
	
	ResponseOptions thenReturnHTML(String html);
	ResponseOptions theReturnHTMLFromResource(String path) throws FileNotFoundException;
	
	ResponseOptions thenReturnText(String txt);
	ResponseOptions thenReturnTextFromResource(String path) throws FileNotFoundException;
	
	ResponseOptions thenReturnErrorCodeWithMessage(int errorCode, String message);
	
}

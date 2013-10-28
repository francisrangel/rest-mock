package restmock;

import java.io.FileNotFoundException;

public interface RestMockResponse {

	void thenReturnJSON(Object object);
	void thenReturnJSON(String json);
	void thenReturnJSONFromResource(String path) throws FileNotFoundException;
	
	void thenReturnXML(Object object);
	void thenReturnXML(String xml);
	void thenReturnXMLFromResource(String path) throws FileNotFoundException;
	
	void thenReturnHTML(String html);
	void theReturnHTMLFromResource(String path) throws FileNotFoundException;
	
	void thenReturnText(String txt);
	void thenReturnTextFromResource(String path) throws FileNotFoundException;
	
	void thenReturnErrorCodeWithMessage(int errorCode, String message);
	
}

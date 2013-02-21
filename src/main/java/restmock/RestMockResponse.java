package restmock;

public interface RestMockResponse {

	void thenReturnJSON(Object object);
	void thenReturnJSON(String json);
	
	void thenReturnXML(Object object);
	void thenReturnXML(String xml);
	
	void thenReturnHtml(String html);
	
	void thenReturnText(String txt);
	
	void thenReturnErrorCodeWithMessage(int errorCode, String message);
	
}

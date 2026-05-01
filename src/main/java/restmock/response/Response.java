package restmock.response;

import java.net.HttpURLConnection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public abstract class Response {

	private final String content;
	private final Map<String, String> header;
	private int responseStatus = HttpURLConnection.HTTP_OK;
	private long delayMillis;

	Response(String body) {
		this.content = body;
		this.header = new HashMap<>();
	}

	public abstract ContentType getContentType();

	public String getContent() {
		return content;
	}

	public int getResponseStatus() {
		return responseStatus;
	}

	public void setResponseStatus(int responseStatus) {
		this.responseStatus = responseStatus;
	}

	public long getDelayMillis() {
		return delayMillis;
	}

	public void setDelayMillis(long delayMillis) {
		this.delayMillis = delayMillis;
	}

	public Map<String, String> getHeader() {
		return Collections.unmodifiableMap(header);
	}

	public void addHeader(String property, String value) {
		header.put(property, value);
	}

	@Override
	public String toString() {
		return content;
	}

}

package br.com.frs.server.response;

import java.io.IOException;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;

public class JSON extends Response {

	public JSON(String body) {
		super(body);
	}
	
	public JSON(Object object) throws JsonGenerationException, JsonMappingException, IOException {
		super(new ObjectMapper().writeValueAsString(object));
	}

	@Override
	public ContentType getContentType() {
		return ContentType.APPLICATION_JSON;
	}

}

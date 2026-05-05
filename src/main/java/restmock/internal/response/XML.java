package restmock.internal.response;

import java.io.UncheckedIOException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;

public final class XML extends Response {

	public static final XmlMapper MAPPER = (XmlMapper) new XmlMapper().findAndRegisterModules();

	public XML(String body) {
		super(body);
	}

	public XML(Object object) {
		super(write(object));
	}

	@Override
	public ContentType getContentType() {
		return ContentType.TEXT_XML;
	}

	private static String write(Object object) {
		try {
			return MAPPER.writeValueAsString(object);
		} catch (JsonProcessingException e) {
			throw new UncheckedIOException(e);
		}
	}

}

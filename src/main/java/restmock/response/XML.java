package restmock.response;

import restmock.utils.StringUtils;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.StaxDriver;

public final class XML extends Response {

	private static final XStream XML_XSTREAM = new XStream(new StaxDriver());

	public XML(String body) {
		super(body);
	}

	public XML(Object object) {
		super(serializeToXML(object));
	}

	private static String serializeToXML(Object object) {
		String alias = StringUtils.uncapitalize(object.getClass().getSimpleName());
		XML_XSTREAM.alias(alias, object.getClass());
		return XML_XSTREAM.toXML(object);
	}

	@Override
	public ContentType getContentType() {
		return ContentType.TEXT_XML;
	}

}

package org.simulatest.restmock.internal.response;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;

public final class XML extends Template {

	public static final XmlMapper MAPPER = (XmlMapper) new XmlMapper().findAndRegisterModules();

	public XML(String body) {
		super(body);
	}

	public XML(Object object) {
		super(serialize(MAPPER, object));
	}

	@Override
	String escape(String value) {
		return Escape.markup(value);
	}

	@Override
	public ContentType getContentType() {
		return ContentType.TEXT_XML;
	}

}

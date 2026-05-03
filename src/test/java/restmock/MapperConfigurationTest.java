package restmock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.SerializationFeature;

import restmock.response.JSON;
import restmock.response.XML;

public class MapperConfigurationTest {

	record Person(String firstName, String lastName) {}

	@Test
	public void jsonMapperIsExposedAsSingleton() {
		assertNotNull(RestMock.json());
		assertSame(RestMock.json(), RestMock.json());
	}

	@Test
	public void xmlMapperIsExposedAsSingleton() {
		assertNotNull(RestMock.xml());
		assertSame(RestMock.xml(), RestMock.xml());
	}

	@Test
	public void jsonConfigurationFlowsThroughToSerialization() {
		PropertyNamingStrategy original = RestMock.json().getPropertyNamingStrategy();
		try {
			RestMock.json().setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
			assertEquals("{\"first_name\":\"Bob\",\"last_name\":\"Smith\"}",
				new JSON(new Person("Bob", "Smith")).getContent());
		} finally {
			RestMock.json().setPropertyNamingStrategy(original);
		}
	}

	@Test
	public void xmlConfigurationFlowsThroughToSerialization() {
		boolean wasIndented = RestMock.xml().isEnabled(SerializationFeature.INDENT_OUTPUT);
		try {
			RestMock.xml().enable(SerializationFeature.INDENT_OUTPUT);
			String xml = new XML(new Person("Bob", "Smith")).getContent();
			assertTrue(xml.contains("\n"), "expected indented XML to contain newlines, got: " + xml);
		} finally {
			if (!wasIndented) RestMock.xml().disable(SerializationFeature.INDENT_OUTPUT);
		}
	}

}

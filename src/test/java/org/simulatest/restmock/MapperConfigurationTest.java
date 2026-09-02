package org.simulatest.restmock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.SerializationFeature;

import org.simulatest.restmock.internal.response.JSON;
import org.simulatest.restmock.internal.response.XML;

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

	// These tests mutate the JVM-wide mappers that ResponseOptionsTest and
	// WhenGetTestCase depend on for their exact expected serialization, so the restore
	// has to be enforced by the fixture rather than by a finally block in each test.
	private PropertyNamingStrategy originalNamingStrategy;
	private boolean xmlWasIndented;

	@BeforeEach
	public void captureMapperConfiguration() {
		originalNamingStrategy = RestMock.json().getPropertyNamingStrategy();
		xmlWasIndented = RestMock.xml().isEnabled(SerializationFeature.INDENT_OUTPUT);
	}

	@AfterEach
	public void restoreMapperConfiguration() {
		RestMock.json().setPropertyNamingStrategy(originalNamingStrategy);
		RestMock.xml().configure(SerializationFeature.INDENT_OUTPUT, xmlWasIndented);
	}

	@Test
	public void jsonConfigurationFlowsThroughToSerialization() {
		RestMock.json().setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);

		assertEquals("{\"first_name\":\"Bob\",\"last_name\":\"Smith\"}",
			new JSON(new Person("Bob", "Smith")).getContent());
	}

	@Test
	public void xmlConfigurationFlowsThroughToSerialization() {
		RestMock.xml().enable(SerializationFeature.INDENT_OUTPUT);

		String xml = new XML(new Person("Bob", "Smith")).getContent();

		assertTrue(xml.contains("\n"), "expected indented XML to contain newlines, got: " + xml);
	}

}

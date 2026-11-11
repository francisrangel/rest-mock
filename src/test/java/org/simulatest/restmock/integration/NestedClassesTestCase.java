package org.simulatest.restmock.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.simulatest.restmock.HttpMock;
import org.simulatest.restmock.RestMockExtension;

/** Two @Nested classes, so one of them always runs after a sibling has ended. */
class NestedClassesTestCase {

	@RegisterExtension
	static RestMockExtension restMock = new RestMockExtension(new HttpMock()).keepRoutes();

	/** The address a client built once for the whole class would hold. */
	static String addressAtStart;

	@BeforeAll
	static void stubTheSharedFixture() {
		addressAtStart = restMock.baseUrl();
		restMock.whenGet("/users/1").thenReturnText("bob");
	}

	private static void assertTheServerFromTheStartStillAnswers() throws Exception {
		assertEquals("bob", TestHttp.get(addressAtStart + "/users/1").body());
	}

	@Nested
	class First {

		@Test
		void theServerAndItsRoutesOutliveASiblingClass() throws Exception {
			assertTheServerFromTheStartStillAnswers();
		}

	}

	@Nested
	class Second {

		@Test
		void theServerAndItsRoutesOutliveASiblingClass() throws Exception {
			assertTheServerFromTheStartStillAnswers();
		}

	}

}

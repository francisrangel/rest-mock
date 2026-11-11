package org.simulatest.restmock.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.simulatest.restmock.HttpMock;
import org.simulatest.restmock.RestMockExtension;

/**
 * Two @Nested classes that mirror each other, so whichever JUnit runs second
 * is the one that runs after a sibling has ended.
 */
class NestedClassesTestCase {

	@RegisterExtension
	static RestMockExtension restMock = new RestMockExtension(new HttpMock());

	/** The address a client built once for the whole class would hold. */
	static String addressAtStart;

	@BeforeAll
	static void stubForEveryNestedClass() {
		addressAtStart = restMock.baseUrl();
		restMock.whenGet("/users/1").thenReturnText("bob");
	}

	private static void assertTheServerFromTheStartStillAnswers() throws Exception {
		assertEquals("bob", TestHttp.get(addressAtStart + "/users/1").body());
	}

	private static void assertNotStubbed(String path) throws Exception {
		assertEquals(404, TestHttp.get(restMock.url(path)).statusCode());
	}

	@Nested
	@TestInstance(Lifecycle.PER_CLASS)
	class First {

		@BeforeAll
		void stubForThisClassOnly() {
			restMock.whenGet("/first").thenReturnText("first");
		}

		@Test
		void theServerAndTheOuterStubsOutliveASiblingClass() throws Exception {
			assertTheServerFromTheStartStillAnswers();
		}

		@Test
		void aSiblingClassesStubDoesNotReachThisOne() throws Exception {
			assertNotStubbed("/second");
		}

	}

	@Nested
	@TestInstance(Lifecycle.PER_CLASS)
	class Second {

		@BeforeAll
		void stubForThisClassOnly() {
			restMock.whenGet("/second").thenReturnText("second");
		}

		@Test
		void theServerAndTheOuterStubsOutliveASiblingClass() throws Exception {
			assertTheServerFromTheStartStillAnswers();
		}

		@Test
		void aSiblingClassesStubDoesNotReachThisOne() throws Exception {
			assertNotStubbed("/first");
		}

	}

}

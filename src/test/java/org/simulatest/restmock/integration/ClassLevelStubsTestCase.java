package org.simulatest.restmock.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.simulatest.restmock.HttpMock;
import org.simulatest.restmock.RestMockExtension;

/**
 * What a class stubs in its @BeforeAll is there for every test, and what a
 * test stubs is gone for the next. The two tests mirror each other, so
 * whichever JUnit runs second is the one that proves it.
 */
class ClassLevelStubsTestCase {

	@RegisterExtension
	static RestMockExtension restMock = new RestMockExtension(new HttpMock());

	@BeforeAll
	static void stubForTheWholeClass() {
		restMock.whenGet("/shared").thenReturnText("for the class");
	}

	@Test
	void oneTestSeesTheClassStubAndNotTheOtherTestsStub() throws Exception {
		restMock.whenGet("/one").thenReturnText("for one test");

		assertEquals("for the class", TestHttp.get(restMock.url("/shared")).body());
		assertEquals(404, TestHttp.get(restMock.url("/other")).statusCode());
	}

	@Test
	void theOtherTestSeesTheClassStubAndNotTheFirstTestsStub() throws Exception {
		restMock.whenGet("/other").thenReturnText("for the other test");

		assertEquals("for the class", TestHttp.get(restMock.url("/shared")).body());
		assertEquals(404, TestHttp.get(restMock.url("/one")).statusCode());
	}

}

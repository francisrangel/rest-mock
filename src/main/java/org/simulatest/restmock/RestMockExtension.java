package org.simulatest.restmock;

import java.util.Objects;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * JUnit 5 extension that starts the mock server before the test class runs and
 * stops it afterwards. Routes and recorded requests are cleared between tests
 * unless you call {@link #keepRoutes()}.
 *
 * Register it on a static field so JUnit ties it to the class lifecycle (one
 * server per class). A non-static field would be reinstantiated per test:
 *
 *   {@code @RegisterExtension static RestMockExtension mock = new RestMockExtension();}
 *
 * That form drives the process-wide default mock, the one behind the static
 * {@link RestMock} methods. Pass an {@link HttpMock} instead to give the class
 * its own mock, which is what lets two test classes run at once:
 *
 *   {@code static HttpMock mock = new HttpMock();}
 *   {@code @RegisterExtension static RestMockExtension server = new RestMockExtension(mock);}
 *
 * Without a port the server binds one the OS assigns, so two builds sharing a
 * machine never fight over it; {@link RestMock#baseUrl()} is the address either
 * way. Pass a port when something outside the test has to know it.
 */
public class RestMockExtension implements BeforeAllCallback, AfterAllCallback, AfterEachCallback {

	private final HttpMock mock;
	private final int port;
	private boolean autoClean = true;

	/** Drives the default mock on an OS-assigned port. */
	public RestMockExtension() {
		this(RestMock.defaultMock());
	}

	/** Drives the default mock on a fixed port. */
	public RestMockExtension(int port) {
		this(RestMock.defaultMock(), port);
	}

	/**
	 * Drives {@code mock} on an OS-assigned port. Give each class its own
	 * {@link HttpMock} and they no longer share routes or a port.
	 */
	public RestMockExtension(HttpMock mock) {
		this(mock, 0);
	}

	/** Drives {@code mock} on a fixed port. */
	public RestMockExtension(HttpMock mock, int port) {
		this.mock = Objects.requireNonNull(mock, "mock");
		this.port = port;
	}

	/** The mock this extension drives. */
	public HttpMock mock() {
		return mock;
	}

	/**
	 * Disables the per-test reset. Routes and recorded requests survive across
	 * tests in the class. Useful when several tests share the same fixture.
	 */
	public RestMockExtension keepRoutes() {
		this.autoClean = false;
		return this;
	}

	@Override
	public void beforeAll(ExtensionContext context) {
		mock.startServer(port);
	}

	@Override
	public void afterAll(ExtensionContext context) {
		mock.stopServer();
	}

	@Override
	public void afterEach(ExtensionContext context) {
		if (autoClean) mock.clean();
	}

}

package restmock;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JUnit 5 extension that starts the mock server before the test class runs and
 * stops it afterwards. Routes and recorded requests are cleared between tests
 * unless you call {@link #keepRoutes()}.
 *
 * Register it on a static field so JUnit ties it to the class lifecycle (one
 * server per class). A non-static field would be reinstantiated per test:
 *
 *   {@code @RegisterExtension static RestMockExtension mock = new RestMockExtension();}
 */
public class RestMockExtension implements BeforeAllCallback, AfterAllCallback, AfterEachCallback {

	private static final Logger log = LoggerFactory.getLogger(RestMockExtension.class);

	private final int port;
	private boolean autoClean = true;

	/** Uses {@link RestMock#DEFAULT_PORT}. */
	public RestMockExtension() {
		this(RestMock.DEFAULT_PORT);
	}

	/** Uses the given port. */
	public RestMockExtension(int port) {
		this.port = port;
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
		RestMock.startServer(port);
	}

	@Override
	public void afterAll(ExtensionContext context) {
		RestMock.stopServer();
	}

	@Override
	public void afterEach(ExtensionContext context) {
		if (autoClean) {
			RestMock.clean();
			log.debug("Auto-cleaned routes and request log after {}", context.getDisplayName());
		}
	}

}

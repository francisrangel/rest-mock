package restmock;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RestMockExtension implements BeforeAllCallback, AfterAllCallback, AfterEachCallback {

	private static final Logger log = LoggerFactory.getLogger(RestMockExtension.class);

	private final int port;
	private boolean autoClean = true;

	public RestMockExtension() {
		this(RestMock.DEFAULT_PORT);
	}

	public RestMockExtension(int port) {
		this.port = port;
	}

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

package restmock.utils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public final class Resource {

	private Resource() {}

	public static String dataFromResource(String resource) throws IOException {
		try (InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(resource)) {
			if (is == null) throw new FileNotFoundException(resource + " was not found at resources folder!");
			return new String(is.readAllBytes(), StandardCharsets.UTF_8);
		}
	}

}

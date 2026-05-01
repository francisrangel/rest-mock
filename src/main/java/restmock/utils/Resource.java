package restmock.utils;

import static java.nio.file.Files.readAllBytes;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;

public final class Resource {

	private Resource() {}

	public static String dataFromResource(String resource) throws IOException {
		try {
			return new String(readAllBytes(Paths.get(fullPath(resource).toURI())));
		} catch (URISyntaxException e) {
			throw new IOException(e);
		}
	}

	private static URL fullPath(String resource) throws FileNotFoundException {
		URL url = Thread.currentThread().getContextClassLoader().getResource(resource);
		if (url == null) throw new FileNotFoundException(resource + " was not found at resources folder!");
		return url;
	}

}

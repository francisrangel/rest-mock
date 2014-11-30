package restmock.utils;

import static java.nio.file.Files.readAllBytes;
import static java.nio.file.Paths.get;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;

public class Resource {
	
	public static final String LINE_SEPARATOR = System.getProperty("line.separator");
	
	public static String dataFromResource(String resource) throws IOException {
		return new String(readAllBytes(get(fullPath(resource))));
	}
	
	private static String fullPath(String resource) throws FileNotFoundException {
		URL url = Thread.currentThread().getContextClassLoader().getResource(resource);
		if (url == null) throw new FileNotFoundException(resource + " was not found at resources folder!");
		return url.getPath();
	}

}

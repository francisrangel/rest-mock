package restmock.utils;

import static java.nio.file.FileSystems.getDefault;
import static java.nio.file.Files.readAllBytes;

import java.io.IOException;

public class Resource {
	
	public static final String LINE_SEPARATOR = System.getProperty("line.separator");
	
	public static String dataFromResource(String resource) throws IOException {
		String path = Resource.class.getProtectionDomain().getCodeSource().getLocation().getPath();
		return new String(readAllBytes(getDefault().getPath(path, resource)));
	}

}

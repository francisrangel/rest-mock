package restmock.utils;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Scanner;

public class Resource {
	
	public static final String LINE_SEPARATOR = System.getProperty("line.separator");
	
	public static String dataFromResource(String path) throws FileNotFoundException {
		return valueOfFile(fileFromResource(path));
	}
	
	private static InputStream fileFromResource(String path) throws FileNotFoundException {
		InputStream resource = Thread.currentThread().getContextClassLoader().getResourceAsStream(path);
		if (resource == null) throw new FileNotFoundException(path + " was not found!");
		return resource;
	}
	
	private static String valueOfFile(InputStream inputStream) throws FileNotFoundException {
		return new Scanner(inputStream, "UTF8").useDelimiter("\\Z").next();
	}

}

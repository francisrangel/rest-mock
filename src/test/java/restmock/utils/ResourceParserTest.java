package restmock.utils;

import static org.junit.Assert.assertEquals;

import java.io.FileNotFoundException;

import org.junit.Test;

public class ResourceParserTest {
	
	@Test
	public void simpleParseSample() throws FileNotFoundException {
		String expected = "rest-mock rock! :-)";
		assertEquals(expected, Resource.dataFromResource("example.txt"));
	}
	
	@Test(expected = FileNotFoundException.class)
	public void unexistentFile() throws FileNotFoundException {
		Resource.dataFromResource("unexistent.file");
	}

}

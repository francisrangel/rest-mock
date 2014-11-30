package restmock.utils;

import static org.junit.Assert.assertEquals;

import java.nio.file.NoSuchFileException;

import org.junit.Test;

public class ResourceParserTest {
	
	@Test
	public void simpleParseSample() throws Exception {
		String expected = "rest-mock rock! :-)";
		assertEquals(expected, Resource.dataFromResource("example.txt"));
	}
	
	@Test(expected = NoSuchFileException.class)
	public void unexistentFile() throws Exception {
		Resource.dataFromResource("unexistent.file");
	}

}

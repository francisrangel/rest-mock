package restmock.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.FileNotFoundException;

import org.junit.jupiter.api.Test;

public class ResourceParserTest {

	@Test
	public void simpleParseSample() throws Exception {
		String expected = "rest-mock rock! :-)";
		assertEquals(expected, Resource.dataFromResource("example.txt"));
	}

	@Test
	public void unexistentFile() {
		assertThrows(FileNotFoundException.class, () -> Resource.dataFromResource("unexistent.file"));
	}

}

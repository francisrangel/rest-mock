package restmock.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class StringUtilsTest {
	
	@Test
	public void uncapitalizeTest() {
		assertNull(StringUtils.uncapitalize(null));
		assertEquals("", StringUtils.uncapitalize(""));
		assertEquals("i am fINE", StringUtils.uncapitalize("I Am FINE"));
		assertEquals("a", StringUtils.uncapitalize("A"));
		assertEquals("uNCAPITALIZEWORD", StringUtils.uncapitalize("UNCAPITALIZEWORD"));
	}

}

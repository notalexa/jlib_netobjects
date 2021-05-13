package not.alexa.coding.xml;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import java.util.Arrays;
import java.util.List;

import not.alexa.netobjects.coding.xml.XMLHelper;

@RunWith(org.junit.runners.Parameterized.class)
public class XMLHelperTest {

	@Parameters
	public static List<TestCase> primitiveClasses() {
		return Arrays.asList(new TestCase[] {
		        new TestCase("","",""),
		        new TestCase("01234567890","01234567890","01234567890"),
		        new TestCase("<>&","&lt;&gt;&amp;","&lt;&gt;&amp;"),
		        new TestCase("\"\r\n","&quot;&#xd;&#xa;","\"\r\n")
		});
	}
	
	@Parameter
	public TestCase testCase;
	
	public XMLHelperTest() {
	}
	
	@Test
	public void checkAttribute() {
	    assertEquals(testCase.attribute,XMLHelper.attribute(testCase.s));
        assertEquals(testCase.attribute,XMLHelper.encode(true,testCase.s));
	}
	
    @Test
    public void checkText() {
        assertEquals(testCase.text,XMLHelper.text(testCase.s));
        assertEquals(testCase.text,XMLHelper.encode(false,testCase.s));
    }
    
	private static class TestCase {
	    String s;
	    String attribute;
	    String text;
	    public TestCase(String s,String attribute,String text) {
	        this.s=s;
	        this.attribute=attribute;
	        this.text=text;
	    }
	}
}

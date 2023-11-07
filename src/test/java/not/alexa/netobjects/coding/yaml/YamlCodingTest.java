package not.alexa.netobjects.coding.yaml;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import not.alexa.netobjects.BaseException;
import not.alexa.netobjects.Context;
import not.alexa.netobjects.coding.yaml.Token.Type;
import not.alexa.netobjects.coding.yaml.Yaml.Mode;
import not.alexa.netobjects.types.DefaultTypeLoader;
import not.alexa.netobjects.utils.Sequence;

public class YamlCodingTest {

	public YamlCodingTest() {
	}
    
    @Test
    public void testFile1() {
    	YamlCodingScheme scheme=new YamlCodingScheme(new Yaml(Mode.Indented));
    	Context context=Context.createRootContext(new DefaultTypeLoader());
    	List<Object> result=new ArrayList<>();

    	try(InputStream stream=getClass().getResourceAsStream("codingtest1.yaml"); 
    		Sequence<Object> seq=scheme.createDecoder(context, stream).decodeAll(Object.class)) {
    		for(Object o:seq) {
    			result.add(o);
    		}
    		// fail(); Exception is thrown at close
    	} catch(BaseException|IOException t) {
    		//t.printStackTrace();
    		assertEquals(1,result.size());
    	}
    }
    
    @Test
    public void testFile2() {
    	YamlCodingScheme scheme=new YamlCodingScheme(new Yaml(Mode.Indented));
    	Context context=Context.createRootContext(new DefaultTypeLoader());
    	List<Object> result=new ArrayList<>();

    	try(InputStream stream=getClass().getResourceAsStream("codingtest2.yaml"); 
    		Sequence<Object> seq=scheme.createDecoder(context, stream).decodeAll(Object.class)) {
    		for(Object o:seq) {
    			result.add(o);
    		}
    		// fail(); Exception is thrown at close
    	} catch(BaseException|IOException t) {
    		//t.printStackTrace();
    		assertEquals(1,result.size());
    	}
    }
    
    @Test
    public void testFile3() {
    	YamlCodingScheme scheme=new YamlCodingScheme(new Yaml(Mode.Indented));
    	Context context=Context.createRootContext(new DefaultTypeLoader());
    	List<Object> result=new ArrayList<>();

    	try(InputStream stream=getClass().getResourceAsStream("codingtest3.yaml"); 
    		Sequence<Object> seq=scheme.createDecoder(context, stream).decodeAll(Object.class)) {
    		for(Object o:seq) {
    			result.add(o);
    		}
    		assertEquals(2,result.size());
    	} catch(BaseException|IOException t) {
    		fail();
    	}
    }
    
    @Test
    public void testFile4() {
    	YamlCodingScheme scheme=YamlCodingScheme.builder().setCharset("UTF-8").build();
    	Context context=Context.createRootContext(new DefaultTypeLoader());
    	List<Object> result=new ArrayList<>();
    	Token token=new MapToken()
    			.add(new Token.SimpleToken("class"), new Token.SimpleToken("int"))
    			.add(new Token.SimpleToken("."),new Token.SimpleToken("123"));

    	try( 
    		Sequence<Object> seq=scheme.createDecoder(context, token).decodeAll(Object.class)) {
    		for(Object o:seq) {
    			result.add(o);
    			System.out.println(o);
    		}
    		assertEquals(1,result.size());
    	} catch(BaseException|IOException t) {
    		fail();
    	}
    }

    @Test
    public void testFile5() {
    	YamlCodingScheme scheme=new YamlCodingScheme(new Yaml(Mode.Indented));
    	Context context=Context.createRootContext(new DefaultTypeLoader());
    	List<Object> result=new ArrayList<>();

    	try(InputStream stream=new ByteArrayInputStream("%YAML 2.0".getBytes()); 
    		Sequence<Object> seq=scheme.createDecoder(context, stream).decodeAll(Object.class)) {
    		for(Object o:seq) {
    			result.add(o);
    		}
    		// fail(); Exception is thrown at close
    	} catch(BaseException|IOException t) {
    		//t.printStackTrace();
    		assertEquals(0,result.size());
    	}
    }

}

/*
 * Copyright (C) 2023 Not Alexa
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package not.alexa.netobjects.coding.xml;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.junit.Test;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import not.alexa.netobjects.BaseException;
import not.alexa.netobjects.Context;
import not.alexa.netobjects.coding.CodingScheme;
import not.alexa.netobjects.coding.xml.XMLDecoder.NodeAttributes;
import not.alexa.netobjects.coding.yaml.YamlCodingScheme;
import not.alexa.netobjects.types.DefaultTypeLoader;
import not.alexa.netobjects.utils.Sequence;

public class XMLCodingTest {

	public XMLCodingTest() {
	}
	
	@Test
	public void nodeTest() {
		try {
			DocumentBuilder builder=DocumentBuilderFactory.newInstance().newDocumentBuilder();
			System.out.println(XMLCodingScheme.DEFAULT_SCHEME.createDecoder(Context.createRootContext(), builder.parse(new ByteArrayInputStream("<object class=\"java.lang.String\">&lt;123&gt;</object>".getBytes()))).decode(Object.class));
		} catch(DOMException|ParserConfigurationException|IOException|SAXException|BaseException e) {
			e.printStackTrace();
			fail();
		}
	}

	@Test
	public void attrNodeTest() {
		try {
			DocumentBuilder builder=DocumentBuilderFactory.newInstance().newDocumentBuilder();
			Document doc=builder.parse(new ByteArrayInputStream("<object class=\"java.lang.String\">&lt;123&gt;</object>".getBytes()));
			NodeAttributes attrs=new NodeAttributes(doc.getLastChild().getAttributes());
			assertEquals(1,attrs.getLength());
			assertEquals(0, attrs.getIndex("class"));
			assertEquals(0, attrs.getIndex(null,"class"));
			assertEquals("java.lang.String", attrs.getValue(0));
			assertEquals("java.lang.String", attrs.getValue("class"));
			assertEquals("java.lang.String", attrs.getValue(null,"class"));
			assertEquals("CDATA", attrs.getType(0));
			assertEquals("CDATA", attrs.getType("class"));
			assertEquals("CDATA", attrs.getType(null,"class"));
			assertNull(attrs.getLocalName(0));
			assertEquals("class", attrs.getQName(0));
			assertNull(attrs.getURI(0));
			assertEquals(-1, attrs.getIndex("clazz"));
			assertEquals(-1, attrs.getIndex(null,"clazz"));
			assertNull(attrs.getValue("clazz"));
			assertNull(attrs.getValue(null,"clazz"));
		} catch(DOMException|ParserConfigurationException|IOException|SAXException e) {
			e.printStackTrace();
			fail();
		}
	}
	
    @Test
    public void testFile1() {
    	XMLCodingScheme scheme=XMLCodingScheme.DEFAULT_SCHEME.newBuilder().setResourceBranch("resource").build();// YamlCodingScheme(new Yaml(Mode.Indented));
    	Context context=Context.createRootContext(new DefaultTypeLoader());
    	List<Object> result=new ArrayList<>();
    	try(InputStream stream=getClass().getResourceAsStream("codingtest1.xml"); 
    		Sequence<Object> seq=scheme.createDecoder(context, stream).decodeAll(Object.class)) {
    		for(Object o:seq) {
    			result.add(o);
    		}
    		System.out.println("Result "+result);
    		// fail(); Exception is thrown at close
    	} catch(BaseException|IOException t) {
    		t.printStackTrace();
    		assertEquals(0,result.size());
    	}
    }
    
    @Test
    public void testFile2() {
    	Context context=Context.createRootContext();
    	CodingScheme scheme=XMLCodingScheme.DEFAULT_SCHEME.newBuilder().setIndent("  ", "\n").setRootType(context.resolveType(Integer.class)).build();
    	try(Sequence<Object> seq=scheme.createDecoder(context, "<object>123</object>".getBytes()).decodeAll(Object.class)) {
    		for(Object o:seq) {
    			assertEquals(123,o);
    			System.out.write(scheme.createEncoder(context).encode(o).asBytes());
    		}
    	} catch(BaseException|IOException t) {
    		t.printStackTrace();
    		fail();
    	}
    	
    }

    @Test
    public void testFile3() {
    	Context context=Context.createRootContext();
    	CodingScheme scheme=XMLCodingScheme.DEFAULT_SCHEME.newBuilder().setIndent("  ", "\n").setRootType(context.resolveType(Object[].class)).build();
    	try(Sequence<Object> seq=scheme.createDecoder(context, "<object><object class=\"java.lang.String\">Hello World</object><object class=\"int\">123</object></object>\n".getBytes()).decodeAll(Object.class)) {
    		for(Object o:seq) {
    			assert(o.getClass().isArray());
    			System.out.write(scheme.createEncoder(context).encode(o).asBytes());
    		}
    	} catch(BaseException|IOException t) {
    		t.printStackTrace();
    		fail();
    	}
    }

    @Test
    public void testFile4() {
    	Context context=Context.createRootContext();
    	CodingScheme scheme=XMLCodingScheme.DEFAULT_SCHEME.newBuilder().setIndent("  ", "\n").setRootType(context.resolveType(Object[].class)).build();
    	try(Sequence<Object> seq=scheme.createDecoder(context, "<object is-empty=\"true\"/>\n".getBytes()).decodeAll(Object.class)) {
    		for(Object o:seq) {
    			assert(o.getClass().isArray());
    			System.out.write(scheme.createEncoder(context).encode(o).asBytes());
    		}
    	} catch(BaseException|IOException t) {
    		t.printStackTrace();
    		fail();
    	}
    }

}

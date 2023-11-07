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

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.junit.Test;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import not.alexa.netobjects.BaseException;
import not.alexa.netobjects.Context;
import not.alexa.netobjects.coding.xml.XMLDecoder.NodeAttributes;

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


}

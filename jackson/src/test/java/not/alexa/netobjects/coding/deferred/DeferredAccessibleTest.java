package not.alexa.netobjects.coding.deferred;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.fasterxml.jackson.annotation.JsonProperty;

import not.alexa.netobjects.Context;
import not.alexa.netobjects.coding.ByteEncoder;
import not.alexa.netobjects.coding.CodingScheme;
import not.alexa.netobjects.coding.Decoder;
import not.alexa.netobjects.coding.deferred.DeferredInterfaceTest.Content;
import not.alexa.netobjects.coding.protobuf.ProtobufCodingScheme;
import not.alexa.netobjects.coding.xml.XMLCodingScheme;
import not.alexa.netobjects.coding.yaml.YamlCodingScheme;
import not.alexa.netobjects.jackson.ClassLoaderTestWrapper;
import not.alexa.netobjects.types.DefaultTypeLoader;
import not.alexa.netobjects.types.DeferredObject;
import not.alexa.netobjects.types.ObjectType;
import not.alexa.netobjects.types.TypeDefinition;
import not.alexa.netobjects.utils.BackingClassLoader;

@RunWith(org.junit.runners.Parameterized.class)
public class DeferredAccessibleTest {
	
    @Parameters
    public static List<TestData> testObjects() {
        return Arrays.asList(new TestData[] {
        		new TestData(XMLCodingScheme.DEFAULT_SCHEME.newBuilder().setIndent("  ", "\n").build(),0,"Display content of default","Display content of TestClass: s1"),
        		new TestData(XMLCodingScheme.DEFAULT_SCHEME.newBuilder().setIndent("  ", "\n").build(),1,"Display content of default","Display content of TestClass: s1"),
        		new TestData(XMLCodingScheme.DEFAULT_SCHEME.newBuilder().setIndent("  ", "\n").build(),2,"Display content of default","Display content of TestClass: s1"),
        		new TestData(XMLCodingScheme.DEFAULT_SCHEME.newBuilder().setIndent("  ", "\n").build(),3,"Display content: not.alexa.netobjects.coding.xml.DeferredXMLObject","Display content: not.alexa.netobjects.coding.xml.DeferredXMLObject"),
        		new TestData(YamlCodingScheme.DEFAULT_SCHEME,0,"Display content of default","Display content of TestClass: s1"),
        		new TestData(YamlCodingScheme.DEFAULT_SCHEME,1,"Display content of default","Display content of TestClass: s1"),
        		new TestData(YamlCodingScheme.DEFAULT_SCHEME,2,"Display content of default","Display content of TestClass: s1"),
        		new TestData(YamlCodingScheme.DEFAULT_SCHEME,3,"Display content: not.alexa.netobjects.coding.yaml.DeferredTokenizedObject","Display content: not.alexa.netobjects.coding.yaml.DeferredTokenizedObject"),
        		new TestData(ProtobufCodingScheme.DEFAULT_SCHEME,0,"Display content of default","Display content of TestClass: s1"),
        		new TestData(ProtobufCodingScheme.DEFAULT_SCHEME,1,"Display content of default","Display content of TestClass: s1"),
        		new TestData(ProtobufCodingScheme.DEFAULT_SCHEME,2,"Display content of default","Display content of TestClass: s1"),
        		new TestData(ProtobufCodingScheme.DEFAULT_SCHEME,3,"Display content: not.alexa.netobjects.coding.protobuf.DeferredProtobufObject","Display content: not.alexa.netobjects.coding.protobuf.DeferredProtobufObject"),
        });
    }
    
	@Parameter public TestData data;
	
	public DeferredAccessibleTest() {
	}
	
	public byte[] createData(CodingScheme scheme) throws Throwable {
		ClassLoader classLoader=new BackingClassLoader(new URL[] { new File("src/test/overlay").getAbsoluteFile().toURI().toURL()}, ClassLoaderTestWrapper.class.getClassLoader());
		Context context1=Context.createRootContext(new DefaultTypeLoader(classLoader));
		try(InputStream in=DeferredCodingTest.class.getResourceAsStream("accessible.yaml");
			Decoder decoder=YamlCodingScheme.REST_SCHEME.createDecoder(context1, in)) {
			Content o=decoder.decode(Content.class);
			try(ByteEncoder encoder=scheme.createEncoder(context1)) {
					switch(data.index) {
					case 0:return encoder.encode(o).asBytes();
					case 1:return encoder.encode(new ContentData(o)).asBytes();
					case 2: return encoder.encode(new ObjectData(o)).asBytes();
					case 3: return encoder.encode(new DeferredData(new DeferredObject(o))).asBytes();
				}
			}
		}
		return null;
	}
	
	@Test public void test0() {
		try {
			CodingScheme scheme=data.scheme;
			byte[] content=createData(scheme);
			ClassLoader classLoader=new BackingClassLoader(new URL[] { new File("src/test/overlay").getAbsoluteFile().toURI().toURL()}, ClassLoaderTestWrapper.class.getClassLoader());
			Context context1=Context.createRootContext(new DefaultTypeLoader(classLoader));
			Class<?> testClass=Class.forName("not.alexa.netobjects.jackson.ClassLoaderTest$TestClass", false, classLoader);
			ObjectType testType=ObjectType.createClassType(testClass);
			TypeDefinition def=context1.resolveType(testType);
			Context context0=Context.createRootContext(new DefaultTypeLoader() {
				@Override
				public TypeDefinition resolveType(ObjectType t) {
					if(t.equals(testType)) {
						return def;
					}
					return super.resolveType(t);
				}
			});
			System.out.write(content);System.out.println();
			try(InputStream in=new ByteArrayInputStream(content);
				Decoder decoder=scheme.createDecoder(context0, in)) {
				Content o=decoder.decode(Content.class);
				assertEquals(data.expectedValue1,o.displayContent(context0));
				try(ByteEncoder encoder=data.scheme.createEncoder(context0)) {
					byte[] encoded=encoder.encode(o).asBytes();
					assertArrayEquals(content,encoded);
				}
				assertEquals(data.expectedValue2,o.displayContent(context1));
				try(ByteEncoder encoder=data.scheme.createEncoder(context1)) {
					byte[] encoded=encoder.encode(o).asBytes();
					assertArrayEquals(content,encoded);
				}
			}
		} catch(Throwable t) {
			t.printStackTrace();
			fail();
		}

	}

	@Test public void test1() {
		try {
			CodingScheme scheme=data.scheme;
			byte[] content=createData(scheme);
			ClassLoader classLoader=new BackingClassLoader(new URL[] { new File("src/test/overlay").getAbsoluteFile().toURI().toURL()}, ClassLoaderTestWrapper.class.getClassLoader());
			Context context1=Context.createRootContext(new DefaultTypeLoader(classLoader));
			Context context0=Context.createRootContext();
			try(InputStream in=new ByteArrayInputStream(content);
				Decoder decoder=scheme.createDecoder(context0, in)) {
				Content o=decoder.decode(Content.class);
				assertEquals(data.expectedValue1,o.displayContent(context0));
				try(ByteEncoder encoder=data.scheme.createEncoder(context0)) {
					byte[] encoded=encoder.encode(o).asBytes();
					assertArrayEquals(content,encoded);
				}
				assertEquals(data.expectedValue2,o.displayContent(context1));
				try(ByteEncoder encoder=data.scheme.createEncoder(context1)) {
					byte[] encoded=encoder.encode(o).asBytes();
					assertArrayEquals(content,encoded);
				}
			}
		} catch(Throwable t) {
			t.printStackTrace();
			fail();
		}
	}
	
	public static class ContentData implements Content {
		@JsonProperty Content content;
		ContentData() {}
		public ContentData(Content content) {
			this.content=content;
		}
		@Override
		public String displayContent(Context context) {
			return content.displayContent(context);
		}
	}

	public static class ObjectData implements Content {
		@JsonProperty Object content;
		ObjectData() {}
		public ObjectData(Object content) {
			this.content=content;
		}
		@Override
		public String displayContent(Context context) {
			Content c=context.cast(Content.class, content);
			if(c==null) {
				return "Display content: "+content;
			} else {
				return c.displayContent(context);
			}
		}
	}
	
	public static class DeferredData implements Content {
		@JsonProperty DeferredObject content;
		DeferredData() {}
		public DeferredData(DeferredObject content) {
			this.content=content;
		}
		@Override
		public String displayContent(Context context) {
			Content c=context.cast(Content.class, content);
			if(c==null) {
				return "Display content: "+content.getClass().getName();
			} else {
				return c.displayContent(context);
			}
		}
	}

	public static class Data {
		@JsonProperty String s;
	}
	
	public static class TestData {
		CodingScheme scheme;
		int index;
		String expectedValue1;
		String expectedValue2;
		public TestData(CodingScheme scheme, int index,String v1,String v2) {
			this.scheme=scheme;
			this.index=index;
			this.expectedValue1=v1;
			this.expectedValue2=v2;
		}
	}

}

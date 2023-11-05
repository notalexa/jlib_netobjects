package not.alexa.netobjects.coding.yaml;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import not.alexa.netobjects.coding.text.LineReader.Line;
import not.alexa.netobjects.coding.yaml.Token.Type;
import not.alexa.netobjects.coding.yaml.Yaml.Document;
import not.alexa.netobjects.coding.yaml.Yaml.Handler;
import not.alexa.netobjects.coding.yaml.Yaml.Mode;
import not.alexa.netobjects.coding.yaml.YamlLine.Tokenizer;

public class MiscTest {

	public MiscTest() {
	}
	
	@Test
	public void mapValueTest() {
		Token t=new Token.SimpleToken(Type.Alias,null,"xxx");
		Map.Entry<Token,Token> entry=new MapToken.Item(t, t);
		assert(entry.getKey()==t);
		assert(entry.getValue()==t);
		Token n=new Token.SimpleToken(Type.Scalar, null, "xxx");
		assert(entry.setValue(n)==t);
		assert(entry.getKey()==t);
		assert(entry.getValue()==n);
	}
	
	@Test
	public void exceptionTest1() {
		try {
			throw new YamlException();
		} catch(YamlException e) {
			assertEquals(e.getMessage(), null);
		}
	}

	@Test
	public void exceptionTest2() {
		try {
			throw new YamlException("xxx");
		} catch(YamlException e) {
			assertEquals(e.getMessage(),"xxx");
		}
	}

	@Test
	public void exceptionTest3() {
		IOException t=new IOException("xxx");
		try {
			YamlException.throwException(t);
		} catch(YamlException e) {
			assert(e.getCause()==t);
		}
	}

	@Test
	public void exceptionTest4() {
		YamlException t=new YamlException("xxx");
		try {
			YamlException.throwException(t);
		} catch(YamlException e) {
			assert(e==t);
		}
	}

	@Test
	public void exceptionTest5() {
		YamlException t=new YamlException("xxx");
		try {
			throw new YamlException("yyy", t);
		} catch(YamlException e) {
			assert(e.getCause()==t);
			assertEquals(e.getMessage(),"yyy");
		}
	}
	
	@Test
	public void includeTest1() {
		try {
			Yaml yaml=new Yaml(Mode.ExtendedJson);
			Handler include=new IncludeScript().create(yaml,yaml.createOutput("  ","\n", new ByteArrayOutputStream()));
			include.beginObject(false, Collections.emptyList());
			fail();
		} catch(YamlException t) {
		}
	}

	@Test
	public void includeTest2() {
		try {
			Yaml yaml=new Yaml(Mode.ExtendedJson);
			Handler include=new IncludeScript().create(yaml,yaml.createOutput("  ","\n", new ByteArrayOutputStream()));
			include.endObject(false);
		} catch(YamlException e) {
			fail();
		}
	}

	@Test
	public void includeTest3() {
		try {
			Yaml yaml=new Yaml(Mode.ExtendedJson);
			Handler include=new IncludeScript().create(yaml,yaml.createOutput("  ","\n", new ByteArrayOutputStream()));
			include.scalar(false,Collections.emptyList(),new Token.SimpleToken(Type.Alias, null, "xxx"));
			fail();
		} catch(YamlException t) {
		}
	}

	@Test
	public void includeTest4() {
		try {
			Yaml yaml=new Yaml(Mode.ExtendedJson);
			Handler include=new IncludeScript().create(yaml,yaml.createOutput("  ","\n", new ByteArrayOutputStream()));
			include.scalar(false,Collections.emptyList(),new Token.SimpleToken(Type.Scalar, null, "file:///hopefullyunknown.yaml"));
			fail();
		} catch(YamlException t) {
		}
	}

	@Test
	public void includeTest5() {
		try {
			Yaml yaml=new Yaml(Mode.ExtendedJson);
			Handler include=new IncludeScript().create(yaml,yaml.createOutput("  ","\n", new ByteArrayOutputStream()));
			include.scalar(false,Collections.emptyList(),new Token.SimpleToken(Type.Scalar, null, "cp://hopefullyunknown.yaml"));
			fail();
		} catch(YamlException t) {
		}
	}

	@Test
	public void simpleTokenTest1() {
		assertEquals("script",new Token.SimpleToken(Type.Script, null, "script").getValue());
		assertEquals("anchor",new Token.SimpleToken(Type.Anchor, null, "anchor").getValue());
		assertEquals("alias",new Token.SimpleToken(Type.Alias, null, "alias").getValue());
		Token defaultImpl=new Token() {

			@Override
			public Type getType() {
				return Type.Separator;
			}

			@Override
			public String toString() {
				return "token";
			}
		};
		assertEquals(Type.Separator,defaultImpl.getType());
		assertEquals("token",defaultImpl.getValue());
		assertNull(defaultImpl.getTag());
	}
	
	@Test
	public void anchorTokenTest1() {
		try {
			Token anchor=new Token.SimpleToken(Type.Anchor, null, "a");
			Token value=new Token.SimpleToken(Type.Scalar, null, "xxx");
			Token.DecoratedToken a=value.decorate(anchor);
			assertEquals(Type.DecoratedToken,a.getType());
			assertEquals(Type.Anchor,a.getDecorator().getType());
			assertEquals(a.isNode(),value.isNode());
			assert(!anchor.isNode());
			assert(value.isNode());
			assert(a.getDecorator()==anchor);
			assert(a.getToken()==value);
			assertEquals(a.toString(),"&a \"xxx\"");
			assertEquals(a.getValue(),"&a xxx");
		} catch(Throwable t) {
			fail();
		}
	}

	@Test
	public void scriptTokenTest1() {
		try {
			Token script=new Token.SimpleToken(Type.Script, null, "a");
			Token value=new Token.SimpleToken(Type.Scalar, null, "xxx");
			Token.DecoratedToken a=value.decorate(script);
			assertEquals(a.isNode(),value.isNode());
			assertEquals(Type.DecoratedToken,a.getType());
			assertEquals(Type.Script,a.getDecorator().getType());
			assert(!script.isNode());
			assert(value.isNode());
			assert(a.getDecorator()==script);
			assert(a.getToken()==value);
			assertEquals(a.toString(),"@a \"xxx\"");
			assertEquals(a.getValue(),"@a xxx");
		} catch(Throwable t) {
			fail();
		}
	}

	@Test
	public void decoratorFailureTest1() {
		Token script=new Token.SimpleToken(Type.Scalar, null, "a");
		Token value=new Token.SimpleToken(Type.Scalar, null, "xxx");
		try {
			Token.DecoratedToken a=value.decorate(script);
			fail();
		} catch(YamlException e) {
		}
	}

	@Test
	public void documentFailureTest1() {
		Token value=new Token.SimpleToken(Type.Anchor, null, "xxx");
		try {
			Yaml.Document doc=value.asDocument(new Yaml(Mode.Indented));
			fail();
		} catch(YamlException e) {
		}
	}

	@Test
	public void scriptFailureTest1() {
		YamlScript failureScript=new YamlScript() {

			@Override
			public String getName() {
				return "failure";
			}

			@Override
			public Handler create(Yaml yaml, Handler base) throws YamlException {
				return new Handler() {
					@Override
					public void scalar(boolean key, List<Token> modifier, Token token) throws YamlException {
						throw new YamlException("Scalar failure");
					}

					@Override
					public void beginObject(boolean key, List<Token> modifier) throws YamlException {
						throw new YamlException("beginObject failure");
					}
					
					
				};
			}
		};
		Yaml yaml=new Yaml(Mode.Indented,failureScript);
		try(InputStream in=new ByteArrayInputStream("@failure xxx".getBytes())) {
			yaml.parse(in, Yaml.NOOP);
			fail();
		} catch(IOException e) {
			assertEquals(e.getMessage(),"l.1: Scalar failure");
		}
		try(InputStream in=new ByteArrayInputStream("a: @failure\n  x: y".getBytes())) {
			yaml.parse(in, Yaml.NOOP);
			fail();
		} catch(IOException e) {
			assertEquals(e.getMessage(),"l.2: beginObject failure");
		}
	}
	
	@Test
	public void simpleJsonTest1() {
		ByteArrayOutputStream out=new ByteArrayOutputStream();
		try {
			Handler handler=new JsonOutput(false,"","",out);
			handler.beginDocument();
			handler.scalar(false,OutputTest.modifier(""),new Token.SimpleToken(Type.Scalar, null,"a"));
			handler.endDocument();
		} catch(YamlException t) {
		}
		assertEquals("\"a\"",new String(out.toByteArray()));
	}

	@Test
	public void simpleJsonTest2() {
		ByteArrayOutputStream out=new ByteArrayOutputStream();
		try {
			Handler handler=new JsonOutput(false,"","",out);
			handler.beginDocument();
			handler.beginArray(false, OutputTest.modifier(""));
			handler.scalar(false,OutputTest.modifier(""),new Token.SimpleToken(Type.Scalar, "!int","a"));
			handler.scalar(false,OutputTest.modifier(""),new Token.SimpleToken(Type.Scalar, "!int","{}"));
			handler.endArray(false);
			handler.endDocument();
		} catch(YamlException t) {
		}
		assertEquals("[a,\"{}\"]",new String(out.toByteArray()));
	}
	
	@Test
	public void simpleJsonTest3() {
		ByteArrayOutputStream out=new ByteArrayOutputStream();
		try {
			Handler handler=new JsonOutput(false,"","",out);
			handler.beginDocument();
			handler.beginObject(false, OutputTest.modifier(""));
			handler.scalar(true,OutputTest.modifier(""),new Token.SimpleToken(Type.Scalar, "!int","key"));
			handler.scalar(false,OutputTest.modifier(""),new Token.SimpleToken(Type.Scalar, "!int","100"));
			handler.endObject(false);
			handler.endDocument();
		} catch(YamlException t) {
		}
		assertEquals("{\"key\": 100}",new String(out.toByteArray()));
	}
	
	@Test
	public void versionTest() {
		try(InputStream in=new ByteArrayInputStream("%YAML 2.0".getBytes())) {
			for(Document doc:new Yaml(Mode.Indented).parse(in)) {
				doc.process(Yaml.NOOP);
			}
			fail();
		} catch(IOException e) {
			assertEquals("l.1: Unsupported YAML version: 2.0", e.getMessage());
		}
	}
	
	@Test 
	public void failureTest1() {
		Handler handler=new YamlProcessor.ExceptionDecorator(Mode.Indented,new Line(),new FailureHandler());
		try {
			handler.beginDocument();
			fail();
		} catch(YamlException e) {
			assertEquals("l.1: failure",e.getMessage());
		}
		try {
			handler.endDocument();
			fail();
		} catch(YamlException e) {
			assertEquals("l.1: failure",e.getMessage());
		}
		try {
			handler.beginArray(false,Collections.emptyList());
			fail();
		} catch(YamlException e) {
			assertEquals("l.1: failure",e.getMessage());
		}
		try {
			handler.endArray(false);
			fail();
		} catch(YamlException e) {
			assertEquals("l.1: failure",e.getMessage());
		}
		try {
			handler.beginObject(false,Collections.emptyList());
			fail();
		} catch(YamlException e) {
			assertEquals("l.1: failure",e.getMessage());
		}
		try {
			handler.endObject(false);
			fail();
		} catch(YamlException e) {
			assertEquals("l.1: failure",e.getMessage());
		}
		try {
			handler.scalar(false,"test");
			fail();
		} catch(YamlException e) {
			assertEquals("l.1: failure",e.getMessage());
		}
		try {
			handler.scalar(false,Collections.emptyList(),null);
			fail();
		} catch(YamlException e) {
			assertEquals("l.1: failure",e.getMessage());
		}
	}
	
	@Test
	public void failureTest2() {
		try(InputStream in=new ByteArrayInputStream("a".getBytes())) {
			for(Document doc:new Yaml(Mode.Indented).parse(in)) {
				doc.process(new Yaml.Delegator(Yaml.NOOP) {

					@Override
					public void onError(YamlException e) throws YamlException {
					}

					@Override
					public void scalar(boolean key, List<Token> modifier, Token token) throws YamlException {
						throw new YamlException("My exception");
					}
					
				});
			}
		} catch(IOException e) {
			e.printStackTrace();
		}
	}
	
	private class FailureHandler implements Handler {

		@Override
		public void beginDocument() throws YamlException {
			throw new YamlException("failure");
		}

		@Override
		public void endDocument() throws YamlException {
			throw new YamlException("failure");
		}

		@Override
		public void beginArray(boolean key, List<Token> modifier) throws YamlException {
			throw new YamlException("failure");
		}

		@Override
		public void endArray(boolean key) throws YamlException {
			throw new YamlException("failure");
		}

		@Override
		public void beginObject(boolean key, List<Token> modifier) throws YamlException {
			throw new YamlException("failure");
		}

		@Override
		public void endObject(boolean key) throws YamlException {
			throw new YamlException("failure");
		}

		@Override
		public void scalar(boolean key, List<Token> modifier, Token token) throws YamlException {
			throw new YamlException("failure");
		}

		@Override
		public void scalar(boolean key, String token) throws YamlException {
			throw new YamlException("failure");
		}
	}
	
	@Test
	public void exceptionLineNoTest() {
		YamlException ex=new YamlException("");
		ex.setLine(0, 2, 4);
		assertEquals(2+1, ex.getLineNo());
	}
	
	@Test
	public void defaultTokenizerTest1() {
		Tokenizer tok=new YamlLine.Tokenizer() {
			
			@Override
			public Token next() {
				return null;
			}
			
			@Override
			public boolean hasNext() {
				return false;
			}
			
			@Override
			public void update(Line line) throws IOException {
			}
			
			@Override
			public YamlLine nextLine() throws IOException {
				return null;
			}
			
			@Override
			public boolean isContinued() {
				return false;
			}
			
			@Override
			public void eof() throws IOException {
			}
		};
//		assertEquals(false,tok.isArray());
//		assertEquals(false,tok.isObject());
		assertEquals(false,tok.isFlowMode());
		try {
			tok.peekFlowEntry();
			fail();
		} catch(RuntimeException e) {
		}
		try {
			tok.peekIndentationEntry();
			fail();
		} catch(RuntimeException e) {
		}
	}
	
	@Test
	public void emptyDocument() {
		try(InputStream in=new ByteArrayInputStream("".getBytes())) {
			int c=0;
			for(Document doc:new Yaml(Mode.Indented).parse(in)) {
				c++;
			}
			assertEquals(0, c);
		} catch(Throwable t) {
			
		}
	}
	@Test
	public void endDocument() {
		try(InputStream in=new ByteArrayInputStream("...\nMore text".getBytes())) {
			int c=0;
			for(Document doc:new Yaml(Mode.Indented).parse(in)) {
				c++;
			}
			assertEquals(0, c);
		} catch(Throwable t) {
			
		}
	}
}

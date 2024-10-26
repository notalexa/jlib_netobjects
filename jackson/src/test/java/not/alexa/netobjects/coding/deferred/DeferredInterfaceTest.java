package not.alexa.netobjects.coding.deferred;

import static org.junit.Assert.assertArrayEquals;

import java.io.IOException;

import org.junit.Test;

import com.fasterxml.jackson.annotation.JsonProperty;

import not.alexa.netobjects.BaseException;
import not.alexa.netobjects.Context;
import not.alexa.netobjects.coding.ByteEncoder;
import not.alexa.netobjects.coding.CodingScheme;
import not.alexa.netobjects.coding.Decoder;
import not.alexa.netobjects.coding.json.JsonCodingScheme;
import not.alexa.netobjects.coding.protobuf.ProtobufCodingScheme;
import not.alexa.netobjects.coding.xml.XMLCodingScheme;
import not.alexa.netobjects.types.DefaultTypeLoader;
import not.alexa.netobjects.types.ObjectType;
import not.alexa.netobjects.types.TypeDefinition;

public class DeferredInterfaceTest {

	public DeferredInterfaceTest() {
	}
	
	@Test public void testXML() {
		test(XMLCodingScheme.REST_SCHEME);
	}

	@Test public void testJson() {
		test(JsonCodingScheme.REST_SCHEME);
	}
	@Test public void testProtobuf() {
		test(ProtobufCodingScheme.REST_SCHEME);
	}

	public void test(CodingScheme codingScheme) {
		Data data=new Data("test");
		Context context=Context.createRootContext();
		Context restrictedContext=Context.createRootContext(new DefaultTypeLoader() {
			@Override
			public TypeDefinition resolveType(ObjectType t) {
				if(t.equals(ObjectType.createClassType(ContentData.class))) {
					return null;
				}
				return super.resolveType(t);
			}
		});
		try(ByteEncoder encoder=codingScheme.createEncoder(context)) {
			byte[] content=encoder.encode(data).asBytes();
			System.out.write(content);System.out.println();
			try(Decoder decoder=codingScheme.createDecoder(restrictedContext, content)) {
				Data data1=decoder.decode(Data.class);
				try(ByteEncoder encoder1=codingScheme.createEncoder(context)) {
					byte[] content1=encoder1.encode(data1).asBytes();
					System.out.write(content1);System.out.println();
					assertArrayEquals(content,content1);
				}
			}
		} catch(BaseException|IOException e) {
			e.printStackTrace();
		} catch(Throwable t) {
			t.printStackTrace();
		}
	}

	public static class Data {
		@JsonProperty Content content;
		protected Data() {}
		public Data(String content) {
			this.content=new ContentData(content);
		}
	}
	
	public interface Content {
		public default String displayContent(Context context) {
			return "Display content of default";
		}
	}
	
	public static class ContentData implements Content {
		@JsonProperty String data;
		protected ContentData() {}
		public ContentData(String data) {
			this.data=data;
		}
	}
}

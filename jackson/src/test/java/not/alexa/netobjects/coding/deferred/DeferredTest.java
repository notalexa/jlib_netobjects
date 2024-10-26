package not.alexa.netobjects.coding.deferred;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.fasterxml.jackson.annotation.JsonProperty;

import not.alexa.netobjects.BaseException;
import not.alexa.netobjects.Context;
import not.alexa.netobjects.api.Field;
import not.alexa.netobjects.coding.ByteEncoder;
import not.alexa.netobjects.coding.CodingScheme;
import not.alexa.netobjects.coding.Decoder;
import not.alexa.netobjects.coding.Encoder;
import not.alexa.netobjects.coding.json.JsonCodingScheme;
import not.alexa.netobjects.coding.protobuf.ProtobufCodingScheme;
import not.alexa.netobjects.coding.xml.XMLCodingScheme;
import not.alexa.netobjects.types.DeferredObject;
import not.alexa.netobjects.types.JavaClass;
import not.alexa.netobjects.types.JavaClass.Type;
import not.alexa.netobjects.types.ObjectType;

@RunWith(org.junit.runners.Parameterized.class)
public class DeferredTest {

	public DeferredTest() {
	}
	
    @Parameters
    public static List<Data> testObjects() {
        return Arrays.asList(new Data[] {
        		new Data(2,XMLCodingScheme.REST_SCHEME.newBuilder().setIndent("  ","\n").build()),
        		new Data("deferred1.json",XMLCodingScheme.REST_SCHEME.newBuilder().setIndent("  ","\n").build()),
        		new Data(new TestData(1,"hello"),XMLCodingScheme.REST_SCHEME.newBuilder().setIndent("  ","\n").build()),
        		new Data(2,JsonCodingScheme.REST_SCHEME.newBuilder().setIndent("  ","\n").build()),
        		new Data("deferred1.json",JsonCodingScheme.REST_SCHEME.newBuilder().setIndent("  ","\n").build()),
        		new Data(new TestData(1,"hello"),JsonCodingScheme.REST_SCHEME.newBuilder().setIndent("  ","\n").build()),
        		new Data(2,ProtobufCodingScheme.REST_SCHEME),
        		new Data("deferred1.json",ProtobufCodingScheme.REST_SCHEME),
        		new Data(new TestData(1,"hello"),ProtobufCodingScheme.REST_SCHEME),
        });
    }
    
	@Parameter public Data data;
	
	private Any decode(Context context,byte[] content) throws BaseException {
		try(Decoder decoder=data.scheme.createDecoder(context, content)) {
			return decoder.decode(Any.class);
		}
	}
	
	@Test public void test1() {
		Context context=Context.createRootContext();
		try(ByteEncoder encoder=data.scheme.createEncoder(context)) {
			encoder.encode(new Any(data.o));
			System.out.println(encoder.toString());
			try(Decoder decoder=data.scheme.createDecoder(context, encoder.asBytes())) {
				Any any=decoder.decode(Any.class);
				try(ByteEncoder encoder2=data.scheme.createEncoder(context)) {
					encoder2.encode(any);
					System.out.println(encoder2.toString());
					Any any2=decode(context,encoder2.asBytes());
				}
				any.resolve(context);
				try(ByteEncoder encoder2=data.scheme.createEncoder(context)) {
					encoder2.encode(any);
					System.out.println(encoder2.toString());
					Any any2=decode(context,encoder2.asBytes());
				}
			}
		} catch(Throwable t) {
			t.printStackTrace();
		}
	}
	
	public static class Any {
		@JsonProperty ObjectType type;
		@JsonProperty DeferredObject content;
		@JsonProperty DeferredObject[] listContent;
		Any() {
		}
		Any(Object o) {
			type=ObjectType.createClassType(o.getClass());
			content=new DeferredObject(o);
			listContent=new DeferredObject[] { content, content };
		}
		void resolve(Context context) throws BaseException {
			Class<?> clazz=((Type)type).asLinkedLocal(context.getTypeLoader().getClassLoader()).asClass();
			content.get(clazz);
			for(DeferredObject o:listContent) {
				o.get(clazz);
			}
		}
	}
	
	public static class Data {
		Object o;
		CodingScheme scheme;
		public Data(Object o,CodingScheme scheme) {
			this.o=o;
			this.scheme=scheme;
		}
	}
	
	public static class TestData {
		@Field(type="xml",name="@id")
		@JsonProperty int id;
		@JsonProperty String value;
		TestData() {}
		TestData(int id,String value) {
			this.id=id;
			this.value=value;
		}
	}
}

package not.alexa.netobjects.coding.deferred;

import java.io.File;

import org.junit.Test;

import com.fasterxml.jackson.annotation.JsonProperty;

import not.alexa.netobjects.Context;
import not.alexa.netobjects.coding.Decoder;
import not.alexa.netobjects.coding.protobuf.ProtobufCodingScheme;
import not.alexa.netobjects.coding.xml.XMLCodingScheme;
import not.alexa.netobjects.coding.yaml.YamlCodingScheme;
import not.alexa.netobjects.types.Deferred;
import not.alexa.netobjects.types.Namespace;
import not.alexa.netobjects.types.ObjectType;
import not.alexa.netobjects.types.TypeDefinition;
import not.alexa.netobjects.types.access.Access;
import not.alexa.netobjects.types.access.AccessContext;
import not.alexa.netobjects.types.access.AccessFactory;

public class DeferredDataObjectTest {

	public DeferredDataObjectTest() {
		// TODO Auto-generated constructor stub
	}
	
	@Test public void fileTest() {
		Context context=Context.createRootContext();
		TypeDefinition def=context.resolveType(FileData.class);
		try {
			System.out.write(XMLCodingScheme.DEFAULT_SCHEME.newBuilder().setIndent("  ","\n").build().createEncoder(context).encode(def).asBytes());
		} catch(Throwable t) {
			t.printStackTrace();
		}
	}
	
	@Test public void encodingTest1() {
		Context context=Context.createRootContext();
		FileData data=new FileData(new File("test"));
		try {
			byte[] content=YamlCodingScheme.DEFAULT_SCHEME.createEncoder(context).encode(data).asBytes();
			System.out.write(content);
			try(Decoder decoder=YamlCodingScheme.DEFAULT_SCHEME.createDecoder(context, content)) {
				FileData fileData=decoder.decode(FileData.class);
				System.out.println(fileData);
			}
		} catch(Throwable t) {
			t.printStackTrace();
		}
	}
	@Test public void encodingTest2() {
		Context context=Context.createRootContext();
		FileData data=new FileData(new File("test"));
		try {
			byte[] content=XMLCodingScheme.DEFAULT_SCHEME.newBuilder().setIndent("  ","\n").build().createEncoder(context).encode(data).asBytes();
			System.out.write(content);
			try(Decoder decoder=XMLCodingScheme.DEFAULT_SCHEME.createDecoder(context, content)) {
				FileData fileData=decoder.decode(FileData.class);
				System.out.println(fileData);
			}
		} catch(Throwable t) {
			t.printStackTrace();
		}
	}
	@Test public void encodingTest3() {
		Context context=Context.createRootContext();
		FileData data=new FileData(new File("test"));
		try {
			byte[] content=ProtobufCodingScheme.DEFAULT_SCHEME.createEncoder(context).encode(data).asBytes();
			System.out.write(content);
			try(Decoder decoder=ProtobufCodingScheme.DEFAULT_SCHEME.createDecoder(context, content)) {
				FileData fileData=decoder.decode(FileData.class);
				System.out.println(fileData);
			}
		} catch(Throwable t) {
			t.printStackTrace();
		}
	}
	
	public static class FileData {
		@JsonProperty Class<?> c;
		@JsonProperty File f;
		@JsonProperty A a;
		protected FileData() {}
		protected FileData(File f) {
			c=FileData.class;
			this.f=f;
			this.a=new A(f);
		}
	}
	
	public static class A implements Deferred<AData,AData> {
		File f;
		A(File f) {
			this.f=f;
		}

		@Override
		public <R extends AData> AData getCodingObject(AccessContext context) {
			return new AData(f);
		}

		@Override
		public boolean isResolved() {
			return true;
		}

		@Override
		public ObjectType getObjectType(Namespace ns) {
			return ns==Namespace.getJavaNamespace()?ObjectType.createClassType(A.class):null;
		}

		@Override
		public Object makeProxy(Class<?> clazz) {
			return null;
		}

		@Override
		public Access getCodingAccess(AccessContext context, AccessFactory factory) {
			return factory.resolve(context.getContext(),context.getContext().resolveType(AData.class));
		}
		
	}
	
	public static class AData {
		@JsonProperty File f;
		AData() {}
		AData(File f) {
			this.f=f;
		}
		
		Object finish(AccessContext context) {
			return new A(f);
		}
	}
}

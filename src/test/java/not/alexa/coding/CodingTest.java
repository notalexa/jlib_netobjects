package not.alexa.coding;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.io.ByteArrayOutputStream;

import not.alexa.coding.Data.State;
import not.alexa.netobjects.Context;
import not.alexa.netobjects.api.Overlay;
import not.alexa.netobjects.coding.Decoder;
import not.alexa.netobjects.coding.Encoder;
import not.alexa.netobjects.coding.xml.XMLCodingScheme;
import not.alexa.netobjects.types.DefaultTypeLoader;
import not.alexa.netobjects.types.ObjectType;
import not.alexa.netobjects.types.PrimitiveTypeDefinition;
import not.alexa.netobjects.types.TypeLoader;

public class CodingTest {

	public CodingTest() {
		// TODO Auto-generated constructor stub
	}

	//@Test
	public void test() throws Throwable {
		XMLCodingScheme scheme=XMLCodingScheme.builder().setIndent("  ","\r\n").setRootTag("root").build();//new XMLCodingScheme();
		Context context=Context.createRootContext(new DefaultTypeLoader());
		//System.out.println(context.getTypeLoader().resolveType(PrimitiveTypeDefinition.class));
//		try(ByteEncoder encoder=scheme.createEncoder(context)) {
//	//		System.out.write(encoder.encode(Data.getTypeDescription()).asBytes());System.out.println();
//		}
//		System.out.println(Namespace.get("oid:2.3.1.24.2"));
		ObjectType.createClassType(PrimitiveTypeDefinition.class);
		for(Object o:new Object[] {new not.alexa.coding.Data("Hello World",100,"T1","T2"),
				"\"Hello&World\"",Data.State.active,
				1,
				ObjectType.createClassType(PrimitiveTypeDefinition.class),
				ObjectType.resolve("oid:2.3.1.24.2")}) try(ByteArrayOutputStream out=new ByteArrayOutputStream();
			Encoder encoder=scheme.createEncoder(context, out)) {
			encoder.encode(o).flush();
			System.out.write(out.toByteArray());System.out.println();
			try(Decoder decoder=scheme.createDecoder(context, out.toByteArray())) {
				Object decoded=decoder.decode(Object.class);
				System.out.println(decoded);
				try(ByteArrayOutputStream out2=new ByteArrayOutputStream();
						Encoder encoder2=scheme.createEncoder(context, out2)) {
					encoder2.encode(decoded).flush();
					System.out.write(out2.toByteArray());System.out.println();
				}
			}
		}
		System.out.println(State[].class.getCanonicalName());
		System.out.println(Byte.TYPE.getCanonicalName());
		System.out.println(String.class.getCanonicalName());
		System.out.println(String[][].class.getName());
	}
	
    @Test
    public void overlayTest() throws Throwable {
        XMLCodingScheme scheme=XMLCodingScheme.builder().setIndent("  ","\r\n").setRootTag("root").build();//new XMLCodingScheme();
        DefaultTypeLoader resolver=new DefaultTypeLoader();
        Context context=Context.createRootContext(resolver);
        Context overlayContext=Context.createRootContext(resolver.overlay(DataOverlay.class));
        ObjectType.createClassType(PrimitiveTypeDefinition.class);
        for(Object o:new Object[] {new not.alexa.coding.Data("Hello World",100,"T1","T2")}) try(ByteArrayOutputStream out=new ByteArrayOutputStream();
            Encoder encoder=scheme.createEncoder(context, out)) {
            encoder.encode(o).flush();
            System.out.write(out.toByteArray());System.out.println();
            try(Decoder decoder=scheme.createDecoder(overlayContext, out.toByteArray())) {
                Object decoded=decoder.decode(Object.class);
                assertEquals(DataOverlay.class,decoded.getClass());
                System.out.println(decoded);
                try(ByteArrayOutputStream out2=new ByteArrayOutputStream();
                        Encoder encoder2=scheme.createEncoder(context, out2)) {
                    encoder2.encode(decoded).flush();
                    System.out.write(out2.toByteArray());System.out.println();
                }
            }
        }
    }
    
    @Overlay
    public static class DataOverlay extends Data {
        
    }
}

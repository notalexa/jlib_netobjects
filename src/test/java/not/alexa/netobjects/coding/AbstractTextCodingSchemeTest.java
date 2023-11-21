package not.alexa.netobjects.coding;

import static org.junit.Assert.assertEquals;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import not.alexa.netobjects.BaseException;
import not.alexa.netobjects.Context;
import not.alexa.netobjects.types.AccessibleObject;
import not.alexa.netobjects.types.JavaClass.Type;
import not.alexa.netobjects.types.Namespace;
import not.alexa.netobjects.types.PrimitiveTypeDefinition;
import not.alexa.netobjects.types.TypeDefinition;
import not.alexa.netobjects.types.access.Access;
import not.alexa.netobjects.types.access.Access.SimpleTypeAccess;
import not.alexa.netobjects.types.access.AccessContext;
import not.alexa.netobjects.types.access.AccessFactory;
import not.alexa.netobjects.types.access.Constructor;

@RunWith(org.junit.runners.Parameterized.class)
public class AbstractTextCodingSchemeTest {
    private static DummyAccessFactory ACCESS=new DummyAccessFactory();
    private static DummyAccessFactory ACCESS2=new DummyAccessFactory();

	@Parameters
	public static List<TestCase> primitiveClasses() {
		return Arrays.asList(new TestCase[] {
		        new TestCase(TestScheme.DEFAULT_INSTANCE,"class",null,"","",PrimitiveTypeDefinition.getTypeDescription(Object.class),Charset.forName("UTF-8"),"mimetype","dat",ACCESS),
                new TestCase(TestScheme.DEFAULT_INSTANCE.newBuilder()
                        .setCharset("ISO-8859-1").build(),"class",null,"","",PrimitiveTypeDefinition.getTypeDescription(Object.class),Charset.forName("ISO-8859-1"),"mimetype","dat",ACCESS),
                new TestCase(TestScheme.DEFAULT_INSTANCE.newBuilder()
                        .setResourceBranch("properties")
                        .setIndent("  ", "\r\n")
                        .setCharset(Charset.forName("ISO-8859-1"))
                        .setTypeInfos("text/xml","xml")
                        .setAccessFctory(ACCESS2)
                        .setRootType(PrimitiveTypeDefinition.getTypeDescription(String.class))
                        .setNamespace("clazz",Namespace.getJavaNamespace()).build(),"clazz","properties","  ","\r\n",PrimitiveTypeDefinition.getTypeDescription(String.class),Charset.forName("ISO-8859-1"),"text/xml","xml",ACCESS2),
                new TestCase(TestScheme.DEFAULT_INSTANCE,"class",null,"","",PrimitiveTypeDefinition.getTypeDescription(Object.class),Charset.forName("UTF-8"),"mimetype","dat",ACCESS)
		});
	}
	
	@Parameter
	public TestCase testCase;
	
	public AbstractTextCodingSchemeTest() {
	}
	
	@Test
	public void checkAttribute() {
	    assertEquals("TypeRef",testCase.typeRef,testCase.scheme.getTypeRef());
        assertEquals("ResourceBranch",testCase.resourceBranch,testCase.scheme.getResourceBranch());
        assertEquals("Indent",testCase.indent,testCase.scheme.getIndent());
        assertEquals("LineTerminator",testCase.lineTerminator,testCase.scheme.getLineTerminator());	    
        assertEquals("RootType",testCase.rootType,testCase.scheme.getRootType());     
        assertEquals("Charset",testCase.charset,testCase.scheme.getCodingCharset());     
        assertEquals("Namespace",Namespace.getJavaNamespace(),testCase.scheme.getNamespace());
        assertEquals("MimeType",testCase.mimeType,testCase.scheme.getMimeType());
        assertEquals("FileExtension",testCase.fileExtension,testCase.scheme.getFileExtension());
        assertEquals("AccessFactory",testCase.accessFactory,testCase.scheme.getFactory());
	}
	    
	private static class TestCase {
	    TestScheme scheme;
	    String typeRef;
	    String indent;
	    String lineTerminator;
	    String resourceBranch;
	    TypeDefinition rootType;
        Charset charset;
        String mimeType;
        String fileExtension;
        AccessFactory accessFactory;
	    public TestCase(TestScheme scheme,String typeRef,String resourceBranch,String indent,String lineTerminator,TypeDefinition rootType,Charset charset,String mimeType,String fileExtension,AccessFactory accessFactory) {
	        this.scheme=scheme;
	        this.typeRef=typeRef;
	        this.resourceBranch=resourceBranch;
	        this.indent=indent;
	        this.lineTerminator=lineTerminator;
	        this.rootType=rootType;
	        this.charset=charset;
	        this.mimeType=mimeType;
	        this.fileExtension=fileExtension;
	        this.accessFactory=accessFactory;
	    }
	}
	
	public static class TestScheme extends AbstractTextCodingScheme {
	    public static final TestScheme DEFAULT_INSTANCE=new TestScheme(Charset.forName("UTF-8"),ACCESS);
	    boolean walkThrough;
	    
	    public TestScheme(Charset charset, AccessFactory factory) {
            super(charset, factory);
            mimeType="mimetype";
            fileExtension="dat";
        }

        public Builder newBuilder() {
	        return new Builder(this);
	    }

        @Override
        public Encoder createEncoder(Context context, OutputStream stream) {
            return null;
        }

        @Override
        public Decoder createDecoder(Context context, InputStream stream) {
            return null;
        }
        
        public static class Builder extends AbstractTextCodingScheme.Builder<TestScheme, Builder> {

            public Builder(TestScheme scheme) {
                super(scheme);
            }

            @Override
            public Builder myself() {
                return this;
            }
        }
 	}
	
	static class DummyAccessFactory implements AccessFactory {
	    
        @Override
        public Access resolve(Context context, TypeDefinition type) {
            switch(type.getFlavour()) {
                case PrimitiveType:return new SimpleTypeAccess(this,type);
                default:return  new DummyAccess(type);
            }
        }

        @Override
        public Access resolve(Access referrer, TypeDefinition type) {
            switch(type.getFlavour()) {
                case PrimitiveType:return new SimpleTypeAccess(this,type);
                default:return new DummyAccess(type);
            }
        }
        public class DummyAccess implements Access {
            TypeDefinition type;
            DummyAccess(TypeDefinition type) {
                this.type=type;
            }

            @Override
            public TypeDefinition getType() {
                return type;
            }

            @Override
            public AccessibleObject newAccessible(AccessContext context) throws BaseException {
                return null;
            }

            @Override
            public AccessFactory getFactory() {
                return DummyAccessFactory.this;
            }
        }
        @Override
        public Constructor resolve(Context context, Type type) {
            return null;//new ConstructorHelper.DefaultConstructor(type.asLinkedLocal(context.getTypeLoader().getClassLoader()));
        }
	}
}

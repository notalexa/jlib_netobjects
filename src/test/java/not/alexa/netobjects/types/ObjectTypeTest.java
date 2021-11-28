package not.alexa.netobjects.types;

import static org.junit.Assert.fail;

import org.junit.Test;

import not.alexa.coding.Data;
import not.alexa.netobjects.BaseException;
import not.alexa.netobjects.Context;
import not.alexa.netobjects.HelloWorldService;
import not.alexa.netobjects.api.Overlay;
import not.alexa.netobjects.coding.xml.XMLCodingScheme;

public class ObjectTypeTest {

    public ObjectTypeTest() {
    }
    
    private Context createContext() {
        TypeLoader loader=new DefaultTypeLoader();
        return new Context.Root() {

            @Override
            public TypeLoader getTypeLoader() {
                return loader;
            }
            
        };
    }
    
    private Context createOverlayContext() {
        TypeLoader loader=new DefaultTypeLoader().overlay(C.class,ClassLoaderTest.defineOverlayClass());
        return new Context.Root() {

            @Override
            public TypeLoader getTypeLoader() {
                return loader;
            }
            
        };
    }
    
    @Test
    public void test() {
        Context context=createContext();
        Context overlayContext=createOverlayContext();
        ObjectType type1=ObjectType.resolve("jvm:not.alexa.coding.Data::1a833da6-3a6b-3e20-898d-ae06d06602e1");
        ObjectType type2=ObjectType.resolve("jvm:not.alexa.coding.Data::hello-universe");
        try {
            new HelloWorldService() {
            }.helloWorld(context, "test");
            fail();
        } catch(BaseException e) {
            //e.printStackTrace();
        }
        System.out.println(context.getTypeLoader().resolveType(type1));
        System.out.println(context.getTypeLoader().resolveType(type2));
        Data d1=new Data("textA",1,"A");
        Data d2=new Data("textB",2,"B");
        d2.setRef(new Lambda(d1,type1,d1,d2));
        for(Lambda lambda:new Lambda[] {
                new Lambda(new Data("text",1,"x","y","z"),type1),
                new Lambda(d1.setRef(d2),type2)
        }) try {
            byte[] serialized=XMLCodingScheme.DEFAULT_SCHEME.newBuilder().setIndent("  ","\n").build().createEncoder(context).encode(lambda).asBytes();
//            System.out.write(serialized);
            Lambda l=XMLCodingScheme.DEFAULT_SCHEME.createDecoder(overlayContext, serialized).decode(Lambda.class);
            System.out.write(XMLCodingScheme.DEFAULT_SCHEME.newBuilder().setIndent("  ","\n").build().createEncoder(context).encode(lambda).asBytes());
            System.out.println();
            System.out.println(lambda.<Object>call(context));
            System.out.println(lambda.<Object>call(overlayContext));
            System.out.println(l.<Object>call(context));
            System.out.println(l.<Object>call(overlayContext));
        } catch(Throwable t) {
            t.printStackTrace();
        }
    }

    @Overlay
    public static class A extends Lambda {

        @Override
        protected boolean callService(Context context) {
            // TODO Auto-generated method stub
            return true;//super.callService(context);
        }

        @Override
        protected <T> T invokeService(Context context, boolean implicit) throws BaseException {
            // TODO Auto-generated method stub
            return null;//super.invokeService(context, implicit);
        }
        
    }
    
    public static class B {
        public String test() {
            return "B";
        }
        public class B1 {
            public String toString() {
                return getClass()+"->"+test();
            }
        }
    }
    @Overlay
    public static class C extends B{
        public String test() {
            return "C";
        }
    }
}

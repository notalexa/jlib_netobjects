package not.alexa.netobjects.types;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;

import not.alexa.coding.Data;
import not.alexa.netobjects.Context;
import not.alexa.netobjects.coding.Decoder;
import not.alexa.netobjects.coding.Encoder;
import not.alexa.netobjects.coding.xml.XMLCodingScheme;
import not.alexa.netobjects.types.JavaClass.Type;
import not.alexa.netobjects.types.access.DefaultAccessFactory;

public class ClassLoaderTest {
    private static int ITERATIONS=1000;
    private String object1="<root class=\"not.alexa.coding.Data\" index=\"100\" state=\"active\">\r\n" + 
            "  <text>Hello World</text>\r\n" + 
            "  <ref obj-ref=\"0\"/>\r\n" + 
            "  <data obj-ref=\"0\"/>\r\n" + 
            "  <list>T1</list>\r\n" + 
            "  <list>T2</list>\r\n" + 
            "  <matrix>\r\n" + 
            "    <matrix>T1</matrix>\r\n" + 
            "    <matrix>T2</matrix>\r\n" + 
            "  </matrix>\r\n" + 
            "  <matrix>\r\n" + 
            "    <matrix>T1</matrix>\r\n" + 
            "    <matrix>T2</matrix>\r\n" + 
            "  </matrix>\r\n" + 
            "  <map k=\"T1\">\r\n" + 
            "    <v>0</v>\r\n" + 
            "  </map>\r\n" + 
            "  <map k=\"T2\">\r\n" + 
            "    <v>1</v>\r\n" + 
            "  </map>\r\n" + 
            "</root>\r\n";
    private String object2="<root class=\"not.alexa.netobjects.types.overlay.Data2\" index=\"100\" state=\"active\">\r\n" + 
            "  <text>Hello World</text>\r\n" + 
            "  <ref obj-ref=\"0\"/>\r\n" + 
            "  <data obj-ref=\"0\"/>\r\n" + 
            "  <list>T1</list>\r\n" + 
            "  <list>T2</list>\r\n" + 
            "  <matrix>\r\n" + 
            "    <matrix>T1</matrix>\r\n" + 
            "    <matrix>T2</matrix>\r\n" + 
            "  </matrix>\r\n" + 
            "  <matrix>\r\n" + 
            "    <matrix>T1</matrix>\r\n" + 
            "    <matrix>T2</matrix>\r\n" + 
            "  </matrix>\r\n" + 
            "  <map k=\"T1\">\r\n" + 
            "    <v>0</v>\r\n" + 
            "  </map>\r\n" + 
            "  <map k=\"T2\">\r\n" + 
            "    <v>1</v>\r\n" + 
            "  </map>\r\n" + 
            "</root>\r\n";
    
    public ClassLoaderTest() {
    }
    
    static ClassLoader createClassLoader() {
        try {
            File f=new File("src/test/libs/loadertest.jar");
            if(f.exists()) {
                return new URLClassLoader(new URL[] { f.toURL() } , ClassLoaderTest.class.getClassLoader());
            } else {
                System.err.println("Install loadertest lib");
            }
        } catch(Throwable t) {
            t.printStackTrace();
        }
        return null;
    }

    static Class<?> defineOverlayClass() {
        try {
            return Class.forName("not.alexa.netobjects.types.overlay.DataOverlay",true,createClassLoader());
        } catch(Throwable t) {
            t.printStackTrace();
        }
        return null;
    }

    private Class<?> defineOverlayClass2(ClassLoader cl) {
        try {
            return Class.forName("not.alexa.netobjects.types.overlay.DataOverlay2",true,cl);
        } catch(Throwable t) {
            t.printStackTrace();
        }
        return null;
    }

    @Test
    public void loaderTest1() throws Throwable {
        TestAccessFactory factory=new TestAccessFactory();
        XMLCodingScheme scheme=XMLCodingScheme.builder().setAccessFctory(factory).setIndent("  ","\r\n").setRootTag("root").build();
        for(int i=0;i<ITERATIONS;i++) {
            Context context=Context.createRootContext(new DefaultTypeLoader());
            try(Decoder decoder=scheme.createDecoder(context, object1.getBytes())) {
                Object decoded=decoder.decode(Object.class);
                try(ByteArrayOutputStream out=new ByteArrayOutputStream();
                        Encoder encoder=scheme.createEncoder(context, out)) {
                    encoder.encode(decoded).flush();
                    assertEquals(object1.trim(),out.toString().trim());
                }
            }
        }
        for(int i=0;i<3;i++) {
            factory.update();
            System.gc();
            Thread.sleep(100);
        }
        // All type maps should be garbage collected
        assertEquals(0,factory.update());
    }

    @Test
    public void overlayTest1() throws Throwable {
        TestAccessFactory factory=new TestAccessFactory();
        XMLCodingScheme scheme=XMLCodingScheme.builder().setAccessFctory(factory).setIndent("  ","\r\n").setRootTag("root").build();
        DefaultTypeLoader resolver=new DefaultTypeLoader();
        Context context=Context.createRootContext(resolver);
        Type type=ObjectType.createClassType(Data.class);
        for(int i=0;i<ITERATIONS;i++) {
            Class<?> overlayClass=defineOverlayClass();
            TypeLoader loader=resolver.overlay(overlayClass);
            Context overlayContext=Context.createRootContext(loader);
            assertTrue(loader.hasOverlays(Data.class));
            assertTrue(type.hasOverlays());
            assertEquals(overlayClass, overlayContext.getTypeLoader().getLinkedClass(type));
            try(Decoder decoder=scheme.createDecoder(overlayContext, object1.getBytes())) {
                Object decoded=decoder.decode(Object.class);
                assertEquals(overlayClass,decoded.getClass());
                try(ByteArrayOutputStream out=new ByteArrayOutputStream();
                        Encoder encoder=scheme.createEncoder(context, out)) {
                    encoder.encode(decoded).flush();
                    assertEquals(object1.trim(),out.toString().trim());
                }
            }           
        }
        for(int i=0;i<3;i++) {
            factory.update();
            System.gc();
            Thread.sleep(100);
        }
        type.createInstanceSupport(Data.class.getClassLoader(),Data.class);
        // All type maps (except the global one) should be garbage collected
        assertEquals(1,factory.update());
        // All overlays should be garbage collected
        assertFalse(type.hasOverlays());
    }

    @Test
    public void loaderTest2() throws Throwable {
        TestAccessFactory factory=new TestAccessFactory();
        XMLCodingScheme scheme=XMLCodingScheme.builder().setAccessFctory(factory).setIndent("  ","\r\n").setRootTag("root").build();
        for(int i=0;i<ITERATIONS;i++) {
            TypeLoader resolver=new DefaultTypeLoader(createClassLoader());
            Context context=Context.createRootContext(resolver);
            try(Decoder decoder=scheme.createDecoder(context, object2.getBytes())) {
                Object decoded=decoder.decode(Object.class);
                try(ByteArrayOutputStream out=new ByteArrayOutputStream();
                        Encoder encoder=scheme.createEncoder(context, out)) {
                    encoder.encode(decoded).flush();
                    assertEquals(object2.trim(),out.toString().trim());
                }
            }
        }
        for(int i=0;i<3;i++) {
            factory.update();
            System.gc();
            Thread.sleep(100);
        }
        // All type maps should be garbage collected
        assertEquals(0,factory.update());
    }

    @Test
    public void overlayTest2() throws Throwable {
        TestAccessFactory factory=new TestAccessFactory();
        XMLCodingScheme scheme=XMLCodingScheme.builder().setAccessFctory(factory).setIndent("  ","\r\n").setRootTag("root").build();
        Type type=ObjectType.createClassType("not.alexa.netobjects.types.overlay.Data2");
        for(int i=0;i<ITERATIONS;i++) {
            TypeLoader resolver=new DefaultTypeLoader(createClassLoader());//resolver.overlay(overlayClass);
            Context context=Context.createRootContext(resolver);
            Class<?> overlayClass=defineOverlayClass2(context.getTypeLoader().getClassLoader());
            TypeLoader loader=resolver.overlay(overlayClass);
            Context overlayContext=Context.createRootContext(loader);
            assertEquals(type.getClassName(), context.getTypeLoader().getLinkedClass(type).getName());
            assertTrue(type.hasOverlays());
            try(Decoder decoder=scheme.createDecoder(overlayContext, object2.getBytes())) {
                Object decoded=decoder.decode(Object.class);
                assertEquals(overlayClass,decoded.getClass());
                try(ByteArrayOutputStream out=new ByteArrayOutputStream();
                        Encoder encoder=scheme.createEncoder(context, out)) {
                    encoder.encode(decoded).flush();
                    assertEquals(object2.trim(),out.toString().trim());
                }
            }
        }
        for(int i=0;i<3;i++) {
            factory.update();
            System.gc();
            Thread.sleep(100);
        }
        type.createInstanceSupport(Data.class.getClassLoader(),Data.class);
        // All type maps should be garbage collected
        assertEquals(0,factory.update());
        // All overlays should be garbage collected
        assertFalse(type.hasOverlays());
    }

    public static class TestAccessFactory extends DefaultAccessFactory {
        public int update() {
            return gc();
        }
    }
}

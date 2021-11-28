package not.alexa.netobjects.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import org.junit.Test;

import java.lang.reflect.Method;

import not.alexa.netobjects.api.Final;
import not.alexa.netobjects.api.NetworkObject;
import not.alexa.netobjects.api.Overlay;
import not.alexa.netobjects.types.JavaClass;

public class TypeUtilsTest {

    public TypeUtilsTest() {
    }

    @Test
    public void testClassOverlay() {
        assertEquals(A1.class, TypeUtils.resolve(A1.class));
        assertEquals(A1.class, TypeUtils.resolve(O1.class));
        assertEquals(A1.class, TypeUtils.resolve(O2.class));
        assertEquals(A1.class, TypeUtils.resolve(O3.class));
    }
    
    @Test
    public void testArrayOverlay1() {
        assertEquals(A1[].class, TypeUtils.resolve(A1[].class));
        assertEquals(A1[].class, TypeUtils.resolve(O1[].class));
        assertEquals(A1[].class, TypeUtils.resolve(O2[].class));
        assertEquals(A1[].class, TypeUtils.resolve(O3[].class));
    }
    
    @Test
    public void testArrayOverlay2() {
        assertEquals(A1[][].class, TypeUtils.resolve(A1[][].class));
        assertEquals(A1[][].class, TypeUtils.resolve(O1[][].class));
        assertEquals(A1[][].class, TypeUtils.resolve(O2[][].class));
        assertEquals(A1[][].class, TypeUtils.resolve(O3[][].class));
    }

    @Test
    public void testFailureCase1() {
        try {
            TypeUtils.resolve(O4.class);
            fail();
        } catch(RuntimeException e) {
        }
    }
    @Test
    public void testFailureCase2() {
        try {
            TypeUtils.resolve(O5.class);
            fail();
        } catch(RuntimeException e) {
        }
    }
    
    @Test
    public void testFailureCase3() {
        try {
            TypeUtils.resolve(O6.class);
            fail();
        } catch(RuntimeException e) {
        }
    }
    
    @Test
    public void testNetworkObject1() {
        assertNull(TypeUtils.getNetworkObject(JavaClass.getJavaNamespace(),A1.class));
        assertNotNull(TypeUtils.getNetworkObject(JavaClass.getJavaNamespace(),A2.class));
        Method m1=new A1().f().getClass().getEnclosingMethod();
        Method m2=new A2().f().getClass().getEnclosingMethod();
        assertNull(TypeUtils.getNetworkObject(JavaClass.getJavaNamespace(),m1));
        assertNotNull(TypeUtils.getNetworkObject(JavaClass.getJavaNamespace(),m2));
    }

    public static interface I1 {}
    public static class A1 implements I1 {
        public Object f() {
            return new Object() {};
        }

    }
    @NetworkObject(ns="test") @NetworkObject public static class A2 implements I1 {
        @NetworkObject(ns="test") @NetworkObject public Object f() {
            return new Object() {};
        }
    }
    @Final public static class A3 implements I1 {}

    @Overlay
    public static class O1 extends A1 {}
    
    @Overlay(A1.class)
    public static class O2 extends A1 {}
    
    @Overlay(A1.class)
    public static class O3 extends O1 {}
    @Overlay(A1.class)
    public static class O4 extends A2 {}
    @Overlay(I1.class)
    public static class O5 extends A2 {}
    @Overlay
    public static class O6 extends A3 {}
}

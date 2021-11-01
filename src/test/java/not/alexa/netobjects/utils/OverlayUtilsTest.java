package not.alexa.netobjects.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Test;

import not.alexa.netobjects.api.Final;
import not.alexa.netobjects.api.Overlay;

public class OverlayUtilsTest {

    public OverlayUtilsTest() {
    }

    @Test
    public void testClassOverlay() {
        assertEquals(A1.class, OverlayUtils.resolve(A1.class));
        assertEquals(A1.class, OverlayUtils.resolve(O1.class));
        assertEquals(A1.class, OverlayUtils.resolve(O2.class));
        assertEquals(A1.class, OverlayUtils.resolve(O3.class));
    }
    
    @Test
    public void testArrayOverlay1() {
        assertEquals(A1[].class, OverlayUtils.resolve(A1[].class));
        assertEquals(A1[].class, OverlayUtils.resolve(O1[].class));
        assertEquals(A1[].class, OverlayUtils.resolve(O2[].class));
        assertEquals(A1[].class, OverlayUtils.resolve(O3[].class));
    }
    
    @Test
    public void testArrayOverlay2() {
        assertEquals(A1[][].class, OverlayUtils.resolve(A1[][].class));
        assertEquals(A1[][].class, OverlayUtils.resolve(O1[][].class));
        assertEquals(A1[][].class, OverlayUtils.resolve(O2[][].class));
        assertEquals(A1[][].class, OverlayUtils.resolve(O3[][].class));
    }

    @Test
    public void testFailureCase1() {
        try {
            OverlayUtils.resolve(O4.class);
            fail();
        } catch(RuntimeException e) {
        }
    }
    @Test
    public void testFailureCase2() {
        try {
            OverlayUtils.resolve(O5.class);
            fail();
        } catch(RuntimeException e) {
        }
    }
    
    @Test
    public void testFailureCase3() {
        try {
            OverlayUtils.resolve(O6.class);
            fail();
        } catch(RuntimeException e) {
        }
    }

    public static interface I1 {}
    public static class A1 implements I1 {}
    public static class A2 implements I1 {}
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

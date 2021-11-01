package not.alexa.netobjects.types;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import org.junit.Test;

import not.alexa.netobjects.api.Overlay;
import not.alexa.netobjects.types.JavaClass.Type;

public class JavaClassTest {

    public JavaClassTest() {
    }
    
    @Test
    public void asStringTest() {
        assertEquals(JavaClassTest.class.getName(), Namespace.asString(JavaClassTest.class));
        assertEquals(JavaClassTest.class.getName()+"[]", Namespace.asString(JavaClassTest[].class));
        assertEquals(JavaClassTest.class.getName()+"[][]", Namespace.asString(JavaClassTest[][].class));
        assertEquals(A1.class.getName(), Namespace.asString(A1.class));
        assertEquals(A1.class.getName()+"[]", Namespace.asString(A1[].class));
        assertEquals(A1.class.getName()+"[][]", Namespace.asString(A1[][].class));
    }

    @Test
    public void typeTest1() {
        assertEquals(ObjectType.createClassType(JavaClassTest.class.getName()),ObjectType.createClassType(JavaClassTest.class));
        assertEquals(Namespace.getJavaNamespace().create(A1.class.getName()),ObjectType.createClassType(A1.class));
    }
    
    @Test
    public void overlayTest1() {
        assertEquals(ObjectType.createClassType(A1.class),ObjectType.createClassType(O1.class));
    }
    
    @Test
    public void overlayTest2() {
        assertEquals(ObjectType.createClassType(A1[].class),ObjectType.createClassType(O1[].class));
    }
    
    @Test 
    public void testObjectType1() {
        ObjectType t=Namespace.resolve("jvm:"+JavaClassTest.class.getName()+":1");
        assertEquals(Namespace.getJavaNamespace(),t.getNamespace());
        assertEquals(JavaClassTest.class.getName()+":1",t.getName());
    }
    
    @Test 
    public void testObjectType2() {
        Type t1=ObjectType.createClassType(A1.class.getName()+":1");
        Type t2=ObjectType.createClassType(A1.class);
        assertEquals(Namespace.getJavaNamespace(),t2.getNamespace());
        assertEquals("1",t1.getVersion());
        assertEquals(A1.class.getName(),t1.getClassName());
        assertEquals(A1.class.getName(),t2.getName());
        // Version
        assertNotEquals(t1,t2);
        assertEquals(A1.class,t1.asClass(A1.class.getClassLoader()));
        assertEquals(A1.class,t2.asClass(A1.class.getClassLoader()));
    }

    public static class A1 {}
    
    @Overlay public static class O1 extends A1 {}

}

package not.alexa.netobjects.types;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import org.junit.Test;

import not.alexa.coding.Data;
import not.alexa.netobjects.api.Overlay;
import not.alexa.netobjects.types.JavaClass.Type;

public class JavaClassTest {
	private static ClassTypeDefinition TEST=new ClassTypeDefinition().createBuilder().build();

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
        TypeDefinition def=Data.getTypeDescription();
        for(MethodTypeDefinition m:def.getMethods()) {
            System.out.println(m.getJavaClassType());
        }
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
        assertEquals(A1.class,t1.asLinkedLocal(A1.class.getClassLoader()).asClass());
        assertEquals(A1.class,t2.asLinkedLocal(A1.class.getClassLoader()).asClass());
    }
    
    @Test
    public void testClassDefType1() {
    	ClassTypeDefinition def=ClassTypeDefinition.getTypeDescription();
    	ClassTypeDefinition def1=def.forType(ObjectType.createClassType("de.notalexa.Dummy"));
    	assertSame(def,def1);
    }

    @Test
    public void testClassDefType2() {
    	ClassTypeDefinition def=TEST;
    	ClassTypeDefinition def1=def.forType(ObjectType.createClassType("de.notalexa.Dummy"));
    	assertNotSame(def,def1);
    	assertNull(def.getJavaClassType());
    	assertNotNull(def1.getJavaClassType());
    	assertEquals(ObjectType.createClassType("de.notalexa.Dummy"), def1.getJavaClassType());
    }

    public static class A1 {}
    
    @Overlay public static class O1 extends A1 {}

}

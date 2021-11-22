package not.alexa.netobjects;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import org.junit.Test;

import java.io.Serializable;

public class CastableTest {

    public CastableTest() {
    }
    
    @Test 
    public void successTest() {
        Context context=new Context.Root();
        CastableClass o=new CastableClass();
        A1 a1=context.cast(A1.class, o);
        assertNotNull(a1);
        try {
            a1=context.fallibleCast(A1.class, o);
            assertNotNull(a1);
        } catch(Throwable t) {
            fail(t.getMessage());
        }
    }
    @Test 
    public void failureTest() {
        Context context=new Context.Root();
        CastableClass o=new CastableClass();
        A2 a2=context.cast(A2.class, o);
        assertNull(a2);
        try {
            a2=context.fallibleCast(A2.class, o);
            fail();
        } catch(Throwable t) {
        }
    }
    
    public static class CastableClass implements Serializable, Castable {
        /**
         * 
         */
        private static final long serialVersionUID = 1L;
        private A1 a1=new A1();
        @Override
        public <T> T castTo(Context context, Class<T> clazz) {
            if(A1.class.equals(clazz)) {
                return context.cast(clazz,a1);
            }
            return null;
        }
        
    }
    
    public static class A1 {
        
    }
    public static class A2 {
        
    }

}

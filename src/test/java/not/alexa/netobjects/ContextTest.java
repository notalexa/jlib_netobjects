package not.alexa.netobjects;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import java.security.SecurityPermission;
import java.util.Arrays;
import java.util.List;


@RunWith(org.junit.runners.Parameterized.class)
public class ContextTest {
    @Parameter public Context context;
    
    @Parameters
    public static List<Context> primitiveClasses() {
        return Arrays.asList(new Context[] {
                new Context.Root(),
                new Context.Default(new Context.Root()) });
        }

    

    public ContextTest() {
    }
    
    @Test
    public void notNullTests() {
        assertNotNull(context.getTypeLoader());
        assertNotNull(context.getLogger());
        assertNotNull(context.getLocale());
        assertTrue(context.implies(new SecurityPermission("test")));
    }
    
    @Test 
    public void adapterTest() {
        A1 a1=new A1();
        context.putAdapter(a1);
        context.putAdapter(A1.class, a1);
        assertEquals(a1,context.getAdapter(A1.class));
        assertEquals(a1,context.castTo(context,A1.class));
        assertEquals(a1,context.castTo(A1.class));
        assertEquals(a1,context.cast(A1.class,context));
        assertEquals(context,context.castTo(context.getClass()));
        assertEquals(context,context.cast(context.getClass(),context));
        assertEquals(a1,context.getAdapter(A1.class));
        try {
            assertEquals(a1,context.fallibleCastTo(context,A1.class));
            assertEquals(a1,context.fallibleCastTo(A1.class));
            assertEquals(a1,context.fallibleCast(A1.class,context));
            assertEquals(context,context.fallibleCastTo(context,context.getClass()));
            assertEquals(context,context.fallibleCast(context.getClass(),context));
        } catch(BaseException e) {
            fail(e.getMessage());
        }
        try {
            context.putAdapter(null);
            fail();
        } catch(Throwable t) {
        }
        context.putAdapter(A1.class,null);
        assertNull(context.castTo(context, A1.class));
        assertNull(context.castTo(A1.class));
    }
    
    @Test
    public void castTest1() {
        A1 a1=new A1();
        assertEquals(a1,context.cast(A1.class,a1));
        assertNull(context.cast(Context.class, a1));
        try {
            assertEquals(a1,context.fallibleCast(A1.class, a1));
        } catch(BaseException e) {
            fail(e.getMessage());
        }
        try {
            context.fallibleCast(Context.class,a1);
            fail();
        } catch(BaseException e) {
        }
        
    }
    
    public static class A1 {
        
    }

}

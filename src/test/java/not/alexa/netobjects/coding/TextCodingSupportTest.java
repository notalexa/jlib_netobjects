package not.alexa.netobjects.coding;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

import org.junit.Test;

import not.alexa.netobjects.BaseException;
import not.alexa.netobjects.Context;
import not.alexa.netobjects.coding.AbstractTextCodingSchemeTest.TestScheme;
import not.alexa.netobjects.types.AccessibleObject;
import not.alexa.netobjects.types.PrimitiveTypeDefinition;
import not.alexa.netobjects.types.access.Access.SimpleTypeAccess;

public class TextCodingSupportTest {

    public TextCodingSupportTest() {
    }
    
    @Test
    public void testGetter() {
        Context context=new Context.Root();
        TextCodingSupport<TestScheme> support=new TextCodingSupport<TestScheme>(TestScheme.DEFAULT_INSTANCE, context);
        assertSame(TestScheme.DEFAULT_INSTANCE,support.getCodingScheme());
        assertSame(context,support.getContext());
    }
    
    @Test
    public void testEncodingSupport() {
        Context context=new Context.Root();
        TextCodingSupport<TestScheme> support=new TextCodingSupport<TestScheme>(TestScheme.DEFAULT_INSTANCE, context);
        Object o=new Object();
        try {
            assertNull(support.getObjectReference(o));
            assertEquals(Integer.valueOf(0), support.getObjectReference(o));
            assertEquals(Integer.valueOf(0), support.getObjectReference(o));
        } catch(BaseException e) {
            fail();
        }
    }

    @Test
    public void testDecodingSupport() {
        Context context=new Context.Root();
        TextCodingSupport<TestScheme> support=new TextCodingSupport<TestScheme>(TestScheme.DEFAULT_INSTANCE, context);
        AccessibleObject o=new SimpleTypeAccess(new AbstractTextCodingSchemeTest.DummyAccessFactory(),PrimitiveTypeDefinition.getTypeDescription(String.class),"");// Object();
        support.addObjectReference(true, "my-ref", o);
        assertSame(support.resolveObjectReference("0"),o);
        assertSame(support.resolveObjectReference("my-ref"),o);
        assertNull(support.resolveObjectReference("1"));
        assertNull(support.resolveObjectReference("my-ref2"));
    }
}

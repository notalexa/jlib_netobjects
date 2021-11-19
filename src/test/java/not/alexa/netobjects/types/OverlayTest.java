package not.alexa.netobjects.types;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import not.alexa.coding.Data;
import not.alexa.netobjects.Context;
import not.alexa.netobjects.api.Overlay;

public class OverlayTest {

    public OverlayTest() {
    }
    
    @Test public void registerTest() {
        Context context=new Context.Root() {
            TypeLoader loader=new DefaultTypeLoader();
            public TypeLoader getTypeLoader() {
                return loader;
            }
        };
        TypeLoader overlayLoader=context.getTypeLoader().overlay(O1.class).overlay(O2.class);
        assertEquals(overlayLoader.getLinkedLocal(overlayLoader.resolveType(Data.class)),overlayLoader.getLinkedLocal(overlayLoader.resolveType(O1.class)));
        assertEquals(O1.class,overlayLoader.getLinkedLocal(overlayLoader.resolveType(Data.class)));
        assertEquals(O1.class,overlayLoader.getLinkedLocal(overlayLoader.resolveType(O1.class)));
        assertEquals(PrimitiveTypeDefinition.class, overlayLoader.getLinkedLocal(PrimitiveTypeDefinition.getTypeDescription()));
    }

    public static class A1 {}
    @Overlay
    public static class O1 extends Data {}
    @Overlay
    public static class O2 extends A1 {}
}

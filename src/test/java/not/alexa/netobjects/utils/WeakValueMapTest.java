package not.alexa.netobjects.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class WeakValueMapTest {

    public WeakValueMapTest() {
    }
    
    @Test
    public void test() {
        WeakValueMap<Object,Object> weakMap=new WeakValueMap<>();
        List<Object> refKeeper=new ArrayList<Object>();
        for(int i=0;i<100000;i++) {
            Object o=new Object();
            Object v=new Object();
            refKeeper.add(v);
            weakMap.put(o,v);
            weakMap.put(o,v);
        }
        assertEquals(100000,weakMap.size());
        refKeeper=new ArrayList<Object>();
        for(int i=0;i<3;i++) {
            System.gc();
            try {
                Thread.sleep(100);
            } catch(Throwable t) {
            }
        }
        assertEquals(0,weakMap.size());
    }
    
    @Test
    public void removeTest() {
        WeakValueMap<Object,Object> weakMap=new WeakValueMap<>();
        Object o=new Object();
        Object v=new Object();
        weakMap.put(o,v);
        assertEquals(v, weakMap.get(o));
        weakMap.remove(o);
        assertNull(weakMap.get(o));
        assertEquals(0,weakMap.size());
    }
}

package not.alexa.netobjects.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class WeakKeyMapTest {

    public WeakKeyMapTest() {
    }
    
    @Test
    public void test() {
        WeakKeyMap<Object,Object> weakMap=new WeakKeyMap<>();
        List<Object> refKeeper=new ArrayList<Object>();
        for(int i=0;i<100000;i++) {
            Object o=new Object();
            refKeeper.add(o);
            weakMap.put(o,new Object());
            weakMap.put(o,new Object());
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
        WeakKeyMap<Object,Object> weakMap=new WeakKeyMap<>();
        Object o=new Object();
        Object v=new Object();
        weakMap.put(o,v);
        assertEquals(v, weakMap.get(o));
        weakMap.remove(o);
        assertNull(weakMap.get(o));
        assertEquals(0,weakMap.size());
    }

}

package not.alexa.netobjects;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class BaseExceptionTest {

    public BaseExceptionTest() {
    }
    
    @Test
    public void exceptionTest1() {
        try {
            BaseException.throwException(new NullPointerException());
        } catch(BaseException e) {
            assertEquals(BaseException.NOT_FOUND,e.getCode());
            assertEquals("NullPointerException",e.getMessage());
        }
    }
    
    @Test
    public void exceptionTest3() {
        try {
            BaseException.throwException(new SecurityException("Denied"));
        } catch(BaseException e) {
            assertEquals(BaseException.NOT_AUTHORIZED,e.getCode());
            assertEquals("Denied",e.getMessage());
        }
    }

    @Test
    public void exceptionTest5() {
        try {
            BaseException.throwException(new Exception("Test"));
        } catch(BaseException e) {
            assertEquals(BaseException.GENERAL,e.getCode());
            assertEquals("Test",e.getMessage());
        }
    }

    @Test
    public void exceptionTest2() {
        try {
            BaseException.throwException(new BaseException(BaseException.BAD_REQUEST,"Test"));
        } catch(BaseException e) {
            assertEquals(BaseException.BAD_REQUEST,e.getCode());
            assertEquals("Test",e.getMessage());
        }
    }

    @Test
    public void exceptionTest6() {
        try {
            throw new BaseException();
        } catch(BaseException e) {
            assertEquals(BaseException.GENERAL,e.getCode());
            assertNull(e.getMessage());
        }
    }

    @Test
    public void exceptionTest4() {
        try {
            try {
                new BaseException(BaseException.BAD_REQUEST,"Test").throwRuntimeException();
            } catch(Throwable t) {
                BaseException.throwException(t);
            }
        } catch(BaseException e) {
            assertEquals(BaseException.BAD_REQUEST,e.getCode());
            assertEquals("Test",e.getMessage());
        }
    }
    
    @Test
    public void exceptionTest7() {
        try {
            try {
                new Context.Default(null);
            } catch(Throwable t) {
                BaseException.throwException(t);
            }
        } catch(BaseException e) {
            assertEquals(BaseException.NOT_FOUND,e.getCode());
            assertEquals("Parent context",e.getMessage());
        }
    }
}

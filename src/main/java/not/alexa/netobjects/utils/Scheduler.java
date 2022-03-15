/*
 * Copyright (C) 2022 Not Alexa
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package not.alexa.netobjects.utils;

import not.alexa.netobjects.BaseException;
import not.alexa.netobjects.Context;
import not.alexa.netobjects.Executable;
import not.alexa.netobjects.coding.CodingScheme;
import not.alexa.netobjects.types.ClassTypeDefinition;
import not.alexa.netobjects.types.Lambda;
import not.alexa.netobjects.types.access.AbstractClassAccess;
import not.alexa.netobjects.types.access.AccessContext;
import not.alexa.netobjects.types.access.AccessFactory;
import not.alexa.netobjects.types.access.Constructor;

/**
 * Utility class to shift execution of network objects <b>in time</b> (normally called a
 * scheduler). After the delay is elapsed, the object is presented the callback (and ignored
 * if the callback is {@code null}.
 * <br>A local implementation is provided in {@link LocalScheduler}.
 * An implementation using Kafka can be found in the Kafka subproject.
 * <p>Usually, the callback is an {@link Executor} and the network object implements the {@link Executable}
 * interface. Even more specific, the network object is a {@link Lambda} and the executors 
 * context defines overlays for the classes executed.
 * 
 * @author notalexa
 * @see LocalScheduler
 */
public abstract class Scheduler {
    protected final Context context;
    protected final Callback<Object> callback;
    protected final Class<?> callbackClass;

    /**
     * Construct a scheduler for the given callback. The callback can be empty (in which case scheduled objects are potentially ignored
     * <i>if no other scheduler is present which consumes the object</i>. Use cases for schedulers with no callback are distributed systems
     * in which the scheduler is implemented using Kafka for example and the object is scheduled in one VM but executed in another environment
     * (that is another VM). In this case, the execution is shifted both in space and time.)
     * @param <T>
     * @param context
     * @param callback
     */
    @SuppressWarnings("unchecked")
    protected <T> Scheduler(Context context,Callback<T> callback) {
        this.context=context;
        this.callback=(Callback<Object>)callback;
        if(callback!=null) {
            callbackClass=Callback.getParameterClass(callback);
        } else {
            callbackClass=null;
        }
    }
    
    /**
     * Schedule the object.
     * 
     * @param delay the delay after which the object should be executed.
     * @param o the object to execute (via a callback)
     * @throws BaseException if an error occurs
     */
    public void schedule(long delay,Object o) throws BaseException {
        schedule(createEntry(delay, o));
    }
    
    /**
     * Schedule the provided entry.
     * 
     * @param id the id of the entry if the entry should be scheduled cancellable and {@code null} otherwise.
     * @param entry the entry to schedule
     * @throws BaseException if the entry cannot be scheduled. If the entry should be scheduled cancelable and the underlying scheduler doesn't provide
     * the functionality, 
     */
    protected abstract void schedule(ScheduledEntry entry) throws BaseException;
    
    /**
     * Method for checking if the given partition is active. This method is inspired by Kafka and the idea is to select one of many VM's accessing
     * a singleton resource. The resource can be accessed if and only a (predefined) partition is active in the current VM.
     * <br>This method doesn't garantuee that whenever calling at least one VM is active. Therefore, accessing a resource is not garantueed. This also
     * implies that a timer executed at intervals should not reschedule himself inside a method which is called only if the current VM is active.
     * <br>Typically it's sufficient to use {@link #isActive()}. But this implies that we have ONE active partition inside a cluster of VMs. Using
     * different partition values helps to distribute timers over the cluster.
     * 
     * @param partition the partition in question (&geq; 0)
     * @return {@code true} if the partition is active in this VM)
     */
    public abstract boolean isActive(int partition);
    
    /**
     * 
     * @return {@code true} if the default partition (0) is active.
     */
    public final boolean isActive() {
        return isActive(0);
    }
    
    /**
     * Create a scheduled entry internally.
     * 
     * @param delay the delay after which the object should be executed.
     * @param o the object to execute (via a callback)
     * @return an object representing this scheduled entry
     * @throws BaseException if an error occurs (for example the object is not a network object and can't be serialized).
     */
    protected ScheduledEntry createEntry(long delay,Object o) throws BaseException {
        byte[] payload=CodingScheme.getSystemScheme().createEncoder(context).encode(o).asBytes();
        return new ScheduledEntry(System.currentTimeMillis()+delay,payload);
    }

    /**
     * Callback interface for executing scheduled entries.
     * 
     * @author notalexa
     *
     * @param <T> the type of this callback.
     */
    public interface Callback<T> {
        /**
         * Static method resolving the type of the callback.
         * 
         * @param callback the callback in question
         * @return the type of the callback
         */
        public static Class<?> getParameterClass(Callback<?> callback) {
            return TypeUtils.createClassResolver(callback.getClass()).resolve(Callback.class).getParameters()[0].getResolvedClass();
        }
        /**
         * 
         * @return the context of the callback
         */
        public Context getCallbackContext();
        
        /**
         * Callback method. Execute the provided entry (in the callbacks context).
         * 
         * @param t the object
         * @throws BaseException if an error occurs.
         */
        public void call(T t) throws BaseException;
    }
        
    /**
     * A scheduled entry.
     * 
     * @author notalexa
     *
     */
    public static class ScheduledEntry {
        private long when;
        private byte[] payload;
        protected ScheduledEntry(long when,byte[] payload) {
            this.when=when;
            this.payload=payload;
        }
        
        /**
         * 
         * @return the time of execution
         */
        public long getScheduledTime() {
            return when;
        }
        
        /**
         * 
         * @return the payload of the entry.
         */
        public byte[] getPayload() {
            return payload;
        }
    }
    
    /**
     * Most common callback. The callback just evaluates the executable.
     * 
     * @author notalexa
     *
     */
    public static class Executor implements Callback<Executable> {
        private static ClassTypeDefinition TYPE=new ClassTypeDefinition(Executor.class);
        public static ClassTypeDefinition getTypeDescription() {
            return TYPE;
        }
        private Context context;
        public Executor(Context context) {
            this.context=context;
        }
        
        @Override
        public Context getCallbackContext() {
            return context;
        }
        
        @Override
        public void call(Executable t) throws BaseException {
            t.main(context);
        }
        
        public static class ClassAccess extends AbstractClassAccess {

            public ClassAccess(AccessFactory factory, ClassTypeDefinition classType, Constructor constructor) {
                super(factory, TYPE, constructor);
            }
            
            @Override
            protected Object newInstance(AccessContext context) throws BaseException {
                Object o=super.newInstance(context);
                ((Executor)o).context=context.getContext();
                return o;
            }


            @Override
            protected Object getField(Object o, int index) throws BaseException {
                return null;
            }

            @Override
            protected void setField(Object o, int index, Object v) throws BaseException {
            }
        }
    }
}

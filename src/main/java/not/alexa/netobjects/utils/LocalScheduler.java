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

import java.util.Hashtable;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import not.alexa.netobjects.BaseException;
import not.alexa.netobjects.Context;
import not.alexa.netobjects.coding.CodingScheme;

/**
 * Local implementation of the scheduler. This class uses an executor service to schedule
 * requested tasks and doesn't save same somewhere.
 * 
 * @author notalexa
 *
 */
public class LocalScheduler extends Scheduler {
    private ScheduledExecutorService executors;
    private Map<UUID,ScheduledFuture<?>> waiting=new Hashtable<>();

    /**
     * Construct this local scheduler
     * @param <T> the type of the callback
     * @param context the context to use for the scheduler itself
     * @param executors the executor service to use
     * @param callback the callback called for tasks which needs to be executed
     */
    public <T> LocalScheduler(Context context, ScheduledExecutorService executors,Callback<T> callback) {
        super(context, callback);
        this.executors=executors;
    }

    /**
     * Since this is a singelton, each partition is active
     */
    @Override
    public boolean isActive(int partition) {
        return true;
    }
    
    /**
     * Cancel the scheduled task.
     */
    @Override
    public boolean cancel(Scheduled scheduled) {
        ScheduledFuture<?> future=waiting.remove(scheduled.id);
        if(future!=null) {
            future.cancel(false);
        }
        return true;
    }
    
    /**
     * Just schedule a wrapper of entry into the executor service. 
     */
    @Override
    protected void schedule(UUID id,ScheduledEntry entry) throws BaseException {
        ScheduledFuture<?> future=executors.schedule(new Runnable() {
            @Override
            public void run() {
                synchronized(id==null?this:id) {
                    if(id==null||waiting.remove(id)!=null) {
                        if(callback!=null) try {
                            if(ScheduledEntry.class.isAssignableFrom(callbackClass)) {
                                callback.call(entry);
                            } else {
                                Object o=CodingScheme.getSystemScheme().createDecoder(callback.getCallbackContext(), entry.getPayload()).decode(callbackClass);
                                callback.call(o);
                            }
                        } catch(Throwable t) {
                            // Silently ignore this exception
                        }
                    }
                }
            }
        }, entry.getScheduledTime()-System.currentTimeMillis(),TimeUnit.MILLISECONDS);
        if(id!=null) {
            waiting.put(id,future);
        }
    }
}

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

import static org.junit.Assert.fail;

import org.junit.Test;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import not.alexa.coding.Data;
import not.alexa.netobjects.Context;
import not.alexa.netobjects.api.Overlay;
import not.alexa.netobjects.types.DefaultTypeLoader;
import not.alexa.netobjects.types.Lambda;
import not.alexa.netobjects.types.ObjectType;
import not.alexa.netobjects.utils.Scheduler.Executor;
import not.alexa.netobjects.utils.Scheduler.Scheduled;

/**
 * The test uses a local scheduler.
 * 
 * @author notalexa
 *
 */
public class SchedulerTest {

    public SchedulerTest() {
    }
    
    private Context createOverlayContext(Context context) {
        return Context.createRootContext(context.getTypeLoader().overlay(Resource.LocalDataOverlay.class));
    }
        
    @Test
    public void schedule() {
        ScheduledExecutorService executors=Executors.newSingleThreadScheduledExecutor();
        try {
            Context context=Context.createRootContext(new DefaultTypeLoader());
            Context overlayContext=createOverlayContext(context);
            Scheduler scheduler=new LocalScheduler(context,executors,new Executor(overlayContext));
            ObjectType type1=ObjectType.resolve("jvm:not.alexa.coding.Data::1a833da6-3a6b-3e20-898d-ae06d06602e1");
            Lambda lambda=new Lambda(new Data("text",1,"x","y","z"),type1);
            Scheduled scheduled=scheduler.scheduleCancellable(System.currentTimeMillis()+5000L,lambda);
            scheduler.schedule(3000L,lambda);
            // Put the resource in the context.
            overlayContext.putAdapter(new Resource());
            Thread.sleep(1000);
            System.out.println("Canceled: "+scheduler.cancel(scheduled));
            Thread.sleep(5000);
        } catch(Throwable t) {
            fail(t.getMessage());
        } finally {
            executors.shutdownNow();
        }
    }
    
    private static class Resource {
        long timestamp=System.currentTimeMillis();
    
        @Overlay
        public class LocalDataOverlay extends Data {
            LocalDataOverlay() {
            }
    
            @Override
            public String helloWorld(Context context) throws Throwable {
                System.out.println((System.currentTimeMillis()-timestamp)+": Super "+super.helloWorld(context));
                return super.helloWorld(context);
            }    
        }
    }
}

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
package not.alexa.netobjects.kafka.timer;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.InputStream;
import java.security.SecureRandom;
import java.util.Calendar;
import java.util.Date;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import not.alexa.coding.Data;
import not.alexa.netobjects.Context;
import not.alexa.netobjects.api.Overlay;
import not.alexa.netobjects.coding.CodingScheme;
import not.alexa.netobjects.kafka.KafkaClient;
import not.alexa.netobjects.types.DefaultTypeLoader;
import not.alexa.netobjects.types.Lambda;
import not.alexa.netobjects.types.ObjectType;
import not.alexa.netobjects.utils.Scheduler.Executor;

/**
 * Scheduler test for a Kafka backend. This test is not automatically invoked since
 * resources are needed.
 * 
 * @author notalexa
 *
 */
public class KafkaSchedulerTest {

    public KafkaSchedulerTest() {
    }
    
    private Context createOverlayContext(Context context) {
        return Context.createRootContext(context.getTypeLoader().overlay(Resource.LocalDataOverlay.class));
    }

    public void schedule(int msgs,int minutes) {
        Random random=new SecureRandom();
        Calendar cal=Calendar.getInstance();
        Context context=Context.createRootContext(new DefaultTypeLoader());
        Context overlayContext=createOverlayContext(context);
        Resource resource=new Resource();
        overlayContext.putAdapter(resource);
        try(InputStream stream=KafkaSchedulerTest.class.getResourceAsStream("client.xml");
            KafkaClient client=CodingScheme.getSystemScheme().createDecoder(context, stream).decode(KafkaClient.class)) {
            assertNotNull(client);
            KafkaScheduler scheduler=new KafkaScheduler(context,"scheduler", "target-topic", new Executor(overlayContext));
            client.main(context);
            Thread.sleep(2000);
            client.install(scheduler);
            ObjectType type1=ObjectType.resolve("jvm:not.alexa.coding.Data::1a833da6-3a6b-3e20-898d-ae06d06602e1");
            Lambda lambda=new Lambda(new Data("text",1,"x","y","z"),type1);
            System.out.println(new Date()+": "+" Scheduling "+msgs+" message(s) for the next "+minutes+" minute(s) ...");
            for(int i=0;i<msgs;i++) {
                // Take care of scheduling in the future to observe "exact" scheduling
                long l=System.currentTimeMillis();
                cal.setTimeInMillis(l);
                cal.clear(Calendar.SECOND);
                cal.clear(Calendar.MILLISECOND);
                long base=cal.getTimeInMillis()+120000;
                long s=base+random.nextInt(minutes)*60000;
                scheduler.schedule(s-l, lambda);
            }
            Thread.sleep(1200000);
        } catch(Throwable t) {
            t.printStackTrace();
            fail(t.getMessage());
        }
    }
    
    // Container for the Overlay class defining some local resources
    private static class Resource {
        long timestamp=System.currentTimeMillis();
        AtomicInteger count=new AtomicInteger();
    
        @Overlay
        public class LocalDataOverlay extends Data {
            LocalDataOverlay() {
            }
    
            @Override
            public String helloWorld(Context context) throws Throwable {
                System.out.println(new Date()+": "+(System.currentTimeMillis()-timestamp)+":"+(count.incrementAndGet())+": "+super.helloWorld(context));
                return super.helloWorld(context);
            }    
        }
    }
    
    public static void main(String[] args) {
        // 1000 msgs in 5 minutes
        new KafkaSchedulerTest().schedule(1000,5);
    }

}

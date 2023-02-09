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

import java.io.InputStream;

import not.alexa.netobjects.Context;
import not.alexa.netobjects.coding.CodingScheme;
import not.alexa.netobjects.kafka.KafkaClient;
import not.alexa.netobjects.types.DefaultTypeLoader;

/**
 * Run a timer. Topics are described in timer.xml. This "test" doesn't use the {@literal @Test}
 * annotation since it needs a Kafka resource and is long running.
 * 
 * @author notalexa
 *
 */
public class KafkaTimerTest {

    public KafkaTimerTest() {
    }

    public void run() {
        Context context=Context.createRootContext(new DefaultTypeLoader());
        try(InputStream stream=KafkaSchedulerTest.class.getResourceAsStream("timer.xml");
            KafkaClient client=stream==null?null:CodingScheme.getSystemScheme().createDecoder(context, stream).decode(KafkaClient.class)) {
            assertNotNull(client);
            client.main(context);
            Thread.sleep(Long.MAX_VALUE);
        } catch(Throwable t) {
            context.getLogger().error("Timer failed.",t);
        }
    }
    
    public static void main(String[] args) {
        new KafkaTimerTest().run();
    }
}

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

import org.apache.kafka.clients.producer.ProducerRecord;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import not.alexa.netobjects.BaseException;
import not.alexa.netobjects.Context;
import not.alexa.netobjects.coding.CodingScheme;
import not.alexa.netobjects.kafka.KafkaApp;
import not.alexa.netobjects.kafka.KafkaClient;
import not.alexa.netobjects.kafka.KafkaClient.Partitions;
import not.alexa.netobjects.kafka.Message;
import not.alexa.netobjects.types.ClassTypeDefinition;
import not.alexa.netobjects.types.PrimitiveTypeDefinition;

/**
 * A Kafka application which implements a timer. The app needs a scheduler topic for incoming messages and a "waiting grid" to store the messages.
 * <br>The algorithm uses a base time unit t (defaulting to 49sec) and assigns a time t<sub>n</sub> to any partition n&gt;0 as 4<sup>n-1</sup>t.
 * The message scheduled at delay d is written into the target topic if d&leq;0 and written in partition n if t<sub>n</sub>&leq;d&lt;t<sub>n+1</sub> (with t<sub>0</sub>=0).
 * In addition to the message, the algorithm saves the timestamp at which the message is written (t<sub>msg</sub>) and the time the message is scheduled. Now, if the message is in
 * partition n, it's save to pause this partition until the time t<sub>msg</sub>+t<sub>n</sub> is reached. We cannot say that a message written in this partition after the
 * current message is scheduled after the current message but we can say that it is scheduled after t<sub>msg</sub>+t<sub>n</sub> (since the second message is located after
 * the current message in partition n and therefore the timestamp of the second message is greater than the timestamp of the first message). After pausing, the message is
 * reanalyzed according to the remaining delay.
 * <p>The algorithm assumes that reading the message from a partition and writing it into a partition is an expensive operation. The number of such operations is independent of
 * the number scheduled messages in the timer and can be bounded by the crpss sum of the delay with respect to the decomposition into the base bime unit t times 4<sup>n</sup>.
 * Therefore a reduction to a quarter of the base time unit adds a maximum of 3 extra operations. Together with network delays, the resolution of the timer should not be considered as
 * very good. Good use cases for the timer are for example cron jobs.
 * <p>On partition 0, the algorithm pauses until the scheduled time of the message is reached. Subsequent messages may be scheduled before the first message but due to the time ordering
 * of the partition the error is less than the base time unit (and the message is never delivered before the scheduled time).
 * <br>Pausing with the minimum of 1/4t and the delay of the first message reduce the error of the scheduled time (with the cost of additional operations). If the scheduled times are not
 * uniformly distributed but peaked within the base time interval, these operations are useless. A common case is the cron time with a peak every minute and 49sec (1 or 0 peaks in the base unit).
 * <p>The time interval which can be efficiently handled by a number of partitions increases very fast. Taken a base unit of 49 sec for example we have the following table:
 * <table style="text-align:center">
 * <tr><td>Partitions</td><td>2</td><td>3</td><td>4</td><td>5</td><td>6</td><td>7</td><td>8</td><td>9</td><td>10</td></tr>
 * <tr><td>Time</td><td>3 min</td><td>12 min</td><td>48 min</td><td>3 hours</td><td>12 hours</td><td>2 days</td><td>8 days</td><td>1 month</td><td>4 months</tr>
 * </table>
 * 
 * @author notalexa
 *
 */
public class KafkaTimer implements KafkaApp {
    private static ClassTypeDefinition TYPE=new ClassTypeDefinition(KafkaTimer.class).createBuilder()
            .createField("timerTopic",PrimitiveTypeDefinition.getTypeDescription(String.class))
                .addTag("XML","@timerTopic").build()
            .createField("waitTopic",PrimitiveTypeDefinition.getTypeDescription(String.class))
                .addTag("XML", "@waitTopic").build()
            .createField("timeUnit",PrimitiveTypeDefinition.getTypeDescription(Long.class))
                .addTag("XML", "@timeUnit").setDefaultValue(49000L).build()
            .build();
    public static ClassTypeDefinition getTypeDescription() {
        return TYPE;
    }
    
    protected String timerTopic;
    protected String waitTopic;
    protected Context context;
    protected long timeUnit=49000L;
    protected long[] offsets;
    protected KafkaTimer() {}
    
    /**
     * Construct a timer with the given timer and wait topic.
     * 
     * @param timerTopic the timer topic where incoming messages are read from
     * @param waitTopic the wait topic
     */
    public KafkaTimer(String timerTopic,String waitTopic) {
        this.timerTopic=timerTopic;
        this.waitTopic=waitTopic;
    }

    /**
     * The method returns an empty set if the group id doesn't match the wait topic. This avoids double registration of the timer component
     * with different group ids.
     */
    @Override
    public Collection<String> getTopics(String groupId) {
        return groupId.equals(waitTopic)?Arrays.asList(timerTopic,waitTopic):Collections.emptySet();
    }

    @Override
    public void start(Context context,KafkaClient client, Collection<String> topics) {
        this.context=context;
        offsets=new long[client.getPartitionCount(waitTopic)];
        long n=1;
        for(int i=1;i<offsets.length;i++) {
            offsets[i]=timeUnit*n;
            n*=4;
        }
    }

    /**
     * The main method either reschedules a (wrapped) message from the wait topic or initially schedules a
     * message from the timer topic.
     * 
     */
    @Override
    public void onMessageReceived(Message msg) {
        if(timerTopic.equals(msg.getTopicPartition().topic())) {
            handleScheduleRequest(msg);
           // msg.commit();
        } else {
            byte[] payload=msg.getValue();
            long scheduled=getLong(payload,8);
            schedule(msg,true,msg.getTopicPartition().partition(),scheduled,payload);
        }
    }
    
    /**
     * Schedule the given payload (with timestamp at the first 8 bytes and scheduled time at the second 8 bytes).
     * if the scheduled time is in the past, the payload or the scheduled object (dependiing on {@link KafkaTimerEntry#objectOnly})
     * is written into the target topic (given in {@link KafkaTimerEntry#targetTopic}). Otherwise, the new parition is
     * calculated as described in the class description.
     * 
     * @param msg the incoming message
     * @param fromWaitingQueue {@link true} is the message is from the wait topic 
     * @param startPartition the maximum partition this message should be scheduled in
     * @param scheduled the scheduled time (already parsed and contained in the second 8 bytes of the payload
     * @param payload the payload itself
     */
    protected void schedule(Message msg,boolean fromWaitingQueue,int startPartition,long scheduled,byte[] payload) {
        long scheduleRef=System.currentTimeMillis();
        long delta=scheduled-scheduleRef;
        if(delta<0) try {
            // Ready for evaluation
            payload=Arrays.copyOfRange(payload,16,payload.length);
            KafkaTimerEntry entry=CodingScheme.getSystemScheme().createDecoder(context,payload).decode(KafkaTimerEntry.class);
            context.getLogger().info("Payload requested to be performed (scheduled for {}) (off by {}).",new Date(entry.when),System.currentTimeMillis()-entry.when);
            msg.send(new ProducerRecord<byte[], byte[]>(entry.targetTopic, entry.objectOnly?entry.timedObject:payload),this);                
        } catch(BaseException e) {
            context.getLogger().error("Executing payload failed.",e);
        } finally {
            msg.commit();
        } else if(fromWaitingQueue&&msg.getTopicPartition().partition()==0) {
            // Nearby
            msg.pause(delta,TimeUnit.MILLISECONDS);
        } else {
            int partition=startPartition;
            long l=getLong(payload,0)-scheduleRef+offsets[partition];
            if(l>0&&fromWaitingQueue) {
                msg.pause(l,TimeUnit.MILLISECONDS);
            } else try {
                while(partition>0&&offsets[partition]>delta) {
                    partition--;
                }
                putLong(scheduleRef,payload,0);
                //System.out.println(new Date()+": Schedule in Partition "+partition+". Scheduled for "+new Date(scheduled));
                msg.send(new ProducerRecord<byte[], byte[]>(waitTopic, partition, null, payload),this);
            } finally {
                msg.commit();
            }
        }
    }
    
    /**
     * For the payload in the message, construct the wrapping payload and schedule.
     * 
     * @param msg the message (out of the timer topic)
     */
   protected void handleScheduleRequest(Message msg) {
        //System.out.println("Received timer request with offset "+msg.getRecord().offset());
        try {
           byte[] value=msg.getValue();
           KafkaTimerEntry entry=CodingScheme.getSystemScheme().createDecoder(context,value).decode(KafkaTimerEntry.class);
           //System.out.println(new Date()+" Received timer request with offset "+msg.getRecord().offset()+" scheduled at "+new Date(entry.when));
           byte[] payload=new byte[16+value.length];
           System.arraycopy(value,0, payload,16,value.length);
           putLong(entry.when,payload,8);
           schedule(msg,false,offsets.length-1,entry.when,payload);
        } catch(Throwable t) {
            context.getLogger().error("Scheduling msg failed.",t);
        }
    }
    
    private static void putLong(long v,byte[] b,int offset) {
        putInt((int)(v>>32),b,offset);
        putInt((int)v,b,offset+4);
    }
    
    private static long getLong(byte[] b,int offset) {
        return ((long)getInt(b,offset))<<32|(0xffffffffL&getInt(b,offset+4));
    }
    
    private static int getInt(byte[] b,int offset) {
        return ((b[offset]&0xff)<<24)|((b[offset+1]&0xff)<<16)|((b[offset+2]&0xff)<<8)|((b[offset+3]&0xff));
    }
    
    private static void putInt(int v,byte[] b,int offset) {
        b[offset]=(byte)(v>>24);
        b[offset+1]=(byte)(v>>16);
        b[offset+2]=(byte)(v>>8);
        b[offset+3]=(byte)(v);
    }

    /**
     * We seek to the beginning if not committed in this case since not committed means "in the future".
     */
    @Override
    public void onPartitionsAssigned(Partitions partitions) {
        partitions.seekToEarliestIfNotCommitted();
    }
    
    /**
     * Class holding relevant information for the scheduled object.
     * 
     * @author notalexa
     *
     */
    public static class KafkaTimerEntry {
        protected long when;
        protected boolean objectOnly;
        protected String targetTopic;
        protected byte[] timedObject;
        KafkaTimerEntry() {}
        public KafkaTimerEntry(long when,String targetTopic,boolean objectOnly,byte[] timedObject) {
            this.when=when;
            this.objectOnly=objectOnly;
            this.targetTopic=targetTopic;
            this.timedObject=timedObject;
        }
        
        private static ClassTypeDefinition TYPE=new ClassTypeDefinition(KafkaTimerEntry.class).createBuilder()
                .addField("when",PrimitiveTypeDefinition.getTypeDescription(Long.class))
                .createField("objectOnly",PrimitiveTypeDefinition.getTypeDescription(Boolean.class))
                    .setDefaultValue(true).build()
                .addField("targetTopic",PrimitiveTypeDefinition.getTypeDescription(String.class))
                .addField("timedObject",PrimitiveTypeDefinition.getTypeDescription(Object.class)).build();
        public static ClassTypeDefinition getTypeDescription() {
            return TYPE;
        }
        
        public long getWhen() {
            return when;
        }

        public boolean isObjectOnly() {
            return objectOnly;
        }
        
        public String getTargetTopic() {
            return targetTopic;
        }
        
        public byte[] getTimedObject() {
            return timedObject;
        }
    }
}

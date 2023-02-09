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
import org.apache.kafka.common.TopicPartition;

import java.util.Collection;
import java.util.Collections;

import not.alexa.netobjects.BaseException;
import not.alexa.netobjects.Context;
import not.alexa.netobjects.coding.CodingScheme;
import not.alexa.netobjects.kafka.KafkaApp;
import not.alexa.netobjects.kafka.KafkaClient;
import not.alexa.netobjects.kafka.KafkaClient.Partitions;
import not.alexa.netobjects.kafka.Message;
import not.alexa.netobjects.kafka.timer.KafkaTimer.KafkaTimerEntry;
import not.alexa.netobjects.utils.Scheduler;

/**
 * Scheduler implementation based on Kafka topics. The implementation expects
 * a scheduler topic in which {@link ScheduledEntry}s are written and a
 * target topic, which is used
 * <ul>
 * <li>as a base for {@link Scheduler#isActive(int)}: If the partition with the given number is assigned, the
 * scheduler is active.
 * <li>as a source for the callback
 * </ul>
 * <br>If the callback is not {@code null}, but the target topic is {@code null}, the callback will
 * never be invoked (and {@link #isActive()} will always return {@code false}. Otherwise, if
 * the callback is {@code null} and the target topic is not {@code null}, any message read will be ignored
 * (but {@link #isActive(int)} can be {@code true}).
 * 
 * @author notalexa
 *
 */
public class KafkaScheduler extends Scheduler implements KafkaApp {
    protected String targetTopic;
    protected String schedulerTopic;
    protected boolean[] active;    
    protected KafkaClient client;
    
    /**
     * Construct a scheduler with the given topics and callback.
     * 
     * @param <T> the type of the callback
     * @param context the context of the scheduler
     * @param schedulerTopic the scheduler topic
     * @param targetTopic the target topic
     * @param callback the callback to invoke if data is read from the target topic
     */
    public <T> KafkaScheduler(Context context, String schedulerTopic,String targetTopic, Callback<T> callback) {
        super(context, callback);
        this.targetTopic=targetTopic;
        this.schedulerTopic=schedulerTopic;
        
    }

    /**
     * Reset the active table.
     *  
     */
    @Override
    public void onPartitionsRevoked(Partitions partitions) {
        if(active!=null) {
            for(TopicPartition partition:partitions.getPartitions()) {
                if(targetTopic.equals(partition.topic())&&partition.partition()<active.length) {
                    active[partition.partition()]=false;
                }
            }
        }
    }

    /**
     * Initialize the active table (and move to the latest offsets if not yet committed)
     */
    @Override
    public void onPartitionsAssigned(Partitions partitions) {
        KafkaApp.super.onPartitionsAssigned(partitions);
        if(active!=null) {
            active=new boolean[active.length];
            for(TopicPartition partition:partitions.getPartitions()) {
                if(targetTopic.equals(partition.topic())&&partition.partition()<active.length) {
                    active[partition.partition()]=true;
                }
            }
        }
    }

    /**
     * The target topic if set, an empty collection otherwise.
     */
    @Override
    public Collection<String> getTopics(String groupId) {
        return targetTopic==null?Collections.emptySet():Collections.singleton(targetTopic);
    }

    /**
     * The method initialize the active table.
     */
    @Override
    public void start(Context context,KafkaClient client, Collection<String> topics) {
        this.client=client;
        if(targetTopic!=null&&topics.contains(targetTopic)) {
            this.active=new boolean[client.getPartitionCount(targetTopic)];
        }
    }

    /**
     * Decode the incoming message and invoke the callback if present.
     */
    @Override
    public void onMessageReceived(Message msg) {
        try {
            if(callback!=null) {
                Object o=CodingScheme.getSystemScheme().createDecoder(callback.getCallbackContext(),msg.getRecord().value()).decode(callbackClass);
                if(o!=null) {
                    callback.call(o);
                }
            }
        } catch(Throwable t) {
            // We silently die with this message
        } finally {
            msg.commit();
        }
    }

    /**
     * Schedule the entry by writing it into the scheduler topic.
     */
    @Override
    protected void schedule(ScheduledEntry entry) throws BaseException {
        KafkaTimerEntry timerEntry=new KafkaTimerEntry(entry.getScheduledTime(), targetTopic,true, entry.getPayload());
        ProducerRecord<byte[],byte[]> record=new ProducerRecord<byte[], byte[]>(schedulerTopic, CodingScheme.getSystemScheme().createEncoder(context).encode(timerEntry).asBytes());
        client.send(record,this);
    }

    /**
     * Is the given partition active? The partition is mapped to the number of partition in the target topic (if any).
     */
    @Override
    public boolean isActive(int partition) {
        if(active==null||partition<0) {
            return false;
        }
        return active[partition%active.length];
    }
}

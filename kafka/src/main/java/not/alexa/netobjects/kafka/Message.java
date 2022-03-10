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
package not.alexa.netobjects.kafka;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * The interface wraps an incoming record and is presented to the kafka application. This
 * interface has three parts:
 * <ul>
 * <li>Part one consists of convenience methods retrieving information from the kafka client
 * and received record
 * <li>Part two exposes send methods from the producer managed in the client
 * <li>Part three defines methods how to deal with the message.
 * </ul>
 * The client fetches records. After fetching, the corresponding app is called in the main fetch thread.
 * After all records are delivered, the client thread <b>waits for all records processed</b>. Calling
 * one of the methods from the third group tells the client that the record is processed (either 
 * because it is processed indicated by {@link Message#commit()} or requested to be
 * reprocessed (indicated by {@link #pause()} or {@link #pause(long, TimeUnit)}).
 * <br>If the processing is performed on the main thread, records are delivered in the "right"
 * order (with increasing offset). A call to commit will commit the record (and all records 
 * with a smaller offset). A call to pause will automatically pause all records with a greater offset.
 * If processing is done asynchronously, the app itself has to ensure that ordering is preserved.
 * 
 * @author notalexa
 *
 */
public interface Message {
    /**
     * 
     * @return the client who fetched the message
     */
    public KafkaClient getClient();
    /**
     * 
     * @return the underlying record
     */
    public ConsumerRecord<byte[],byte[]> getRecord();
    /**
     * 
     * @return the topic partition of this message
     */
    public TopicPartition getTopicPartition();
    /**
     * 
     * @return the offset of this message
     */
    public default long getOffset() {
        return getRecord().offset();
    }

    /**
     * 
     * @return the key of this message
     */
    public default byte[] getKey() {
        return getRecord().key();
    }

    /**
     * 
     * @return the value of this message
     */
    public default byte[] getValue() {
        return getRecord().value();
    }
    
    /**
     * Commit this message (and all messages with a smaller offset). If this operation
     * is performed on the main thread, all messages with a smaller offset are not paused
     * and therefore committed too.
     */
    public void commit();
    
    /**
     * Pause this topic partition forever. Note that pausing is a local operation. If a
     * rebalancing occurs, the partition is read from the last committed offset again.
     * To resume, save the message and call the {@link Resumer#resume()} method or call {@link KafkaClient#resume(TopicPartition)}
     * directly.
     * 
     * @return a resumer which can be used to resume the partition
     */
    public Resumer pause();
    
    /**
     * Pause this topic partition for the given time. Note that pausing is a local operation.
     * If a rebalancing occurs, the partition is read from the last committed offset again.
     * 
     * @param l the amount of time to wait
     * @param unit the time unit
     * @return a resumer which can be used to resume the partition
     */
    public Resumer pause(long l, TimeUnit unit);
    
    /**
     * Convenience method to send a record using the producer managed in the client.
     * 
     * @param record the record to send
     * @param callback the callback handling the result
     * @return a future for the result
     */
    public default Future<RecordMetadata>  send(ProducerRecord<byte[], byte[]> record, Callback callback) {
        return getClient().send(record, callback);
    }
    
    /**
     * Convenience method to send a record using the producer managed in the client.
     * 
     * @param record the record to send
     * @return a future for the result
     */
    public default Future<RecordMetadata>  send(ProducerRecord<byte[], byte[]> record) {
        return getClient().send(record);
    }

    /**
     * Object which can be called to resume the partition via a called to one of the pause methods.
     * 
     * @author notalexa
     *
     */
    public interface Resumer extends Runnable {
        
        /**
         * Resume this partition if pausing. This method is typically called from another thread.
         */
        public void resume();
        
        @Override
        public default void run() {
            resume();
        }
    }
}

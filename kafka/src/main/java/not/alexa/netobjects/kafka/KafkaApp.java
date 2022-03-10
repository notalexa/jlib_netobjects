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

import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.RecordMetadata;

import java.util.Collection;

import not.alexa.netobjects.Context;
import not.alexa.netobjects.kafka.KafkaClient.Partitions;

/**
 * Interface to define an application which can be installed in the kafka client.
 * <br>The lifecylce of an app is a sequence of calls {@link #start(Context, KafkaClient, Collection)} and {@link #stop()}. After starting, the {@link #onMessageReceived(Message)}
 * method can be invoked if topics are defined and messages are received for this topics.
 * <br>For convenience, the app implements the {@link Callback} interface useful when sending records via the client.
 * <br>The interface also defines method analogous to the rebalancer methods for information about assignment and revocation of topic partitions. The default implementation
 * of the assignment method seeks to the latest offset if the partition is not yet committed. The revocation method is empty by default.
 * 
 * @author notalexa
 *
 */
public interface KafkaApp extends Callback {
    /**
     * 
     * @return the topics, this app is listening too.
     */
    public Collection<String> getTopics(String groupId);
    
    /**
     * Lifecycle method. This method is called on startup of the client.
     * 
     * @param context the context of the kafka client
     * @param client the client
     * @param topics the topics assigned to this client. This should be the topics returned by {@link #getTopics()}
     */
    public default void start(Context context,KafkaClient client,Collection<String> topics) {
    }
    
    /**
     * Lifecycle method. This method is called on shutdown.
     */
    public default void stop() {
    }
    
    /**
     * Called when a message is received.
     * @param msg the message received
     */
    public default void onMessageReceived(Message msg) {
        msg.commit();
    }

    /**
     * Method called when partitions of this app are revoked. The default implementation has no action.
     * 
     * @param partitions the partitions revoked
     */
    public default void onPartitionsRevoked(Partitions partitions) {
    }
    
    /**
     * Method called when partitions of this app are assigned. The default implementation seeks to the latest offset
     * if the partition is not committed.
     * 
     * @param partitions the partitions assigned
     */
    public default void onPartitionsAssigned(Partitions partitions) {
        partitions.seekToLatestIfNotCommitted();
    }
    
    /**
     * Introduce a callback default implementation (do nothing).
     */
    @Override
    public default void onCompletion(RecordMetadata metadata, Exception exception) {
    }
}

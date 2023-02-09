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

import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.ByteArraySerializer;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import not.alexa.netobjects.BaseException;
import not.alexa.netobjects.Context;
import not.alexa.netobjects.Executable;
import not.alexa.netobjects.kafka.Message.Resumer;
import not.alexa.netobjects.types.ArrayTypeDefinition;
import not.alexa.netobjects.types.ClassTypeDefinition;
import not.alexa.netobjects.types.ClassTypeDefinition.Field;
import not.alexa.netobjects.types.InterfaceTypeDefinition;
import not.alexa.netobjects.types.PrimitiveTypeDefinition;
import not.alexa.netobjects.types.TypeDefinition;
import not.alexa.netobjects.types.access.AbstractClassAccess;
import not.alexa.netobjects.types.access.Access;
import not.alexa.netobjects.types.access.AccessFactory;
import not.alexa.netobjects.types.access.ArrayTypeAccess;
import not.alexa.netobjects.types.access.Constructor;

public class KafkaClient implements ConsumerRebalanceListener, AutoCloseable, Executable {
    private static final Map<String,String> CONSUMER_DEFAULTS=new HashMap<String, String>();
    private static final Map<String,String> PRODUCER_DEFAULTS=new HashMap<String, String>();
    static {
        CONSUMER_DEFAULTS.put("enable.auto.commit","false");
        CONSUMER_DEFAULTS.put("auto.offset.reset","none");
    }
    private static TypeDefinition STRING_STRING_MAP=new ArrayTypeDefinition(new ClassTypeDefinition()
            .createBuilder()
            .createField("key",PrimitiveTypeDefinition.getTypeDescription(String.class))
                .addTag("XML","@prop").build()
            .createField("value",PrimitiveTypeDefinition.getTypeDescription(String.class))
                .addTag("XML","#text").build()
            .build());
    private static ClassTypeDefinition TYPE=new ClassTypeDefinition(KafkaClient.class).createBuilder()
            .createField("groupId", PrimitiveTypeDefinition.getTypeDescription(String.class))
                .setOptional(false)
                .addTag("XML","@groupId")
                .build()
            .createField("config", STRING_STRING_MAP)
                .build()
            .createField("consumer",STRING_STRING_MAP)
                .setOptional(true)
                .build()
            .createField("producer",STRING_STRING_MAP)
                .setOptional(true)
                .build()
            .createField("preloaded",new ArrayTypeDefinition(new InterfaceTypeDefinition(KafkaApp.class)))
                .addTag("XML","app")
                .setOptional(true)
                .build()
            .build();
    public static ClassTypeDefinition getTypeDescription() {
        return TYPE;
    }
    
    protected String groupId;
    protected Map<String,String> config;
    protected Map<String,String> consumer;
    protected Map<String,String> producer;
    protected List<KafkaApp> preloaded;
    
    private KafkaProducer<byte[],byte[]> writer;
    private Consumer reader;
    private boolean closed;
    private Collection<TopicPartition> assigned;
    
    
    private Map<String,KafkaApp> appMap=new HashMap<String, KafkaApp>();
    private Map<KafkaApp,Collection<String>> topicMap=new IdentityHashMap<KafkaApp, Collection<String>>();
    protected ScheduledExecutorService executor;
    protected boolean refreshSubscription;
    private int gen;
    protected Context context;
    
    KafkaClient() {}
    
    public KafkaClient(Map<String,String> config) {
        this.config=config;
    }
    
    /**
     * Install the given kafka app. The method checks if the requested topics are not yet assigned to another app.
     * The start method of the app is called if the client was already started.
     * 
     * @param app the app to install
     * @return <code>true</code> if the app can be installed.
     */
    public boolean install(KafkaApp app) {
        if(subscribe(false,app)) {
            synchronized(this) {
                if(context!=null) {
                    app.start(context, this, topicMap.get(app));
                }
            }
            return true;
        }
        return false;
    }
    
    /**
     * Subscribe the app (again). If topics changes for the app, the method can be called to subscribe the new topics again.
     * 
     * @param app the app to subscribe (again)
     * @return <code>true</code> if subscription was successful (that is if the app was installed and the topics were not assigned to another app)
     */
    public boolean subscribe(KafkaApp app) {
        return subscribe(true,app);
    }

    /**
     * (Re)subscribe the app.
     * 
     * @param checkExistence check existence of the app. If <code>true</code>, the app needs to be already installed.
     * @param app the app to (re)subscribe.
     * @return <code>true</code> if (re)subscription was successful
     */
    protected boolean subscribe(boolean checkExistence,KafkaApp app) {
        Collection<String> topics=topicMap.get(app);
        if(checkExistence&&topics==null) {
            return false;
        }
        Set<String> requestedTopics=new HashSet<>(app.getTopics(groupId));
        if(requestedTopics.removeAll(appMap.keySet())) {
            return false;
        }
        if(topics==null||!topics.equals(requestedTopics)) {
            synchronized(appMap) {
                if(topics!=null) for(String s:topics) {
                    appMap.remove(s);
                }
                requestedTopics.forEach(topic->{appMap.put(topic,app);});
                topicMap.put(app,requestedTopics);
                refreshSubscription=true;
            }
        }
        return true;
    }
    
    /**
     * Remove the app from this client if installed.
     * 
     * @param app the app to remove.
     */
    public void dispose(KafkaApp app) {
        synchronized(appMap) {
            Collection<String> topics=topicMap.remove(app);
            if(topics!=null) {
                topics.forEach(topic->{appMap.remove(topic);});
                refreshSubscription=true;
            }
        }
    }
    
    /**
     * Start this client if not yet started
     */
    @Override
    public void main(Context context) throws BaseException {
        if(this.context==null) {
            synchronized(this) {
                this.context=context;
                closed=false;
                executor=Executors.newSingleThreadScheduledExecutor();
                reader=new Consumer();
                Thread thread=new Thread(reader::startup);
                List<KafkaApp> failed=new ArrayList<KafkaApp>();
                topicMap.forEach((app,topics)->{
                    try {
                        app.start(context, this, topics);
                    } catch(Throwable t) {
                        failed.add(app);
                    }
                });
                failed.forEach(this::dispose);
                thread.start();
            }
        }
    }
    
    /**
     * Callback method for handling an uncatched exception in the main consumer loop. If this method is called,
     * the consumer loop is dead and the consumer will never start again.
     * 
     * @param t the uncatched throwable
     */
    protected void handleUncatchedException(Throwable t) {
        context.getLogger().error("Consumer died.",t);
    }
    
    /**
     * Create a writer with the supplied configurations if not yet created.
     * 
     * @return a writer with the supplied configuration properties
     */
    protected KafkaProducer<byte[], byte[]> getWriter() {
        if(writer==null) {
            synchronized (this) {
                if(writer==null) {
                    Map<String,Object> producerConfigs=new HashMap<String, Object>(PRODUCER_DEFAULTS);
                    if(config!=null) {
                        producerConfigs.putAll(config);
                    }
                    if(producer!=null) {
                        producerConfigs.putAll(producer);
                    }
                    writer=new KafkaProducer<byte[], byte[]>(producerConfigs,new ByteArraySerializer(),new ByteArraySerializer());
                }
            }
        }
        return writer;
    }

    /**
     * Create the reader with the supplied configurations
     * 
     * @return a reader configuraed with the supplied configuration properties.
     */
    protected KafkaConsumer<byte[], byte[]> createReader() {
        Map<String,Object> consumerConfigs=new HashMap<String, Object>(CONSUMER_DEFAULTS);
        if(config!=null) {
            consumerConfigs.putAll(config);
        }
        if(consumer!=null) {
            consumerConfigs.putAll(consumer);
        }
        consumerConfigs.put("group.id",groupId);
        return new KafkaConsumer<byte[], byte[]>(consumerConfigs,new ByteArrayDeserializer(),new ByteArrayDeserializer());
    }

    /**
     * Delegate to the underlying writer.
     * @param record the record to send
     * @return a future representing the metadata
     */
    public Future<RecordMetadata> send(ProducerRecord<byte[], byte[]> record) {
        return getWriter().send(record);
    }

    /**
     * Delegate to the underlying writer.
     * @param record the record to send
     * @param callback the callback used when sending completed
     * @return a future representing the metadata
     */
    public Future<RecordMetadata> send(ProducerRecord<byte[], byte[]> record, Callback callback) {
        return getWriter().send(record, callback);
    }

    /**
     * 
     * @param topic the topic we want the number of partitions for
     * @return the number of partitions for the given topic
     */
    public int getPartitionCount(String topic) {
        return getWriter().partitionsFor(topic).size();
    }

    /**
     * Close this client. The method waits for the consumer to stop, calls {@link KafkaApp#stop()} on each registered app and closes writer and executor.
     * After terminating, the client can be restarted again using the {@link #main(Context)}.
     */
    @Override
    public void close() {
        if(!closed) try {
            closed=true;
            if(reader!=null) try {
                reader.close();
                reader.await();
            } catch(Throwable t) {
            } finally {
                reader=null;
            }
            for(KafkaApp app:topicMap.keySet()) {
                app.stop();
            }
            if(writer!=null) try {
                writer.close();
            } finally {
                writer=null;
            }
            if(executor!=null) try {
                executor.shutdownNow();
            } finally {
                executor=null;
            }
            context=null;
        } finally {
            executor=null;
            context=null;
            writer=null;
            reader=null;
        }
    }
    
    /**
     * Delegate to the corresponding kafka apps.
     */
    @Override
    public void onPartitionsRevoked(Collection<TopicPartition> partitions) {
        gen++;
        assigned.removeAll(partitions);
        for(Map.Entry<KafkaApp,Collection<String>> entry:topicMap.entrySet()) {
            entry.getKey().onPartitionsRevoked(new Partitions(entry.getKey(),entry.getValue(),partitions));
        }
    }
    
    /**
     * Delegate to the corresponding kafka apps.
     */
    @Override
    public void onPartitionsAssigned(Collection<TopicPartition> partitions) {
        gen++;
        assigned=new HashSet<TopicPartition>(partitions);
        //System.out.println("Assigned in generation "+gen+": "+assigned);
        //partitions.forEach(partition->{if(reader.reader.committed(partition)==null) reader.reader.seek(partition, 0);});
        for(Map.Entry<KafkaApp,Collection<String>> entry:topicMap.entrySet()) {
            entry.getKey().onPartitionsAssigned(new Partitions(entry.getKey(),entry.getValue(),assigned));
        }
    }
    
    protected KafkaClient finish() {
        if(preloaded!=null) {
            for(KafkaApp app:preloaded) {
                install(app);
            }
        }
        return this;
    }
                
    /**
     * Internal consumer of this client. The consumer polls a Kafka Consumer and handles the fetched records
     * via delegating to a Kafka app.
     * 
     * @author notalexa
     *
     */
    class Consumer extends CountDownLatch implements AutoCloseable {
        private KafkaConsumer<byte[],byte[]> reader;
        private boolean closed;
        Consumer() {
            super(1);
        }
        
        /**
         * Commit the given offsets.
         * 
         * @param offsets the offsets to commit.
         */
        void commitSync(Map<TopicPartition, OffsetAndMetadata> offsets) {
            if(reader!=null) {
                reader.commitSync(offsets);
            }
        }

        /**
         * Seek to the given partition
         * 
         * @param partition the partition
         * @param offset the offset to seek to
         */
        void seek(TopicPartition partition, OffsetAndMetadata offset) {
            if(reader!=null) {
                reader.seek(partition, offset);
            }
        }

        /**
         * Subscribe to the given collection of topics. If the collection is empty, the underlying reader is closed, otherwise the
         * reader is created if necessary.
         * 
         * @param topics the topics to subscripte
         * @param listener the rebalance listener
         */
        synchronized void subscribe(Collection<String> topics, ConsumerRebalanceListener listener) {
            if(closed) {
                throw new IllegalStateException("Reader is closed");
            }
            if(topics.isEmpty()) {
                closeInternal();
            } else {
                if(reader==null) {
                    reader=KafkaClient.this.createReader();
                }
                reader.subscribe(topics, listener);
            }
        }

        /**
         * Pause the partitions.
         * 
         * @param partitions the partitions
         */
        void pause(Collection<TopicPartition> partitions) {
            if(reader!=null) {
                reader.pause(partitions);
            }
        }

        /**
         * Delegate method to resume the given partitions
         * 
         * @param partitions the partitions to resume
         */
        void resume(Collection<TopicPartition> partitions) {
            if(reader!=null) {
                reader.resume(partitions);
            }
        }

        /**
         * Poll the underlying KafkaConsumer.
         *  
         * @param timeout the pool timeout
         * @return a list with all fetched records
         */
        protected ConsumerRecords<byte[],byte[]> poll(Duration timeout) {
            if(reader!=null) {
                return reader.poll(timeout);
            } else try {
                Thread.sleep(timeout.toMillis());
            } catch(InterruptedException e) {
            }
            return new ConsumerRecords<byte[], byte[]>(Collections.emptyMap());
        }

        /**
         * Close this reader (and decrement the count down latch)
         */
        @Override
        public void close() {
            if(!closed) {
                closed=true;
                closeInternal();
                countDown();
            }
        }
        
        void closeInternal() {
            if(reader!=null) try {
                reader.close();
            } finally {
                reader=null;
            }            
        }
        
        /**
         * Main loop of the consumer. The consumer can be reconfigured via calling method on a consumed message or via reconfiguring
         * the kafka apps.
         */
        protected void startup() {
            try {
                Collection<TopicPartition> pausing=new HashSet<TopicPartition>();
                Map<TopicPartition,OffsetAndMetadata> resume=new HashMap<>();
                Set<String> topics=new HashSet<>(appMap.keySet());
                subscribe(topics, KafkaClient.this);
                Map<TopicPartition,OffsetAndMetadata> commitOffsets=new HashMap<>();
                Semaphore sync=new Semaphore(Integer.MAX_VALUE);
                Map<String,KafkaApp> map=Collections.emptyMap();
                while(!closed) {
                    synchronized(pausing) {
                        if(resume.size()>0) {
                            //System.out.println(new Date()+": Resume partition "+resume+" in main loop (Pausing="+pausing+").");
                            resume.entrySet().forEach(entry->{reader.seek(entry.getKey(), entry.getValue());});
                            reader.resume(resume.keySet());
                            pausing.removeAll(resume.keySet());
                            resume.clear();
                        }
                        if(pausing.size()>0) try {
                            if(pausing.retainAll(assigned)) {
                                //System.out.println("Pausing changed.");
                            }
                            reader.pause(pausing);
                            pausing.clear();
                        } catch(Throwable t) {
                        }
                    }
                    boolean refreshSubscription=KafkaClient.this.refreshSubscription;
                    if(refreshSubscription) {
                        synchronized (appMap) {
                            map=new HashMap<String, KafkaApp>(appMap);
                            topics=map.keySet();
                            KafkaClient.this.refreshSubscription=false;
                        }
                        subscribe(topics, KafkaClient.this);
                    }
                    ConsumerRecords<byte[],byte[]> records=poll(Duration.ofMillis(100));
                    commitOffsets.clear();
                    for(ConsumerRecord<byte[],byte[]> record:records) {
                        TopicPartition partition=new TopicPartition(record.topic(),record.partition());
                        //System.out.println(new Date()+": Received: "+partition.toString()+"["+record.offset()+"]");
                        if(!pausing.contains(partition)) {
                            class ResumerImpl implements Resumer {
                                long offset;
                                int resumerGeneration=gen;
                                private ResumerImpl(long offset) {
                                    this.offset=offset;
                                }
                                
                                public void resume() {
                                    synchronized(pausing) {
                                        //System.out.println(new Date()+": Resume "+partition+" in generation "+resumerGeneration+" (current="+gen+")");
                                        if(resumerGeneration==gen/*&&pausing.contains(partition)*/) {
                                            OffsetAndMetadata offset=resume.get(partition);
                                            if(offset==null||offset.offset()>this.offset) {
                                                resume.put(partition,new OffsetAndMetadata(this.offset));
                                            }
                                        }
                                    }
                                }
                            }
                            KafkaApp app=appMap.get(record.topic());
                            if(app!=null) {
                                sync.acquire();
                                app.onMessageReceived(new Message() {
                                    boolean released=false;
        
                                    @Override
                                    public KafkaClient getClient() {
                                        return KafkaClient.this;
                                    }
        
                                    @Override
                                    public ConsumerRecord<byte[], byte[]> getRecord() {
                                        return record;
                                    }
        
                                    @Override
                                    public void commit() {
                                        if(!released) {
                                            released=true;
                                            OffsetAndMetadata offset=commitOffsets.get(partition);
                                            if(offset==null||offset.offset()<=record.offset()) {
                                                commitOffsets.put(partition,new OffsetAndMetadata(record.offset()+1));
                                            }
                                            sync.release();
                                        }
                                    }
        
                                    @Override
                                    public Resumer pause() {
                                        if(!released) {
                                            //System.out.println(new Date()+": Request pause for partition "+partition);
                                            released=true;
                                            pausing.add(partition);
                                            sync.release();
                                        }
                                        return new ResumerImpl(getOffset());
                                    }
        
                                    @Override
                                    public Resumer pause(long delay, TimeUnit unit) {
                                        Resumer resumer=pause();
                                        executor.schedule(resumer, delay, unit);
                                        return resumer;
                                    }
        
                                    @Override
                                    public TopicPartition getTopicPartition() {
                                        return partition;
                                    }
                                });
                            }
                        }
                    }
                    try {
                        // Wait until all records are processed
                        sync.acquire(Integer.MAX_VALUE);
                        commitSync(commitOffsets);
                    } finally {
                        sync.release(Integer.MAX_VALUE);
                    }
                }
            } catch(Throwable t) {
                handleUncatchedException(t);
            } finally {
                close();
            }
        }

    }
    
    /**
     * Represents a set of partitions assigned to an app.
     * 
     * @author notalexa
     *
     */
    public class Partitions {
        private KafkaApp app;
        private Collection<String> topics;
        private Collection<TopicPartition> allPartitions;
        private Partitions(KafkaApp app,Collection<String> topics,Collection<TopicPartition> allPartitions) {
            this.app=app;
            this.topics=topics;
            this.allPartitions=allPartitions;
        }
        
        /**
         * @return The app this partitions are assigned to
         */
        public KafkaApp getApp() {
            return app;
        }

        /**
         * 
         * @return all partitions assigned to this app
         */
        public Set<TopicPartition> getPartitions() {
            return allPartitions.stream()
                    .filter(partition->{
                        return topics.contains(partition.topic());
                    })
                    .collect(Collectors.toSet());
        }
        
        /**
         * 
         * @return a collection of the offsets of all partitions assigned to this app
         */
        public Collection<Offsets> getOffsets() {
            return allPartitions.stream()
                    .filter(partition->{
                        return topics.contains(partition.topic());
                    })
                    .map(partition->{
                        return new Offsets(partition);
                    })
                    .collect(Collectors.toList());
        }
        
        /**
         * Always seek to the latest offset
         */
        public void seekToLatest() {
            reader.reader.seekToEnd(getPartitions());
        }
        
        protected Set<TopicPartition> notCommitted() {
            Set<TopicPartition> n= allPartitions.stream()
                    .filter(partition->{
                        return topics.contains(partition.topic())&&reader.reader.committed(Collections.singleton(partition)).get(partition)==null;
                    })
                    .collect(Collectors.toSet());
            return n;
        }
        
        /**
         * Seek to the latest offset if the partition is not yet committed.
         */
        public void seekToLatestIfNotCommitted() {
            Set<TopicPartition> partitions=notCommitted();
            if(partitions.size()>0) {
                // An empty set is interpreted as any partition
                reader.reader.seekToEnd(notCommitted());
            }
        }
        
        /**
         * Always seek to the earlist offset for the partition
         */
        public void seekToEarliest() {
            reader.reader.seekToBeginning(getPartitions());            
        }
        
        /**
         * Seek to the earlist offset if the partition is not yet committed.
         */
        public void seekToEarliestIfNotCommitted() {
            Set<TopicPartition> partitions=notCommitted();
            if(partitions.size()>0) {
                // An empty set is interpreted as any partition
                reader.reader.seekToBeginning(notCommitted());
            }
        }
    }
    
    /**
     * Define access to the different offsets of a partition_
     * <ul>
     * <li>The earlist offset of the partition.
     * <li>The current position in this partition.
     * <li>The latest offset of this partition.
     * <li>The committed offset of this partition (-1 if not committed yet).
     * </ul>
     * 
     * @author notalexa
     *
     */
    public class Offsets {
        private TopicPartition partition;
        private Offsets(TopicPartition partition) {
            this.partition=partition;
        }
        
        /**
         * 
         * @return the partition of this offsets
         */
        public TopicPartition getPartition() {
            return partition;
        }

        /**
         * 
         * @return <code>true</code> if this partition has a committed offset
         */
        public boolean isCommitted() {
            return getCommitted()>=0;
        }
        
        public void seekToLatest() {
            reader.reader.seekToEnd(Collections.singleton(partition));
        }
        
        public void seekToEarlist() {
            reader.reader.seekToBeginning(Collections.singleton(partition));            
        }
        
        /**
         * 
         * @return the current position in this partition
         */
        public long getPosition() {
            return reader.reader.position(partition);
        }
        
        /**
         * 
         * @return the committed offset of this partition (-1 if the partition wasn't committed yet)
         */
        public long getCommitted() {
            OffsetAndMetadata offset=reader.reader.committed(Collections.singleton(partition)).get(partition);
            return offset==null?-1:offset.offset();
        }
        
        /**
         * 
         * @return the earlist offset of this partition
         */
        public long getEarlist() {
            return reader.reader.beginningOffsets(Collections.singleton(partition)).get(partition);
        }

        /**
         * 
         * @return the latest offset of this partition
         */
        public long getLatest() {
            return reader.reader.endOffsets(Collections.singleton(partition)).get(partition);
        }

    }
}

/**
 * Copyright 2012 Lennart Koopmann <lennart@socketfeed.com>
 *
 * This file is part of Graylog2.
 *
 * Graylog2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog2.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package org.graylog2;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import org.apache.log4j.Logger;
import org.graylog2.blacklists.BlacklistCache;
import org.graylog2.buffers.OutputBuffer;
import org.graylog2.buffers.ProcessBuffer;
import org.graylog2.database.MongoBridge;
import org.graylog2.database.MongoConnection;
import org.graylog2.filters.MessageFilter;
import org.graylog2.forwarders.forwarders.LogglyForwarder;
import org.graylog2.indexer.EmbeddedElasticSearchClient;
import org.graylog2.initializers.Initializer;
import org.graylog2.inputs.MessageInput;
import org.graylog2.inputs.gelf.GELFChunkManager;
import org.graylog2.outputs.MessageOutput;
import org.graylog2.streams.StreamCache;

import com.google.common.collect.Lists;
import com.yammer.metrics.HealthChecks;
import com.yammer.metrics.core.HealthCheck;
import org.graylog2.database.HostCounterCache;

public class GraylogServer implements Runnable {

    private static final Logger LOG = Logger.getLogger(GraylogServer.class);

    private MongoConnection mongoConnection;
    private MongoBridge mongoBridge;
    private Configuration configuration;
    private RulesEngine rulesEngine;
    private ServerValue serverValues;
    private GELFChunkManager gelfChunkManager;

    private static final int SCHEDULED_THREADS_POOL_SIZE = 15;
    private ScheduledExecutorService scheduler;

    public static final String GRAYLOG2_VERSION = "0.9.7-dev";

    public static final String MASTER_COUNTER_NAME = "master";
    
    private int lastReceivedMessageTimestamp = 0;

    private EmbeddedElasticSearchClient indexer;

    private HostCounterCache hostCounterCache;

    private MessageCounterManager messageCounterManager;

    private List<Initializer> initializers = Lists.newArrayList();
    private List<MessageInput> inputs = Lists.newArrayList();
    private List<Class<? extends MessageFilter>> filters = Lists.newArrayList();
    private List<Class<? extends MessageOutput>> outputs = Lists.newArrayList();

    private ProcessBuffer processBuffer;
    private OutputBuffer outputBuffer;

    public void initialize(Configuration configuration) {
        this.configuration = configuration; // TODO use dependency injection

        mongoConnection = new MongoConnection();    // TODO use dependency injection
        mongoConnection.setUser(configuration.getMongoUser());
        mongoConnection.setPassword(configuration.getMongoPassword());
        mongoConnection.setHost(configuration.getMongoHost());
        mongoConnection.setPort(configuration.getMongoPort());
        mongoConnection.setDatabase(configuration.getMongoDatabase());
        mongoConnection.setUseAuth(configuration.isMongoUseAuth());
        mongoConnection.setMaxConnections(configuration.getMongoMaxConnections());
        mongoConnection.setThreadsAllowedToBlockMultiplier(configuration.getMongoThreadsAllowedToBlockMultiplier());
        mongoConnection.setReplicaSet(configuration.getMongoReplicaSet());
        mongoConnection.setMessagesCollectionSize(configuration.getMessagesCollectionSize());

        messageCounterManager = new MessageCounterManager();
        messageCounterManager.register(MASTER_COUNTER_NAME);

        hostCounterCache = new HostCounterCache();

        processBuffer = new ProcessBuffer(this);
        processBuffer.initialize();

        outputBuffer = new OutputBuffer(this);
        outputBuffer.initialize();

        mongoBridge = new MongoBridge();
        mongoBridge.setConnection(mongoConnection); // TODO use dependency injection

        gelfChunkManager = new GELFChunkManager(this);

        indexer = new EmbeddedElasticSearchClient(this);
        serverValues = new ServerValue(this);
    }

    public void registerInitializer(Initializer initializer) {
        this.initializers.add(initializer);
    }

    public void registerInput(MessageInput input) {
        this.inputs.add(input);
    }

    public <T extends MessageFilter> void registerFilter(Class<T> klazz) {
        this.filters.add(klazz);
    }

    public <T extends MessageOutput> void registerOutput(Class<T> klazz) {
        this.outputs.add(klazz);
    }

    public void registerHealthCheck(HealthCheck check) {
        HealthChecks.register(check);
    }

    @Override
    public void run() {

        // initiate the mongodb connection, this might fail but it will retry to establish the connection
        mongoConnection.connect();
        gelfChunkManager.start();
        BlacklistCache.initialize(this);
        StreamCache.initialize(this);

        if (indexer.indexExists()) {
            LOG.info("Index exists. Not creating it.");
        } else {
            LOG.info("Index does not exist! Trying to create it ...");
            if (indexer.createIndex()) {
                LOG.info("Successfully created index.");
            } else {
                LOG.fatal("Could not create Index. Terminating.");
                System.exit(1);
            }
        }

        // Statically set timeout for LogglyForwarder.
        // TODO: This is a code smell and needs to be fixed.
        LogglyForwarder.setTimeout(configuration.getForwarderLogglyTimeout());

        scheduler = Executors.newScheduledThreadPool(SCHEDULED_THREADS_POOL_SIZE);

        // Call all registered initializers.
        for (Initializer initializer : this.initializers) {
            initializer.initialize();
            LOG.debug("Initialized: " + initializer.getClass().getSimpleName());
        }

        // Call all registered inputs.
        for (MessageInput input : this.inputs) {
            input.initialize(this.configuration, this);
            LOG.debug("Initialized input: " + input.getName());
        }

        LOG.info("Graylog2 up and running.");

        while (true) {
            try { Thread.sleep(1000); } catch (InterruptedException e) { /* lol, i don't care */ }
        }

    }

    public MongoConnection getMongoConnection() {
        return mongoConnection;
    }

    public MongoBridge getMongoBridge() {
        return mongoBridge;
    }

    public ScheduledExecutorService getScheduler() {
        return scheduler;
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    public void setRulesEngine(RulesEngine engine) {
        rulesEngine = engine;
    }

    public RulesEngine getRulesEngine() {
        return rulesEngine;
    }

    public EmbeddedElasticSearchClient getIndexer() {
        return indexer;
    }

    public ServerValue getServerValues() {
        return serverValues;
    }

    public GELFChunkManager getGELFChunkManager() {
        return this.gelfChunkManager;
    }

    public ProcessBuffer getProcessBuffer() {
        return this.processBuffer;
    }

    public OutputBuffer getOutputBuffer() {
        return this.outputBuffer;
    }

    public List<Class<? extends MessageFilter>> getFilters() {
        return this.filters;
    }

    public List<Class<? extends MessageOutput>> getOutputs() {
        return this.outputs;
    }

    public MessageCounterManager getMessageCounterManager() {
        return this.messageCounterManager;
    }

    public HostCounterCache getHostCounterCache() {
        return this.hostCounterCache;
    }
    
    public int getLastReceivedMessageTimestamp() {
        return this.lastReceivedMessageTimestamp;
    }
    
    public void setLastReceivedMessageTimestamp(int t) {
        this.lastReceivedMessageTimestamp = t;
    }

}

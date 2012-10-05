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
import org.graylog2.forwarders.forwarders.LogglyForwarder;
import org.graylog2.indexer.EmbeddedElasticSearchClient;
import org.graylog2.initializers.Initializer;
import org.graylog2.inputs.MessageInput;
import org.graylog2.inputs.gelf.GELFChunkManager;
import org.graylog2.outputs.MessageOutput;
import org.graylog2.streams.StreamCache;

import com.google.common.collect.Lists;
import org.graylog2.activities.Activity;
import org.graylog2.activities.ActivityWriter;
import org.graylog2.cluster.Cluster;
import org.graylog2.communicator.Communicator;
import org.graylog2.communicator.methods.CommunicatorMethod;
import org.graylog2.database.HostCounterCacheImpl;
import org.graylog2.indexer.Deflector;
import org.graylog2.plugin.GraylogServer;
import org.graylog2.plugin.buffers.Buffer;
import org.graylog2.plugin.filters.MessageFilter;
import org.graylog2.plugins.PluginLoader;

/**
 * Server core, handling and holding basically everything.
 * 
 * (Du kannst das Geraet nicht bremsen, schon garnicht mit bloßen Haenden.)
 * 
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class Core implements GraylogServer {

    private static final Logger LOG = Logger.getLogger(Core.class);

    private MongoConnection mongoConnection;
    private MongoBridge mongoBridge;
    private Configuration configuration;
    private RulesEngineImpl rulesEngine;
    private ServerValue serverValues;
    private GELFChunkManager gelfChunkManager;

    private static final int SCHEDULED_THREADS_POOL_SIZE = 15;
    private ScheduledExecutorService scheduler;

    public static final String GRAYLOG2_VERSION = "0.9.7-dev";

    public static final String MASTER_COUNTER_NAME = "master";
    
    private int lastReceivedMessageTimestamp = 0;

    private EmbeddedElasticSearchClient indexer;

    private HostCounterCacheImpl hostCounterCache;

    private MessageCounterManagerImpl messageCounterManager;

    private Cluster cluster;
    
    private List<Initializer> initializers = Lists.newArrayList();
    private List<MessageInput> inputs = Lists.newArrayList();
    private List<Class<? extends MessageFilter>> filters = Lists.newArrayList();
    private List<Class<? extends MessageOutput>> outputs = Lists.newArrayList();
    private List<Class<? extends CommunicatorMethod>> communicatorMethods = Lists.newArrayList();
    
    private int loadedFilterPlugins = 0;
    
    private ProcessBuffer processBuffer;
    private OutputBuffer outputBuffer;
    
    private Deflector deflector;
    
    private ActivityWriter activityWriter;
    
    private Communicator communicator;
    
    private String serverId;
    
    private boolean localMode = false;

    public void initialize(Configuration configuration) {
        serverId = Tools.generateServerId();
        
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

        mongoBridge = new MongoBridge();
        mongoBridge.setConnection(mongoConnection); // TODO use dependency injection
        mongoConnection.connect();
        
        cluster = new Cluster(this);
        
        communicator = new Communicator(this);
        
        activityWriter = new ActivityWriter(mongoBridge, communicator);
        
        messageCounterManager = new MessageCounterManagerImpl();
        messageCounterManager.register(MASTER_COUNTER_NAME);

        hostCounterCache = new HostCounterCacheImpl();

        processBuffer = new ProcessBuffer(this);
        processBuffer.initialize();

        outputBuffer = new OutputBuffer(this);
        outputBuffer.initialize();

        gelfChunkManager = new GELFChunkManager(this);

        indexer = new EmbeddedElasticSearchClient(this);
        serverValues = new ServerValue(this);
                
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                activityWriter.write(new Activity("Shutting down.", GraylogServer.class));
            }
        });
    }
    
    public void registerInitializer(Initializer initializer) {
        if (initializer.masterOnly() && !this.isMaster()) {
            LOG.info("Not registering initializer " + initializer.getClass().getSimpleName()
                    + " because it is marked as master only.");
            return;
        }
        
            
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

    public <T extends CommunicatorMethod> void registerCommunicatorMethod(Class<T> klazz) {
        this.communicatorMethods.add(klazz);
    }

    @Override
    public void run() {

        // initiate the mongodb connection, this might fail but it will retry to establish the connection
        gelfChunkManager.start();
        BlacklistCache.initialize(this);
        StreamCache.initialize(this);
        
        // Set up deflector.
        LOG.info("Setting up deflector.");
        deflector = new Deflector(this);
        deflector.setUp();
        
        // Set up recent index.
        if (indexer.indexExists(EmbeddedElasticSearchClient.RECENT_INDEX_NAME)) {
            LOG.info("Recent index exists. Not creating it.");
        } else {
            LOG.info("Recent index does not exist! Trying to create it ...");
            if (indexer.createRecentIndex()) {
                LOG.info("Successfully created recent index.");
            } else {
                LOG.fatal("Could not create recent index. Terminating.");
                System.exit(1);
            }
        }

        // Statically set timeout for LogglyForwarder.
        // TODO: This is a code smell and needs to be fixed.
        LogglyForwarder.setTimeout(configuration.getForwarderLogglyTimeout());

        scheduler = Executors.newScheduledThreadPool(SCHEDULED_THREADS_POOL_SIZE);

        loadPlugins();
        
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

        activityWriter.write(new Activity("Started up.", GraylogServer.class));
        LOG.info("Graylog2 up and running.");

        while (true) {
            try { Thread.sleep(1000); } catch (InterruptedException e) { /* lol, i don't care */ }
        }

    }
    
    private void loadPlugins() {
        PluginLoader pl = new PluginLoader("plugin");
        for (Class<? extends MessageFilter> filter : pl.loadFilterPlugins()) {
            LOG.info("Registering plugin filter [" + filter.getSimpleName() + "].");
            registerFilter(filter);
            this.loadedFilterPlugins += 1;
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

    public void setRulesEngine(RulesEngineImpl engine) {
        rulesEngine = engine;
    }

    public RulesEngineImpl getRulesEngine() {
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

    @Override
    public Buffer getProcessBuffer() {
        return this.processBuffer;
    }

    @Override
    public Buffer getOutputBuffer() {
        return this.outputBuffer;
    }

    public List<Class<? extends MessageFilter>> getFilters() {
        return this.filters;
    }

    public List<Class<? extends MessageOutput>> getOutputs() {
        return this.outputs;
    }

    public List<Class<? extends CommunicatorMethod>> getCommunicatorMethods() {
        return this.communicatorMethods;
    }
    
    public MessageCounterManagerImpl getMessageCounterManager() {
        return this.messageCounterManager;
    }

    public HostCounterCacheImpl getHostCounterCache() {
        return this.hostCounterCache;
    }
    
    public Deflector getDeflector() {
        return this.deflector;
    }
    
    public Cluster cluster() {
        return this.cluster;
    }
    
    public ActivityWriter getActivityWriter() {
        return this.activityWriter;
    }
    
    @Override
    public boolean isMaster() {
        return this.configuration.isMaster();
    }
    
    @Override
    public String getServerId() {
        return this.serverId;
    }
    
    public int getLoadedFilterPlugins() {
        return this.loadedFilterPlugins;
    }
    
    public void setLocalMode(boolean mode) {
        this.localMode = mode;
    }
   
    public boolean isLocalMode() {
        return localMode;
    }

}

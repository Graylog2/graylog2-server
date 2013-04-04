/**
 * Copyright 2012, 2013 Lennart Koopmann <lennart@socketfeed.com>
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

import org.glassfish.grizzly.http.server.HttpServer;
import org.graylog2.plugin.Tools;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.graylog2.blacklists.BlacklistCache;
import org.graylog2.buffers.OutputBuffer;
import org.graylog2.buffers.ProcessBuffer;
import org.graylog2.database.MongoBridge;
import org.graylog2.database.MongoConnection;
import org.graylog2.indexer.Indexer;
import org.graylog2.plugin.initializers.Initializer;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.gelf.GELFChunkManager;
import org.graylog2.plugin.outputs.MessageOutput;
import org.graylog2.streams.StreamCache;

import com.google.common.collect.Lists;
import java.util.concurrent.atomic.AtomicInteger;
import com.google.common.collect.Maps;
import java.util.Map;

import javax.ws.rs.core.UriBuilder;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.sun.jersey.api.container.grizzly2.GrizzlyServerFactory;
import com.sun.jersey.api.core.PackagesResourceConfig;
import com.sun.jersey.api.core.ResourceConfig;

import org.cliffc.high_scale_lib.Counter;
import org.graylog2.activities.Activity;
import org.graylog2.activities.ActivityWriter;
import org.graylog2.buffers.BasicCache;
import org.graylog2.buffers.Cache;
import org.graylog2.cluster.Cluster;
import org.graylog2.database.HostCounterCacheImpl;
import org.graylog2.indexer.Deflector;
import org.graylog2.initializers.*;
import org.graylog2.inputs.StandardInputSet;
import org.graylog2.plugin.GraylogServer;
import org.graylog2.plugin.alarms.callbacks.AlarmCallback;
import org.graylog2.plugin.alarms.callbacks.AlarmCallbackConfigurationException;
import org.graylog2.plugin.alarms.transports.Transport;
import org.graylog2.plugin.alarms.transports.TransportConfigurationException;
import org.graylog2.plugin.buffers.Buffer;
import org.graylog2.plugin.filters.MessageFilter;
import org.graylog2.plugin.indexer.MessageGateway;
import org.graylog2.plugin.initializers.InitializerConfigurationException;
import org.graylog2.plugin.inputs.MessageInputConfigurationException;
import org.graylog2.plugin.outputs.MessageOutputConfigurationException;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.plugins.PluginConfiguration;
import org.graylog2.plugins.PluginLoader;
import org.graylog2.streams.StreamImpl;

/**
 * Server core, handling and holding basically everything.
 * 
 * (Du kannst das Geraet nicht bremsen, schon garnicht mit bloßen Haenden.)
 * 
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class Core implements GraylogServer {

    private static final Logger LOG = LoggerFactory.getLogger(Core.class);

    private MongoConnection mongoConnection;
    private MongoBridge mongoBridge;
    private Configuration configuration;
    private RulesEngineImpl rulesEngine;
    private ServerValue serverValues;
    private GELFChunkManager gelfChunkManager;

    private static final int SCHEDULED_THREADS_POOL_SIZE = 30;
    private ScheduledExecutorService scheduler;

    public static final String GRAYLOG2_VERSION = "0.20.0-dev";
    public static final String GRAYLOG2_CODENAME = "Amigo Humanos (Flipper)";

    public static final String MASTER_COUNTER_NAME = "master";

    private Indexer indexer;

    private HostCounterCacheImpl hostCounterCache;

    private MessageCounterManagerImpl messageCounterManager;

    private Cluster cluster;
    
    private Counter benchmarkCounter = new Counter();
    
    private List<Initializer> initializers = Lists.newArrayList();
    private List<MessageInput> inputs = Lists.newArrayList();
    private List<MessageFilter> filters = Lists.newArrayList();
    private List<MessageOutput> outputs = Lists.newArrayList();
    private List<Transport> transports = Lists.newArrayList();
    private List<AlarmCallback> alarmCallbacks = Lists.newArrayList();
    
    private ProcessBuffer processBuffer;
    private OutputBuffer outputBuffer;
    private AtomicInteger outputBufferWatermark = new AtomicInteger();
    private AtomicInteger processBufferWatermark = new AtomicInteger();
    
    private Cache inputCache;
    private Cache outputCache;
    
    private Deflector deflector;
    
    private ActivityWriter activityWriter;

    private String serverId;
    
    private boolean localMode = false;
    private boolean statsMode = false;
    
    private int startedAt;

    public void initialize(Configuration configuration) {
    	startedAt = Tools.getUTCTimestamp();
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

        mongoBridge = new MongoBridge(this);
        mongoBridge.setConnection(mongoConnection); // TODO use dependency injection
        mongoConnection.connect();
        
        cluster = new Cluster(this);
        
        activityWriter = new ActivityWriter(this);
        
        messageCounterManager = new MessageCounterManagerImpl();
        messageCounterManager.register(MASTER_COUNTER_NAME);

        hostCounterCache = new HostCounterCacheImpl();
        
        inputCache = new BasicCache();
        outputCache = new BasicCache();
    
        processBuffer = new ProcessBuffer(this, inputCache);
        processBuffer.initialize();

        outputBuffer = new OutputBuffer(this, outputCache);
        outputBuffer.initialize();

        gelfChunkManager = new GELFChunkManager(this);

        indexer = new Indexer(this);
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
            LOG.info("Not registering initializer {} because it is marked as master only.", initializer.getClass().getSimpleName());
            return;
        }
        
        this.initializers.add(initializer);
    }

    public void registerInput(MessageInput input) {
        this.inputs.add(input);
    }

    public void registerFilter(MessageFilter filter) {
        this.filters.add(filter);
    }

    public void registerOutput(MessageOutput output) {
        this.outputs.add(output);
    }
    
    public void registerTransport(Transport transport) {
        this.transports.add(transport);
    }
    
    public void registerAlarmCallback(AlarmCallback alarmCallback) {
        this.alarmCallbacks.add(alarmCallback);
    }

    @Override
    public void run() {

        gelfChunkManager.start();
        BlacklistCache.initialize(this);
        StreamCache.initialize(this);
        
        // Set up deflector.
        LOG.info("Setting up deflector.");
        deflector = new Deflector(this);
        deflector.setUp();

        scheduler = Executors.newScheduledThreadPool(SCHEDULED_THREADS_POOL_SIZE,
                new ThreadFactoryBuilder().setNameFormat("scheduled-%d").build()
        );

        // Load and register plugins.
        loadPlugins(MessageFilter.class, "filters");
        loadPlugins(MessageOutput.class, "outputs");
        loadPlugins(AlarmCallback.class, "alarm_callbacks");
        loadPlugins(Transport.class, "transports");
        loadPlugins(Initializer.class, "initializers");
        loadPlugins(MessageInput.class, "inputs");
        
        // Initialize all registered transports.
        for (Transport transport : this.transports) {
            try {
                Map<String, String> config;
                
                // The built in transport methods get a more convenient configuration from graylog2.conf.
                if (transport.getClass().getCanonicalName().equals("org.graylog2.alarms.transports.EmailTransport")) {
                    config = configuration.getEmailTransportConfiguration();
                } else if (transport.getClass().getCanonicalName().equals("org.graylog2.alarms.transports.JabberTransport")) {
                    config = configuration.getJabberTransportConfiguration();
                } else {
                    // Load custom plugin config.
                    config = PluginConfiguration.load(this, transport.getClass().getCanonicalName());
                }
                
                transport.initialize(config);
                LOG.debug("Initialized transport: {}", transport.getName());
            } catch (TransportConfigurationException e) {
                LOG.error("Could not initialize transport <" + transport.getName() + ">"
                        + " because of missing or invalid configuration.", e);
            }
        }
        
        // Initialize all registered alarm callbacks.
        for (AlarmCallback callback : this.alarmCallbacks) {
            try {
                callback.initialize(PluginConfiguration.load(this, callback.getClass().getCanonicalName()));
                LOG.debug("Initialized alarm callback: {}", callback.getName());
            } catch(AlarmCallbackConfigurationException e) {
                LOG.error("Could not initialize alarm callback <" + callback.getName() + ">"
                        + " because of missing or invalid configuration.", e);
            }
        }
        
        // Initialize all registered initializers.
        for (Initializer initializer : this.initializers) {
            try {
                if (StandardInitializerSet.get().contains(initializer.getClass())) {
                    // This is a built-in initializer. We don't need special configs for them.
                    initializer.initialize(this, null);
                } else {
                    // This is a plugin. Initialize with custom config from Mongo.
                    initializer.initialize(this, PluginConfiguration.load(
                            this,
                            initializer.getClass().getCanonicalName())
                    );
                }
                
                LOG.debug("Initialized initializer: {}", initializer.getClass().getSimpleName());
            } catch (InitializerConfigurationException e) {
                
            }
            
        }

        // Initialize all registered inputs.
        for (MessageInput input : this.inputs) {
            try {
                if (StandardInputSet.get().contains(input.getClass())) {
                    // This is a built-in input. Initialize with config from graylog2.conf.
                    input.initialize(configuration.getInputConfig(input.getClass()), this);
                } else {
                    // This is a plugin. Initialize with custom config from Mongo.
                    input.initialize(PluginConfiguration.load(
                            this,
                            input.getClass().getCanonicalName()),
                            this
                    );
                }
                
                LOG.debug("Initialized input: {}", input.getName());
            } catch (MessageInputConfigurationException e) {
                LOG.error("Could not initialize input <{}>.", input.getClass().getCanonicalName(), e);
            }
        }
        
        // Initialize all registered outputs.
        for (MessageOutput output : this.outputs) {
            try {
                output.initialize(PluginConfiguration.load(this, output.getClass().getCanonicalName()));
                LOG.debug("Initialized output: {}", output.getName());
            } catch(MessageOutputConfigurationException e) {
                LOG.error("Could not initialize output <" + output.getName() + ">"
                        + " because of missing or invalid configuration.", e);
            }
        }

        activityWriter.write(new Activity("Started up.", GraylogServer.class));
        LOG.info("Graylog2 up and running.");

        while (true) {
            try { Thread.sleep(1000); } catch (InterruptedException e) { /* lol, i don't care */ }
        }

    }
    
    public void startRestApi() throws IOException {
        URI restUri = UriBuilder.fromUri(configuration.getRestListenUri()).port(configuration.getRestListenPort()).build();
        startRestServer(restUri);
        LOG.info("Started REST API at <{}>", restUri);
    }
    
    private <A> void loadPlugins(Class<A> type, String subDirectory) {
        PluginLoader<A> pl = new PluginLoader<A>(configuration.getPluginDir(), subDirectory, type);
        for (A plugin : pl.getPlugins()) {
            LOG.info("Registering <{}> plugin [{}].", type.getSimpleName(), plugin.getClass().getCanonicalName());
            
            if (plugin instanceof MessageFilter) {
                registerFilter((MessageFilter) plugin);
            } else if (plugin instanceof MessageOutput) {
                registerOutput((MessageOutput) plugin);
            } else if (plugin instanceof AlarmCallback) {
                registerAlarmCallback((AlarmCallback) plugin);
            } else if (plugin instanceof Initializer) {
                registerInitializer((Initializer) plugin);
            } else if (plugin instanceof MessageInput) {
                registerInput((MessageInput) plugin);
            } else if (plugin instanceof Transport) {
                registerTransport((Transport) plugin);
            } else {
                LOG.error("Could not load plugin [{}] - Not supported type.", plugin.getClass().getCanonicalName());
            }
        }
    }

    private HttpServer startRestServer(URI restUri) throws IOException {
        ResourceConfig rc = new PackagesResourceConfig("org.graylog2.rest.resources");
        rc.getProperties().put("core", this);
        return GrizzlyServerFactory.createHttpServer(restUri, rc);
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
    
    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
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

    public Indexer getIndexer() {
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
    
    public AtomicInteger outputBufferWatermark() {
        return outputBufferWatermark;
    }
    
    public AtomicInteger processBufferWatermark() {
        return processBufferWatermark;
    }

    public List<Initializer> getInitializers() {
        return this.initializers;
    }
    
    public List<Transport> getTransports() {
        return this.transports;
    }
    
    public List<MessageInput> getInputs() {
        return this.inputs;
    }
    
    public List<MessageFilter> getFilters() {
        return this.filters;
    }

    public List<MessageOutput> getOutputs() {
        return this.outputs;
    }

    public List<AlarmCallback> getAlarmCallbacks() {
        return this.alarmCallbacks;
    }
    
    @Override
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
    
    @Override
    public MessageGateway getMessageGateway() {
        return this.indexer.getMessageGateway();
    }
    
    public void setLocalMode(boolean mode) {
        this.localMode = mode;
    }
   
    public boolean isLocalMode() {
        return localMode;
    }

    public void setStatsMode(boolean mode) {
        this.statsMode = mode;
    }
   
    public boolean isStatsMode() {
        return statsMode;
    }
    
    /*
     * For plugins that need a list of all active streams. Could be moved somewhere
     * more appropiate.
     */
    @Override
    public Map<String, Stream> getEnabledStreams() {
        Map<String, Stream> streams = Maps.newHashMap();
        for (Stream stream : StreamImpl.loadAllEnabled(this)) {
            streams.put(stream.getId().toString(), stream);
        }
        
        return streams;
    }
    
    public Counter getBenchmarkCounter() {
        return benchmarkCounter;
    }

    public Cache getInputCache() {
        return inputCache;
    }
    
    public Cache getOutputCache() {
        return outputCache;
    }
    
    public int getStartedAt() {
    	return startedAt;
    }
    
}

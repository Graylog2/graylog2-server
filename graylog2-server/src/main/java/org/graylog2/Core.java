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

import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DBObject;
import org.apache.shiro.mgt.DefaultSecurityManager;
import org.cliffc.high_scale_lib.Counter;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.message.GZipEncoder;
import org.glassfish.jersey.server.ContainerFactory;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.filter.EncodingFilter;
import org.glassfish.jersey.server.internal.scanning.PackageNamesScanner;
import org.graylog2.blacklists.BlacklistCache;
import org.graylog2.buffers.OutputBuffer;
import org.graylog2.buffers.ProcessBuffer;
import org.graylog2.dashboards.DashboardRegistry;
import org.graylog2.database.MongoBridge;
import org.graylog2.database.MongoConnection;
import org.graylog2.indexer.Deflector;
import org.graylog2.indexer.IndexFailure;
import org.graylog2.indexer.Indexer;
import org.graylog2.initializers.Initializers;
import org.graylog2.inputs.BasicCache;
import org.graylog2.inputs.Cache;
import org.graylog2.inputs.InputRegistry;
import org.graylog2.inputs.gelf.gelf.GELFChunkManager;
import org.graylog2.jersey.container.netty.NettyContainer;
import org.graylog2.plugin.lifecycles.Lifecycle;
import org.graylog2.metrics.jersey2.MetricsDynamicBinding;
import org.graylog2.periodical.Periodicals;
import org.graylog2.rest.RestAccessLogFilter;
import org.graylog2.outputs.OutputRegistry;
import org.graylog2.metrics.MongoDbMetricsReporter;
import org.graylog2.plugin.GraylogServer;
import org.graylog2.plugin.InputHost;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.Version;
import org.graylog2.plugin.alarms.callbacks.AlarmCallback;
import org.graylog2.plugin.alarms.transports.Transport;
import org.graylog2.plugin.buffers.Buffer;
import org.graylog2.plugin.filters.MessageFilter;
import org.graylog2.plugin.indexer.MessageGateway;
import org.graylog2.plugin.initializers.Initializer;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.plugin.outputs.MessageOutput;
import org.graylog2.plugin.rest.AnyExceptionClassMapper;
import org.graylog2.plugin.rest.JacksonPropertyExceptionMapper;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.plugin.system.NodeId;
import org.graylog2.plugins.PluginLoader;
import org.graylog2.rest.CORSFilter;
import org.graylog2.rest.ObjectMapperProvider;
import org.graylog2.security.ShiroSecurityBinding;
import org.graylog2.security.ShiroSecurityContextFactory;
import org.graylog2.security.ldap.LdapConnector;
import org.graylog2.security.realm.LdapUserAuthenticator;
import org.graylog2.streams.StreamImpl;
import org.graylog2.system.activities.Activity;
import org.graylog2.system.activities.ActivityWriter;
import org.graylog2.system.jobs.SystemJobManager;
import org.graylog2.system.shutdown.GracefulShutdown;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.jboss.netty.handler.codec.http.HttpRequestDecoder;
import org.jboss.netty.handler.codec.http.HttpResponseEncoder;
import org.jboss.netty.handler.stream.ChunkedWriteHandler;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Server core, handling and holding basically everything.
 * 
 * (Du kannst das Geraet nicht bremsen, schon garnicht mit bloßen Haenden.)
 * 
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class Core implements GraylogServer, InputHost {

    private static final Logger LOG = LoggerFactory.getLogger(Core.class);

    private Lifecycle lifecycle = Lifecycle.UNINITIALIZED;

    private MongoConnection mongoConnection;
    private MongoBridge mongoBridge;
    private Configuration configuration;
    private RulesEngineImpl rulesEngine;
    private GELFChunkManager gelfChunkManager;

    private static final int SCHEDULED_THREADS_POOL_SIZE = 30;
    private ScheduledExecutorService scheduler;
    private ScheduledExecutorService daemonScheduler;

    public static final Version GRAYLOG2_VERSION = ServerVersion.VERSION;
    public static final String GRAYLOG2_CODENAME = "Moose";

    private Indexer indexer;

    private Counter benchmarkCounter = new Counter();
    private Counter throughputCounter = new Counter();
    private AtomicReference<ConcurrentHashMap<String, Counter>> streamThroughput =
            new AtomicReference<ConcurrentHashMap<String, Counter>>(new ConcurrentHashMap<String, Counter>());
    private long throughput = 0;

    private List<MessageFilter> filters = Lists.newArrayList();
    private List<Transport> transports = Lists.newArrayList();
    private List<AlarmCallback> alarmCallbacks = Lists.newArrayList();

    private Initializers initializers;
    private InputRegistry inputs;
    private OutputRegistry outputs;
    private Periodicals periodicals;

    private DashboardRegistry dashboards;
    
    private ProcessBuffer processBuffer;
    private OutputBuffer outputBuffer;
    private AtomicInteger outputBufferWatermark = new AtomicInteger();
    private AtomicInteger processBufferWatermark = new AtomicInteger();
    
    private Cache inputCache;
    private Cache outputCache;
    
    private Deflector deflector;
    
    private ActivityWriter activityWriter;

    private SystemJobManager systemJobManager;

    private String nodeId;
    
    private boolean localMode = false;
    private boolean statsMode = false;

    private AtomicBoolean isProcessing = new AtomicBoolean(true);
    private AtomicBoolean processingPauseLocked = new AtomicBoolean(false);
    
    private DateTime startedAt;
    private MetricRegistry metricRegistry;
    private LdapUserAuthenticator ldapUserAuthenticator;
    private LdapConnector ldapConnector;
    private DefaultSecurityManager securityManager;
    private MongoDbMetricsReporter metricsReporter;
    private AtomicReference<HashMap<String, Counter>> currentStreamThroughput = new AtomicReference<HashMap<String, Counter>>();

    public void initialize(Configuration configuration, MetricRegistry metrics) {
    	startedAt = new DateTime(DateTimeZone.UTC);

        NodeId id = new NodeId(configuration.getNodeIdFile());
        this.nodeId = id.readOrGenerate();

        this.metricRegistry = metrics;
        this.configuration = configuration; // TODO use dependency injection

        if (configuration.isMetricsCollectionEnabled()) {
            metricsReporter = MongoDbMetricsReporter.forRegistry(this, metricRegistry).build();
            metricsReporter.start(1, TimeUnit.SECONDS);
        }

        if (this.configuration.getRestTransportUri() == null) {
            configuration.setRestTransportUri(configuration.getDefaultRestTransportUri().toString());
            LOG.info("No rest_transport_uri set. Falling back to [{}].", configuration.getRestTransportUri().toString());
        }

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

        initializers = new Initializers(this);
        inputs = new InputRegistry(this);
        outputs = new OutputRegistry(this);
        periodicals = new Periodicals(this);

        if (isMaster()) {
            dashboards = new DashboardRegistry(this);
            dashboards.loadPersisted();
        }

        activityWriter = new ActivityWriter(this);

        systemJobManager = new SystemJobManager(this);

        inputCache = new BasicCache();
        outputCache = new BasicCache();
    
        processBuffer = new ProcessBuffer(this, inputCache);
        processBuffer.initialize();

        outputBuffer = new OutputBuffer(this, outputCache);
        outputBuffer.initialize();

        gelfChunkManager = new GELFChunkManager(this);

        // Make sure that the index failures collection is always created capped.
        if(!mongoConnection.getDatabase().collectionExists(IndexFailure.COLLECTION)) {
            DBObject options = BasicDBObjectBuilder.start()
                    .add("capped", true)
                    .add("size", 52428800) // 50MB max size.
                    .get();

            mongoConnection.getDatabase().createCollection(IndexFailure.COLLECTION, options);
        }

        indexer = new Indexer(this);
        indexer.start();

        final Core core = this;
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                String msg = "SIGNAL received. Shutting down.";
                LOG.info(msg);
                activityWriter.write(new Activity(msg, Core.class));

                GracefulShutdown gs = new GracefulShutdown(core);
                gs.run();
            }
        });
    }

    public void registerFilter(MessageFilter filter) {
        this.filters.add(filter);
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

        // Set up deflector.
        LOG.info("Setting up deflector.");
        deflector = new Deflector(this);
        deflector.setUp();

        scheduler = Executors.newScheduledThreadPool(SCHEDULED_THREADS_POOL_SIZE,
                new ThreadFactoryBuilder()
                        .setNameFormat("scheduled-%d")
                        .setDaemon(false)
                        .build()
        );

        daemonScheduler = Executors.newScheduledThreadPool(SCHEDULED_THREADS_POOL_SIZE,
                new ThreadFactoryBuilder()
                        .setNameFormat("scheduled-%d")
                        .setDaemon(true)
                        .build()
        );

        // Load and register plugins.
        registerPlugins(MessageInput.class, "inputs");

        // Ramp it all up. (both plugins and built-in types)
        initializers().initialize();
        outputs().initialize();

        // Load persisted inputs.
        inputs().launchPersisted();
    }

    public void setLdapConnector(LdapConnector ldapConnector) {
        this.ldapConnector = ldapConnector;
    }

    public LdapConnector getLdapConnector() {
        return ldapConnector;
    }

    public DefaultSecurityManager getSecurityManager() {
        return securityManager;
    }

    public void setSecurityManager(DefaultSecurityManager securityManager) {
        this.securityManager = securityManager;
    }

    public void incrementStreamThroughput(String streamId) {
        final ConcurrentHashMap<String, Counter> counterMap = streamThroughput.get();
        Counter counter;
        synchronized (counterMap) {
            counter = counterMap.get(streamId);
            if (counter == null) {
                counter = new Counter();
                counterMap.put(streamId, counter);
            }
        }
        counter.increment();
    }

    public Map<String, Counter> cycleStreamThroughput() {
        return streamThroughput.getAndSet(new ConcurrentHashMap<String, Counter>());
    }

    public void setCurrentStreamThroughput(HashMap<String, Counter> throughput) {
        currentStreamThroughput.set(throughput);
    }

    public HashMap<String, Counter> getCurrentStreamThroughput() {
        return currentStreamThroughput.get();
    }

    private class Graylog2Binder extends AbstractBinder {

        @Override
        protected void configure() {
            bind(metricRegistry).to(MetricRegistry.class);
            bind(Core.this).to(Core.class);
        }
    }


    public void startRestApi() throws IOException {
        final ExecutorService bossExecutor = Executors.newCachedThreadPool(
                new ThreadFactoryBuilder()
                        .setNameFormat("restapi-boss-%d")
                        .build());

        final ExecutorService workerExecutor = Executors.newCachedThreadPool(
                new ThreadFactoryBuilder()
                        .setNameFormat("restapi-worker-%d")
                        .build());

        final ServerBootstrap bootstrap = new ServerBootstrap(new NioServerSocketChannelFactory(
                bossExecutor,
                workerExecutor
        ));

        ResourceConfig rc = new ResourceConfig()
                .property(NettyContainer.PROPERTY_BASE_URI, configuration.getRestListenUri())
                .registerClasses(MetricsDynamicBinding.class,
                        JacksonPropertyExceptionMapper.class,
                        AnyExceptionClassMapper.class,
                        ShiroSecurityBinding.class,
                        RestAccessLogFilter.class)
                .register(new Graylog2Binder())
                .register(ObjectMapperProvider.class)
                .register(JacksonJsonProvider.class)
                .registerFinder(new PackageNamesScanner(new String[]{"org.graylog2.rest.resources"}, true));

        if (configuration.isRestEnableGzip())
            EncodingFilter.enableFor(rc, GZipEncoder.class);

        if (configuration.isRestEnableCors()) {
            LOG.info("Enabling CORS for REST API");
            rc.register(CORSFilter.class);
        }

        /*rc = rc.registerFinder(new PackageNamesScanner(new String[]{"org.graylog2.rest.resources"}, true));*/

        final NettyContainer jerseyHandler = ContainerFactory.createContainer(NettyContainer.class, rc);
        jerseyHandler.setSecurityContextFactory(new ShiroSecurityContextFactory(this));

        bootstrap.setPipelineFactory(new ChannelPipelineFactory() {
            @Override
            public ChannelPipeline getPipeline() throws Exception {
                ChannelPipeline pipeline = Channels.pipeline();
                pipeline.addLast("decoder", new HttpRequestDecoder());
                pipeline.addLast("encoder", new HttpResponseEncoder());
                pipeline.addLast("chunks", new ChunkedWriteHandler());
                pipeline.addLast("jerseyHandler", jerseyHandler);
                return pipeline;
            }
        }) ;
        bootstrap.setOption("child.tcpNoDelay", true);
        bootstrap.setOption("child.keepAlive", true);

        bootstrap.bind(new InetSocketAddress(
                configuration.getRestListenUri().getHost(),
                configuration.getRestListenUri().getPort()
        ));

        LOG.info("Started REST API at <{}>", configuration.getRestListenUri());
    }


    private <A> void registerPlugins(Class<A> type, String subDirectory) {
        PluginLoader<A> pl = new PluginLoader<A>(configuration.getPluginDir(), subDirectory, type);
        for (A plugin : pl.getPlugins()) {
            LOG.info("Loaded <{}> plugin [{}].", type.getSimpleName(), plugin.getClass().getCanonicalName());

            if (plugin instanceof MessageFilter) {
                registerFilter((MessageFilter) plugin);
            } else if (plugin instanceof MessageInput) {
                inputs.register(plugin.getClass(), ((MessageInput) plugin).getName());
            } else if (plugin instanceof MessageOutput) {
                outputs.register((MessageOutput) plugin);
            } else if (plugin instanceof AlarmCallback) {
                registerAlarmCallback((AlarmCallback) plugin);
            } else if (plugin instanceof Initializer) {
                initializers.register((Initializer) plugin);
            } else if (plugin instanceof Transport) {
                registerTransport((Transport) plugin);
            } else {
                LOG.error("Could not load plugin [{}] - Not supported type.", plugin.getClass().getCanonicalName());
            }
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

    public ScheduledExecutorService getDaemonScheduler() {
        return daemonScheduler;
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
    
    public List<Transport> getTransports() {
        return this.transports;
    }
    
    public List<MessageFilter> getFilters() {
        return this.filters;
    }

    public List<AlarmCallback> getAlarmCallbacks() {
        return this.alarmCallbacks;
    }

    public Deflector getDeflector() {
        return this.deflector;
    }

    public ActivityWriter getActivityWriter() {
        return this.activityWriter;
    }

    public SystemJobManager getSystemJobManager() {
        return this.systemJobManager;
    }

    public void setLdapAuthenticator(LdapUserAuthenticator authenticator) {
        this.ldapUserAuthenticator = authenticator;
    }

    public LdapUserAuthenticator getLdapAuthenticator() {
        return ldapUserAuthenticator;
    }

    @Override
    public boolean isMaster() {
        return this.configuration.isMaster();
    }
    
    @Override
    public String getNodeId() {
        return this.nodeId;
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

    public Counter getThroughputCounter() {
        return throughputCounter;
    }

    public void setCurrentThroughput(long x) {
        this.throughput = x;
    }

    public long getCurrentThroughput() {
        return this.throughput;
    }

    public Cache getInputCache() {
        return inputCache;
    }
    
    public Cache getOutputCache() {
        return outputCache;
    }
    
    public DateTime getStartedAt() {
    	return startedAt;
    }

    public void pauseMessageProcessing(boolean locked) {
        isProcessing.set(false);
        setLifecycle(Lifecycle.PAUSED);

        // Never override pause lock if already locked.
        if (!processingPauseLocked.get()) {
            processingPauseLocked.set(locked);
        }
    }

    public void resumeMessageProcessing() throws ProcessingPauseLockedException {
        if (processingPauseLocked()) {
            throw new ProcessingPauseLockedException("Processing pause is locked. Wait until the locking task has finished " +
                    "or manually unlock if you know what you are doing.");
        }

        isProcessing.set(true);
        setLifecycle(Lifecycle.RUNNING);
    }

    public boolean processingPauseLocked() {
        return processingPauseLocked.get();
    }

    public void unlockProcessingPause() {
        processingPauseLocked.set(false);
    }

    public boolean isProcessing() {
        return isProcessing.get();
    }

    public MetricRegistry metrics() {
        return metricRegistry;
    }

    /**
     * Shortcut to delete an index. This is for plugin compat. We seriously need proper DI.
     *
     * @param indexName The name of the index to delete.
     */
    @Override
    public void deleteIndexShortcut(String indexName) {
        getIndexer().indices().delete(indexName);
    }

    /**
     * Shortcut to close an index. This is for plugin compat. We seriously need proper DI.
     *
     * @param indexName The name of the index to close.
     */
    @Override
    public void closeIndexShortcut(String indexName) {
        getIndexer().indices().close(indexName);
    }

    public Initializers initializers() {
        return initializers;
    }

    public InputRegistry inputs() {
        return inputs;
    }

    public OutputRegistry outputs() {
        return outputs;
    }

    public Periodicals periodicals() {
        return periodicals;
    }

    public DashboardRegistry dashboards() {
        if (!isMaster()) {
            throw new RuntimeException("Dashboards can only be accessed on master nodes.");
        }

        return dashboards;
    }

    public Lifecycle getLifecycle() {
        return lifecycle;
    }

    public void setLifecycle(Lifecycle lifecycle) {
        this.lifecycle = lifecycle;
    }

}

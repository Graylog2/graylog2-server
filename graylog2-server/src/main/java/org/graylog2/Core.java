/*
 * Copyright 2012-2014 TORCH GmbH
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
 */

package org.graylog2;

import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Injector;
import com.google.inject.internal.util.$Nullable;
import com.google.inject.name.Named;
import org.apache.shiro.mgt.DefaultSecurityManager;
import org.cliffc.high_scale_lib.Counter;
import org.glassfish.hk2.extension.ServiceLocatorGenerator;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.internal.inject.Injections;
import org.glassfish.jersey.message.GZipEncoder;
import org.glassfish.jersey.server.ContainerFactory;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.filter.EncodingFilter;
import org.glassfish.jersey.server.internal.scanning.PackageNamesScanner;
import org.graylog2.alerts.AlertSender;
import org.graylog2.blacklists.BlacklistCache;
import org.graylog2.buffers.Buffers;
import org.graylog2.buffers.OutputBuffer;
import org.graylog2.buffers.OutputBufferWatermark;
import org.graylog2.buffers.processors.ServerProcessBufferProcessor;
import org.graylog2.caches.Caches;
import org.graylog2.dashboards.DashboardRegistry;
import org.graylog2.database.MongoConnection;
import org.graylog2.indexer.Deflector;
import org.graylog2.indexer.Indexer;
import org.graylog2.indexer.ranges.RebuildIndexRangesJob;
import org.graylog2.initializers.Initializers;
import org.graylog2.inputs.ServerInputRegistry;
import org.graylog2.inputs.gelf.gelf.GELFChunkManager;
import org.graylog2.jersey.container.netty.NettyContainer;
import org.graylog2.jersey.container.netty.SecurityContextFactory;
import org.graylog2.metrics.MongoDbMetricsReporter;
import org.graylog2.metrics.jersey2.MetricsDynamicBinding;
import org.graylog2.outputs.OutputRegistry;
import org.graylog2.periodical.Periodicals;
import org.graylog2.plugin.*;
import org.graylog2.plugin.alarms.callbacks.AlarmCallback;
import org.graylog2.plugin.alarms.transports.Transport;
import org.graylog2.plugin.filters.MessageFilter;
import org.graylog2.plugin.initializers.Initializer;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.plugin.outputs.MessageOutput;
import org.graylog2.plugin.rest.AnyExceptionClassMapper;
import org.graylog2.plugin.rest.JacksonPropertyExceptionMapper;
import org.graylog2.plugins.LegacyPluginLoader;
import org.graylog2.rest.CORSFilter;
import org.graylog2.rest.ObjectMapperProvider;
import org.graylog2.rest.RestAccessLogFilter;
import org.graylog2.security.ShiroSecurityBinding;
import org.graylog2.security.ldap.LdapConnector;
import org.graylog2.security.realm.LdapUserAuthenticator;
import org.graylog2.shared.ServerStatus;
import org.graylog2.shared.bindings.OwnServiceLocatorGenerator;
import org.graylog2.shared.buffers.ProcessBuffer;
import org.graylog2.shared.buffers.ProcessBufferWatermark;
import org.graylog2.shared.buffers.processors.ProcessBufferProcessor;
import org.graylog2.shared.filters.FilterRegistry;
import org.graylog2.shared.plugins.PluginLoader;
import org.graylog2.shared.stats.ThroughputStats;
import org.graylog2.system.activities.Activity;
import org.graylog2.system.activities.ActivityWriter;
import org.graylog2.system.jobs.SystemJobFactory;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Server core, handling and holding basically everything.
 * 
 * (Du kannst das Geraet nicht bremsen, schon garnicht mit bloßen Haenden.)
 * 
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class Core implements GraylogServer {

    private static final Logger LOG = LoggerFactory.getLogger(Core.class);

    @Inject
    private MongoConnection mongoConnection;
    @Inject
    private Configuration configuration;
    @Inject
    private ServerStatus serverStatus;
//    @Inject @$Nullable
//    private RulesEngine rulesEngine;

    @Inject
    private GELFChunkManager gelfChunkManager;

    @Inject
    @Named("scheduler")
    private ScheduledExecutorService scheduler;

    @Inject
    @Named("daemonScheduler")
    private ScheduledExecutorService daemonScheduler;

    public static final Version GRAYLOG2_VERSION = ServerVersion.VERSION;
    public static final String GRAYLOG2_CODENAME = "Moose";

    @Inject
    private Indexer indexer;

//    private Counter benchmarkCounter = new Counter();
//    private Counter throughputCounter = new Counter();
    @Inject
    private FilterRegistry filterRegistry;

    private List<Transport> transports = Lists.newArrayList();
    private List<AlarmCallback> alarmCallbacks = Lists.newArrayList();

    @Inject
    private Initializers initializers;
    @Inject
    private ServerInputRegistry inputs;

    @Inject
    private OutputRegistry outputs;
    @Inject
    private Periodicals periodicals;

    @Inject
    private ProcessBuffer processBuffer;
    @Inject
    private OutputBuffer outputBuffer;

//    @Inject
//    private OutputBufferWatermark outputBufferWatermark;
    @Inject
    private ProcessBufferWatermark processBufferWatermark;
    
    @Inject
    private Deflector deflector;
    
    @Inject
    private ActivityWriter activityWriter;

//    @Inject
//    private SystemJobManager systemJobManager;

//    private boolean localMode = false;
//    private boolean statsMode = false;

    @Inject
    private MetricRegistry metricRegistry;
//    private LdapUserAuthenticator ldapUserAuthenticator;
//    private LdapConnector ldapConnector;
//    private DefaultSecurityManager securityManager;
//    private MongoDbMetricsReporter metricsReporter;

//    @Inject
//    private ProcessBuffer.Factory processBufferFactory;
    @Inject
    private ServerProcessBufferProcessor.Factory processBufferProcessorFactory;
//    @Inject
//    private OutputBuffer.Factory outputBufferFactory;

//    @Inject
//    private ThroughputStats throughputStats;

    @Inject
    private DashboardRegistry dashboardRegistry;

    @Inject
    private Buffers bufferSynchronizer;

    @Inject
    private Caches cacheSynchronizer;

//    @Inject
//    private SystemJobFactory systemJobFactory;

//    @Inject
//    private RebuildIndexRangesJob.Factory rebuildIndexRangesJobFactory;

//    @Inject
//    private AlertSender alertSender;

    @Inject
    private SecurityContextFactory shiroSecurityContextFactory;

    public void initialize() {
        final MongoDbMetricsReporter metricsReporter;
        if (configuration.isMetricsCollectionEnabled()) {
            metricsReporter = MongoDbMetricsReporter.forRegistry(metricRegistry, mongoConnection, serverStatus).build();
            metricsReporter.start(1, TimeUnit.SECONDS);
        }

        if (this.configuration.getRestTransportUri() == null) {
                String guessedIf;
                try {
                    guessedIf = Tools.guessPrimaryNetworkAddress().getHostAddress();
                } catch (Exception e) {
                    LOG.error("Could not guess primary network address for rest_transport_uri. Please configure it in your graylog2.conf.", e);
                    throw new RuntimeException("No rest_transport_uri.");
                }

                String transportStr = "http://" + guessedIf + ":" + configuration.getRestListenUri().getPort();
                LOG.info("No rest_transport_uri set. Falling back to [{}].", transportStr);
                this.configuration.setRestTransportUri(transportStr);
        }

        if (serverStatus.hasCapability(ServerStatus.Capability.MASTER)) {
            dashboardRegistry.loadPersisted();
        }

        outputBuffer.initialize();

        int processBufferProcessorCount = configuration.getProcessBufferProcessors();

        ProcessBufferProcessor[] processors = new ProcessBufferProcessor[processBufferProcessorCount];

        for (int i = 0; i < processBufferProcessorCount; i++) {
            processors[i] = processBufferProcessorFactory.create(outputBuffer, this, processBufferWatermark, i, processBufferProcessorCount);
        }

        processBuffer.initialize(processors, configuration.getRingSize(),
                configuration.getProcessorWaitStrategy(),
                configuration.getProcessBufferProcessors()
        );

        indexer.start();

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                String msg = "SIGNAL received. Shutting down.";
                LOG.info(msg);
                activityWriter.write(new Activity(msg, Core.class));

                GracefulShutdown gs = new GracefulShutdown(serverStatus, activityWriter, configuration,
                        bufferSynchronizer, cacheSynchronizer, indexer, periodicals, inputs);
                gs.run();
            }
        });
    }

    private void registerTransport(Transport transport) {
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
        deflector.setUp(indexer);

        // Load and register plugins.
        registerPlugins(MessageInput.class, "inputs");

        // Ramp it all up. (both plugins and built-in types)
        initializers.initialize();
        outputs.initialize();

        // Load persisted inputs.
        inputs.launchAllPersisted();
    }

    private class Graylog2Binder extends AbstractBinder {
        @Override
        protected void configure() {
            //bind(Core.this).to(Core.class);
            /*bind(metricRegistry).to(MetricRegistry.class);
            bind(throughputStats).to(ThroughputStats.class);
            bind(new StreamServiceImpl(mongoConnection)).to(StreamService.class);
            bind(new StreamRuleServiceImpl(mongoConnection)).to(StreamRuleService.class);
            bind(new DashboardServiceImpl(mongoConnection)).to(DashboardService.class);
            bind(new NodeServiceImpl(mongoConnection)).to(NodeService.class);
            bind(new LdapSettingsServiceImpl(mongoConnection)).to(LdapSettingsService.class);
            bind(new SystemMessageServiceImpl(mongoConnection)).to(SystemMessageService.class);
            bind(new NotificationServiceImpl(mongoConnection)).to(NotificationService.class);
            bind(new InputServiceImpl(mongoConnection)).to(InputService.class);
            bind(new AlertServiceImpl(mongoConnection)).to(AlertService.class);
            bind(new UserServiceImpl(mongoConnection, configuration)).to(UserService.class);
            bind(new AccessTokenServiceImpl(mongoConnection)).to(AccessTokenService.class);
            bind(new IndexRangeServiceImpl(mongoConnection, activityWriter)).to(IndexRangeService.class);
            bind(new SavedSearchServiceImpl(mongoConnection)).to(SavedSearchService.class);
            bind(new IndexFailureServiceImpl(mongoConnection)).to(IndexFailureService.class);
            bind(dashboardRegistry).to(DashboardRegistry.class);
            bind(activityWriter).to(ActivityWriter.class);
            bind(serverStatus).to(ServerStatus.class);
            bind(outputBufferWatermark).to(OutputBufferWatermark.class);
            bind(processBufferWatermark).to(ProcessBufferWatermark.class);
            bind(deflector).to(Deflector.class);
            bind(indexer).to(Indexer.class);
            bind(systemJobFactory).to(SystemJobFactory.class);
            bind(bufferSynchronizer).to(Buffers.class);
            bind(configuration).to(Configuration.class);
            bind(systemJobManager).to(SystemJobManager.class);
            bind(rebuildIndexRangesJobFactory).to(RebuildIndexRangesJob.Factory.class);
            bind(cacheSynchronizer).to(Caches.class);
            bind(inputs).to(ServerInputRegistry.class);
            bind(alertSender).to(AlertSender.class);
            bind(periodicals).to(Periodicals.class);
            bind(mongoConnection).to(MongoConnection.class);
            bind(serverStatus.getNodeId()).to(NodeId.class);
            bind((InputCache)getInputCache()).to(InputCache.class);
            bind((OutputCache)getOutputCache()).to(OutputCache.class);
            bind(processBuffer).to(ProcessBuffer.class);
            bind(outputBuffer).to(OutputBuffer.class);*/
        }
    }

    public void startRestApi(Injector injector) throws IOException {
        ServiceLocatorGenerator ownGenerator = new OwnServiceLocatorGenerator(injector);
        try {
            Field field = Injections.class.getDeclaredField("generator");
            field.setAccessible(true);
            Field modifiers = Field.class.getDeclaredField("modifiers");
            modifiers.setAccessible(true);
            modifiers.setInt(field, field.getModifiers() & ~Modifier.FINAL);

            field.set(null, ownGenerator);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            LOG.error("Monkey patching Jersey's HK2 failed: ", e);
            System.exit(-1);
        }

        /*ServiceLocatorFactory factory = ServiceLocatorFactory.getInstance();
        factory.addListener(new HK2ServiceLocatorListener(injector));*/

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
        jerseyHandler.setSecurityContextFactory(shiroSecurityContextFactory);

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
        LegacyPluginLoader<A> pl = new LegacyPluginLoader<A>(configuration.getPluginDir(), subDirectory, type);
        for (A plugin : pl.getPlugins()) {
            LOG.info("Loaded <{}> plugin [{}].", type.getSimpleName(), plugin.getClass().getCanonicalName());

            if (plugin instanceof MessageFilter) {
                filterRegistry.register((MessageFilter) plugin);
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

        PluginLoader pluginLoader = new PluginLoader(new File(configuration.getPluginDir()));
        for (Plugin plugin : pluginLoader.loadPlugins()) {
            for (Class<? extends MessageInput> inputClass : plugin.inputs()) {
                final MessageInput messageInput;
                try {
                    messageInput = inputClass.newInstance();
                    inputs.register(inputClass, messageInput.getName());
                } catch (Exception e) {
                    LOG.error("Unable to register message input " + inputClass.getCanonicalName(), e);
                }
            }
        }
    }

    public MongoConnection getMongoConnection() {
        return mongoConnection;
    }

    @Override
    public String getNodeId() {
        return serverStatus.getNodeId().toString();
    }
    
    public MetricRegistry metrics() {
        return metricRegistry;
    }
}

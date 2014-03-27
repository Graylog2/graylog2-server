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

package org.graylog2.radio;

import com.beust.jcommander.internal.Lists;
import com.codahale.metrics.MetricRegistry;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.name.Named;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.server.ContainerFactory;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.internal.scanning.PackageNamesScanner;
import org.graylog2.inputs.BasicCache;
import org.graylog2.inputs.Cache;
import org.graylog2.inputs.gelf.gelf.GELFChunkManager;
import org.graylog2.jersey.container.netty.NettyContainer;
import org.graylog2.plugin.GraylogServer;
import org.graylog2.plugin.InputHost;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.Version;
import org.graylog2.plugin.buffers.Buffer;
import org.graylog2.plugin.filters.MessageFilter;
import org.graylog2.plugin.lifecycles.Lifecycle;
import org.graylog2.plugin.rest.AnyExceptionClassMapper;
import org.graylog2.radio.buffers.processors.RadioProcessBufferProcessor;
import org.graylog2.radio.cluster.Ping;
import org.graylog2.radio.inputs.RadioInputRegistry;
import org.graylog2.radio.transports.RadioTransport;
import org.graylog2.radio.transports.kafka.KafkaProducer;
import org.graylog2.shared.ProcessingHost;
import org.graylog2.shared.ServerStatus;
import org.graylog2.shared.buffers.ProcessBuffer;
import org.graylog2.shared.buffers.processors.ProcessBufferProcessor;
import org.graylog2.shared.inputs.InputRegistry;
import org.graylog2.shared.stats.ThroughputStats;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.jboss.netty.handler.codec.http.HttpRequestDecoder;
import org.jboss.netty.handler.codec.http.HttpResponseEncoder;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class Radio implements InputHost, GraylogServer, ProcessingHost {

    private static final Logger LOG = LoggerFactory.getLogger(Radio.class);

    public static final Version VERSION = RadioVersion.VERSION;

    @Inject
    private MetricRegistry metricRegistry;
    @Inject
    private Configuration configuration;
    @Inject
    private ServerStatus serverStatus;

    private GELFChunkManager gelfChunkManager;

    @Inject
    @Named("scheduler")
    private ScheduledExecutorService scheduler;

    private RadioInputRegistry inputs;
    private Cache inputCache;
    private ProcessBuffer processBuffer;

    @Inject
    @Named("processBufferWatermark")
    private AtomicInteger processBufferWatermark;

    private Ping.Pinger pinger;

    private final AsyncHttpClient httpClient;

    @Inject
    private ProcessBuffer.Factory processBufferFactory;
    @Inject
    private RadioProcessBufferProcessor.Factory processBufferProcessorFactory;
    @Inject
    private ThroughputStats throughputStats;

    public Radio() {
        AsyncHttpClientConfig.Builder builder = new AsyncHttpClientConfig.Builder();
        builder.setAllowPoolingConnection(false);
        httpClient = new AsyncHttpClient(builder.build());
    }

    public void initialize() {
        gelfChunkManager = new GELFChunkManager(this);
        gelfChunkManager.start();

        inputCache = new BasicCache();

        RadioTransport transport = new KafkaProducer(this);

        int processBufferProcessorCount = configuration.getProcessBufferProcessors();
        this.inputs = new RadioInputRegistry(this,
                this.getHttpClient(),
                this.getConfiguration().getGraylog2ServerUri()
        );

        ProcessBufferProcessor[] processors = new ProcessBufferProcessor[processBufferProcessorCount];

        for (int i = 0; i < processBufferProcessorCount; i++) {
            processors[i] = processBufferProcessorFactory.create(this.processBufferWatermark, i, processBufferProcessorCount, transport);
        }

        processBuffer = processBufferFactory.create(inputCache, processBufferWatermark);
        processBuffer.initialize(processors, configuration.getRingSize(),
                configuration.getProcessorWaitStrategy(),
                configuration.getProcessBufferProcessors()
        );

        this.inputs = new RadioInputRegistry(this,
                this.getHttpClient(),
                configuration.getGraylog2ServerUri()
        );

        if (this.configuration.getRestTransportUri() == null) {
            String guessedIf;
            try {
                guessedIf = Tools.guessPrimaryNetworkAddress().getHostAddress();
            } catch (Exception e) {
                LOG.error("Could not guess primary network address for rest_transport_uri. Please configure it in your graylog2-radio.conf.", e);
                throw new RuntimeException("No rest_transport_uri.");
            }

            String transportStr = "http://" + guessedIf + ":" + configuration.getRestListenUri().getPort();
            LOG.info("No rest_transport_uri set. Falling back to [{}].", transportStr);
            this.configuration.setRestTransportUri(transportStr);
        }

        pinger = new Ping.Pinger(httpClient, serverStatus.getNodeId().toString(), configuration.getRestTransportUri(), configuration.getGraylog2ServerUri());

        // TODO: fix this.
        /*ThroughputCounterManagerThread tt = throughputCounterThreadFactory.create();
        scheduler.scheduleAtFixedRate(tt, 0, 1, TimeUnit.SECONDS);*/

        /*MasterCacheWorkerThread masterCacheWorker = new MasterCacheWorkerThread(this, inputCache, processBuffer);
        scheduler.scheduleAtFixedRate(masterCacheWorker, 0, 1, TimeUnit.SECONDS);*/
    }

    public void launchPersistedInputs() throws InterruptedException, ExecutionException, IOException {
        inputs.launchAllPersisted();
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

        ResourceConfig rc = new ResourceConfig();
        rc.property(NettyContainer.PROPERTY_BASE_URI, configuration.getRestListenUri());
        rc.registerClasses(AnyExceptionClassMapper.class);
        rc.register(new Graylog2Binder());
        rc.registerFinder(new PackageNamesScanner(new String[] {"org.graylog2.radio.rest.resources"}, true));
        final NettyContainer jerseyHandler = ContainerFactory.createContainer(NettyContainer.class, rc);

        bootstrap.setPipelineFactory(new ChannelPipelineFactory() {
            @Override
            public ChannelPipeline getPipeline() throws Exception {
                ChannelPipeline pipeline = Channels.pipeline();
                pipeline.addLast("decoder", new HttpRequestDecoder());
                pipeline.addLast("encoder", new HttpResponseEncoder());
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
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                bootstrap.releaseExternalResources();
            }
        });

        LOG.info("Started REST API at <{}>", configuration.getRestListenUri());
    }

    public void startPings() {
        // Start regular pings.
        scheduler.scheduleAtFixedRate(pinger, 0, 1, TimeUnit.SECONDS);
    }

    public void ping() {
        pinger.ping();
    }

    public boolean isProcessing() {
        return true;
    }

    private class Graylog2Binder extends AbstractBinder {

        @Override
        protected void configure() {
            bind(Radio.this).to(Radio.class);
            bind(metricRegistry).to(MetricRegistry.class);
            bind(throughputStats).to(ThroughputStats.class);
            bind(configuration).to(Configuration.class);
            bind(serverStatus).to(ServerStatus.class);
        }

    }

    @Override
    public Buffer getProcessBuffer() {
        return processBuffer;
    }

    public AtomicInteger processBufferWatermark() {
        return processBufferWatermark;
    }

    public String getNodeId() {
        return serverStatus.getNodeId().toString();
    }

    @Override
    public GELFChunkManager getGELFChunkManager() {
        return this.gelfChunkManager;
    }

    @Override
    public MetricRegistry metrics() {
        return metricRegistry;
    }

    public DateTime getStartedAt() {
        return serverStatus.getStartedAt();
    }

    public InputRegistry inputs() {
        return inputs;
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    public Cache getInputCache() {
        return inputCache;
    }

    private AsyncHttpClient getHttpClient() {
        return httpClient;
    }

    public List<MessageFilter> getFilters() {
        List<MessageFilter> result = Lists.newArrayList();
        return result;
    }

    @Override
    public void closeIndexShortcut(String indexName) {
    }

    @Override
    public void deleteIndexShortcut(String indexName) {
    }

    @Override
    public boolean isMaster() {
        return false;
    }

    @Override
    public Buffer getOutputBuffer() {
        return null;
    }

    @Override
    public void run() {
    }

    @Override
    public boolean isServer() {
        return serverStatus.hasCapability(ServerStatus.Capability.SERVER);
    }

    @Override
    public boolean isRadio() {
        return serverStatus.hasCapability(ServerStatus.Capability.RADIO);
    }

    public Lifecycle getLifecycle() {
        return serverStatus.getLifecycle();
    }

    public void setLifecycle(Lifecycle lifecycle) {
        serverStatus.setLifecycle(lifecycle);
    }

}

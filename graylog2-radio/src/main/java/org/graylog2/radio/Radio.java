/**
 * Copyright 2013 Lennart Koopmann <lennart@torch.sh>
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
package org.graylog2.radio;

import com.codahale.metrics.MetricRegistry;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;
import org.cliffc.high_scale_lib.Counter;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.server.ContainerFactory;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.internal.scanning.PackageNamesScanner;
import org.graylog2.inputs.BasicCache;
import org.graylog2.inputs.Cache;
import org.graylog2.inputs.gelf.gelf.GELFChunkManager;
import org.graylog2.jersey.container.netty.NettyContainer;
import org.graylog2.plugin.InputHost;
import org.graylog2.plugin.Version;
import org.graylog2.plugin.buffers.Buffer;
import org.graylog2.plugin.lifecycles.Lifecycle;
import org.graylog2.plugin.rest.AnyExceptionClassMapper;
import org.graylog2.plugin.system.NodeId;
import org.graylog2.radio.buffers.ProcessBuffer;
import org.graylog2.radio.cluster.Ping;
import org.graylog2.radio.inputs.InputRegistry;
import org.graylog2.radio.periodical.MasterCacheWorkerThread;
import org.graylog2.radio.periodical.ThroughputCounterManagerThread;
import org.graylog2.radio.transports.RadioTransport;
import org.graylog2.radio.transports.amqp.AMQPProducer;
import org.graylog2.radio.transports.kafka.KafkaProducer;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.jboss.netty.handler.codec.http.HttpRequestDecoder;
import org.jboss.netty.handler.codec.http.HttpResponseEncoder;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class Radio implements InputHost {

    private static final Logger LOG = LoggerFactory.getLogger(Radio.class);

    public static final Version VERSION = RadioVersion.VERSION;

    private Lifecycle lifecycle = Lifecycle.UNINITIALIZED;

    private DateTime startedAt;
    private MetricRegistry metricRegistry;
    private Configuration configuration;

    private GELFChunkManager gelfChunkManager;

    private static final int SCHEDULED_THREADS_POOL_SIZE = 10;
    private ScheduledExecutorService scheduler;

    private InputRegistry inputs;
    private Cache inputCache;
    private ProcessBuffer processBuffer;
    private AtomicInteger processBufferWatermark = new AtomicInteger();

    private Counter throughputCounter = new Counter();
    private long throughput = 0;

    private Ping.Pinger pinger;

    private final AsyncHttpClient httpClient;

    private RadioTransport transport;

    private String nodeId;

    public Radio() {
        AsyncHttpClientConfig.Builder builder = new AsyncHttpClientConfig.Builder();
        builder.setAllowPoolingConnection(false);
        httpClient = new AsyncHttpClient(builder.build());
    }

    public void initialize(Configuration configuration, MetricRegistry metrics) {
        startedAt = new DateTime(DateTimeZone.UTC);

        this.metricRegistry = metrics;
        this.configuration = configuration;

        NodeId id = new NodeId(configuration.getNodeIdFile());
        this.nodeId = id.readOrGenerate();

        gelfChunkManager = new GELFChunkManager(this);
        gelfChunkManager.start();

        inputCache = new BasicCache();
        processBuffer = new ProcessBuffer(this, inputCache);
        processBuffer.initialize();

        // Set up transport.
        switch (configuration.getTransportType()) {
            case AMQP:
                transport = new AMQPProducer(this);
                break;
            case KAFKA:
                transport = new KafkaProducer(this);
                break;
            default:
                throw new RuntimeException("Cannot map transport type to transport.");
        }

        this.inputs = new InputRegistry(this);

        if (this.configuration.getRestTransportUri() == null) {
            this.configuration.setRestTransportUri(configuration.getDefaultRestTransportUri().toString());
            LOG.info("No rest_transport_uri set. Falling back to [{}].", configuration.getRestTransportUri().toString());
        }

        pinger = new Ping.Pinger(httpClient, nodeId, configuration.getRestTransportUri(), configuration.getGraylog2ServerUri());

        scheduler = Executors.newScheduledThreadPool(SCHEDULED_THREADS_POOL_SIZE,
                new ThreadFactoryBuilder().setNameFormat("scheduled-%d").build()
        );

        ThroughputCounterManagerThread tt = new ThroughputCounterManagerThread(this);
        scheduler.scheduleAtFixedRate(tt, 0, 1, TimeUnit.SECONDS);

        MasterCacheWorkerThread masterCacheWorker = new MasterCacheWorkerThread(this, inputCache, processBuffer);
        scheduler.scheduleAtFixedRate(masterCacheWorker, 0, 1, TimeUnit.SECONDS);
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
            bind(metricRegistry).to(MetricRegistry.class);
            bind(Radio.this).to(Radio.class);
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
        return nodeId;
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
        return startedAt;
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

    public Counter getThroughputCounter() {
        return throughputCounter;
    }

    public void setCurrentThroughput(long x) {
        this.throughput = x;
    }

    public long getCurrentThroughput() {
        return this.throughput;
    }

    public RadioTransport getTransport() {
        return transport;
    }

    public AsyncHttpClient getHttpClient() {
        return httpClient;
    }

    public Lifecycle getLifecycle() {
        return lifecycle;
    }

    public void setLifecycle(Lifecycle lifecycle) {
        this.lifecycle = lifecycle;
    }

}

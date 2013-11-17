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
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.server.ContainerFactory;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.internal.scanning.PackageNamesScanner;
import org.graylog2.jersey.container.netty.NettyContainer;
import org.graylog2.plugin.InputHost;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.Version;
import org.graylog2.plugin.buffers.Buffer;
import org.graylog2.plugin.rest.AnyExceptionClassMapper;
import org.graylog2.plugin.system.NodeId;
import org.graylog2.radio.cluster.Ping;
import org.graylog2.radio.inputs.InputRegistry;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class Radio implements InputHost {

    private static final Logger LOG = LoggerFactory.getLogger(Radio.class);

    public static final Version VERSION = RadioVersion.VERSION;

    private DateTime startedAt;
    private MetricRegistry metricRegistry;
    private Configuration configuration;

    private static final int SCHEDULED_THREADS_POOL_SIZE = 10;
    private ScheduledExecutorService scheduler;

    private InputRegistry inputs;

    private final AsyncHttpClient httpClient;

    private String nodeId;

    public Radio() {
        AsyncHttpClientConfig.Builder builder = new AsyncHttpClientConfig.Builder();
        builder.setAllowPoolingConnection(false);
        httpClient = new AsyncHttpClient(builder.build());
    }

    public void initialize(Configuration configuration, MetricRegistry metrics) {
        startedAt = new DateTime(DateTimeZone.UTC);

        NodeId id = new NodeId(configuration.getNodeIdFile());
        this.nodeId = id.readOrGenerate();

        this.inputs = new InputRegistry(this);

        this.metricRegistry = metrics;

        this.configuration = configuration;

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

        scheduler = Executors.newScheduledThreadPool(SCHEDULED_THREADS_POOL_SIZE,
                new ThreadFactoryBuilder().setNameFormat("scheduled-%d").build()
        );

        // Start regular pings.
        Ping.Pinger pinger = new Ping.Pinger(httpClient, nodeId, configuration.getRestTransportUri(), configuration.getGraylog2ServerUri());
        scheduler.scheduleAtFixedRate(pinger, 0, 5, TimeUnit.SECONDS);
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

        bootstrap.bind(new InetSocketAddress(configuration.getRestListenUri().getPort()));
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                bootstrap.releaseExternalResources();
            }
        });

        LOG.info("Started REST API at <{}>", configuration.getRestListenUri());
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
        return null;
    }

    public String getNodeId() {
        return nodeId;
    }

    @Override
    public Object getGELFChunkManager() {
        return null;
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

}

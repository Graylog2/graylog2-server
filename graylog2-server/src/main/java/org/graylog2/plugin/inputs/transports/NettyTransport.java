/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.plugin.inputs.transports;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.MetricSet;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.socket.DatagramChannelConfig;
import io.netty.util.concurrent.GlobalEventExecutor;
import org.graylog2.inputs.transports.netty.ExceptionLoggingChannelHandler;
import org.graylog2.inputs.transports.netty.MessageAggregationHandler;
import org.graylog2.inputs.transports.netty.PromiseFailureHandler;
import org.graylog2.inputs.transports.netty.RawMessageHandler;
import org.graylog2.plugin.LocalMetricRegistry;
import org.graylog2.plugin.MetricSets;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.plugin.inputs.codecs.CodecAggregator;
import org.graylog2.plugin.inputs.util.PacketInformationDumper;
import org.graylog2.plugin.inputs.util.ThroughputCounter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.net.InetSocketAddress;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Callable;

public abstract class NettyTransport implements Transport {
    public static final String CK_BIND_ADDRESS = "bind_address";
    public static final String CK_PORT = "port";
    public static final String CK_RECV_BUFFER_SIZE = "recv_buffer_size";

    private static final Logger log = LoggerFactory.getLogger(NettyTransport.class);

    protected final EventLoopGroup eventLoopGroup;
    protected final MetricRegistry localRegistry;

    protected final InetSocketAddress socketAddress;
    protected final ThroughputCounter throughputCounter;
    private final int recvBufferSize;

    protected final ChannelGroup channels;

    @Nullable
    private CodecAggregator aggregator;

    public NettyTransport(Configuration configuration,
                          EventLoopGroup eventLoopGroup,
                          ThroughputCounter throughputCounter,
                          LocalMetricRegistry localRegistry) {
        this.throughputCounter = throughputCounter;

            final String hostname = configuration.getString(CK_BIND_ADDRESS);
        if (hostname != null && configuration.intIsSet(CK_PORT)) {
            this.socketAddress = new InetSocketAddress(hostname, configuration.getInt(CK_PORT));
        } else {
            this.socketAddress = null;
        }
        this.recvBufferSize = configuration.intIsSet(CK_RECV_BUFFER_SIZE)
                ? configuration.getInt(CK_RECV_BUFFER_SIZE)
                : MessageInput.getDefaultRecvBufferSize();

        this.eventLoopGroup = eventLoopGroup;
        this.channels = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

        this.localRegistry = localRegistry;
        localRegistry.registerAll(MetricSets.of(throughputCounter.gauges()));
    }

    protected ChannelInitializer<? extends Channel> getChannelInitializer(final LinkedHashMap<String, Callable<? extends ChannelHandler>> handlerList) {
        return new ChannelInitializer<Channel>() {
            @Override
            protected void initChannel(Channel ch) throws Exception {
                final ChannelPipeline p = ch.pipeline();
                for (final Map.Entry<String, Callable<? extends ChannelHandler>> entry : handlerList.entrySet()) {
                    p.addLast(entry.getKey(), entry.getValue().call());
                }
            }
        };
    }

    @Override
    public void setMessageAggregator(@Nullable CodecAggregator aggregator) {
        this.aggregator = aggregator;
    }

    /**
     * Subclasses can override this to add additional ChannelHandlers to the pipeline to support additional features.
     * <p/>
     * Some common use cases are to add SSL/TLS, connection counters or throttling traffic shapers.
     *
     * @param input
     * @return the list of initial channelhandlers to add to the {@link org.jboss.netty.channel.ChannelPipelineFactory}
     */
    protected LinkedHashMap<String, Callable<? extends ChannelHandler>> getBaseChannelHandlers(final MessageInput input) {
        LinkedHashMap<String, Callable<? extends ChannelHandler>> handlerList = new LinkedHashMap<>();

        handlerList.put("exception-logger", () -> new ExceptionLoggingChannelHandler(input, log));
        handlerList.put("packet-meta-dumper", () -> new PacketInformationDumper(input));
        handlerList.put("traffic-counter", () -> throughputCounter);
        handlerList.put("output-failure-logger", () -> PromiseFailureHandler.INSTANCE);

        return handlerList;
    }

    /**
     * Subclasses can override this to modify the {@link org.jboss.netty.channel.ChannelHandler channel handlers} at the end of the pipeline.
     * <p/>
     * The default handlers in this group are the aggregation handler (e.g. for chunked GELF via UDP), which can be missing, and the {@link RawMessageHandler}.
     * <p/>
     * Usually this should not be necessary, only modify them if you have a codec that cannot create a {@link org.graylog2.plugin.journal.RawMessage} for
     * incoming messages at the end of the pipeline.
     * <p/>
     * One valid use case would be to insert debug handlers in the middle of the list, though.
     *
     * @param input
     * @return the list of channel handlers at the end of the pipeline
     */
    protected LinkedHashMap<String, Callable<? extends ChannelHandler>> getFinalChannelHandlers(final MessageInput input) {
        LinkedHashMap<String, Callable<? extends ChannelHandler>> handlerList = new LinkedHashMap<>();

        if (aggregator != null) {
            log.debug("Adding codec aggregator {} to channel pipeline", aggregator);
            handlerList.put("codec-aggregator", () -> new MessageAggregationHandler(aggregator, localRegistry));
        }

        handlerList.put("rawmessage-handler", () -> new RawMessageHandler(input));
        return handlerList;
    }

    @Override
    public void stop() {
        if (channels != null) {
            channels.close().syncUninterruptibly();
        }
    }

    protected int getRecvBufferSize() {
        return recvBufferSize;
    }

    @Override
    public MetricSet getMetricSet() {
        return localRegistry;
    }

    public static class Config implements Transport.Config {
        @Override
        public ConfigurationRequest getRequestedConfiguration() {
            final ConfigurationRequest r = new ConfigurationRequest();

            r.addField(ConfigurationRequest.Templates.bindAddress(CK_BIND_ADDRESS));
            r.addField(ConfigurationRequest.Templates.portNumber(CK_PORT, 5555));
            r.addField(ConfigurationRequest.Templates.recvBufferSize(CK_RECV_BUFFER_SIZE, 1024 * 1024));

            return r;
        }
    }
}

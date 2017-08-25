/*
 * Copyright 2017 Graylog Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.graylog.plugins.netflow.transport;

import com.codahale.metrics.Meter;
import com.codahale.metrics.Timer;
import com.google.inject.assistedinject.Assisted;
import org.graylog.plugins.netflow.codecs.NetflowV9CodecAggregator;
import org.graylog2.inputs.transports.UdpTransport;
import org.graylog2.plugin.LocalMetricRegistry;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.plugin.inputs.annotations.ConfigClass;
import org.graylog2.plugin.inputs.annotations.FactoryClass;
import org.graylog2.plugin.inputs.codecs.CodecAggregator;
import org.graylog2.plugin.inputs.transports.Transport;
import org.graylog2.plugin.inputs.util.ThroughputCounter;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.ChannelHandler;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.net.SocketAddress;
import java.util.LinkedHashMap;
import java.util.concurrent.Callable;

import static org.jboss.netty.channel.Channels.fireMessageReceived;

/**
 * This UDP transport is largely identical to its superclass, but replaces the codec aggregator and its handler with custom
 * implementations that are able to pass the remote address.
 * <br/>
 * Without the remote address the NetFlow V9 code cannot distinguish between flows from different exporters and thus might
 * handle template flows incorrectly should they differ between exporters. See https://tools.ietf.org/html/rfc3954#section-5.1 "Source ID".
 */
public class NetFlowUdpTransport extends UdpTransport {
    private static final Logger LOG = LoggerFactory.getLogger(NetFlowUdpTransport.class);

    private final NetflowV9CodecAggregator netflowV9CodecAggregator;

    @Inject
    public NetFlowUdpTransport(@Assisted Configuration configuration,
                               ThroughputCounter throughputCounter,
                               LocalMetricRegistry localRegistry,
                               NetflowV9CodecAggregator netflowV9CodecAggregator) {
        super(configuration, throughputCounter, localRegistry);
        this.netflowV9CodecAggregator = netflowV9CodecAggregator;
    }

    @Override
    protected LinkedHashMap<String, Callable<? extends ChannelHandler>> getFinalChannelHandlers(MessageInput input) {
        final LinkedHashMap<String, Callable<? extends ChannelHandler>> finalChannelHandlers = super.getFinalChannelHandlers(input);
        final LinkedHashMap<String, Callable<? extends ChannelHandler>> handlers = new LinkedHashMap<>();
        // replace the codec-aggregator handler with one that passes the remote address
        handlers.put("codec-aggregator", () -> new NetflowMessageAggregationHandler(netflowV9CodecAggregator));
        handlers.putAll(finalChannelHandlers);
        return handlers;
    }

    @FactoryClass
    public interface Factory extends Transport.Factory<NetFlowUdpTransport> {
        @Override
        NetFlowUdpTransport create(Configuration configuration);

        @Override
        UdpTransport.Config getConfig();
    }

    @ConfigClass
    public static class Config extends UdpTransport.Config {
    }

    private class NetflowMessageAggregationHandler extends SimpleChannelHandler {
        private final NetflowV9CodecAggregator aggregator;
        private final Timer aggregationTimer;
        private final Meter invalidChunksMeter;

        public NetflowMessageAggregationHandler(NetflowV9CodecAggregator aggregator) {
            this.aggregator = aggregator;
            aggregationTimer = localRegistry.timer("aggregationTime");
            invalidChunksMeter = localRegistry.meter("invalidMessages");
        }

        @Override
        public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
            final Object message = e.getMessage();

            final SocketAddress remoteAddress = e.getRemoteAddress();
            if (message instanceof ChannelBuffer) {
                final ChannelBuffer buf = (ChannelBuffer) message;
                final CodecAggregator.Result result;
                try (Timer.Context ignored = aggregationTimer.time()) {
                    result = aggregator.addChunk(buf, remoteAddress);
                }
                final ChannelBuffer completeMessage = result.getMessage();
                if (completeMessage != null) {
                    LOG.debug("Message aggregation completion, forwarding {}", completeMessage);
                    fireMessageReceived(ctx, completeMessage, remoteAddress);
                } else if (result.isValid()) {
                    LOG.debug("More chunks necessary to complete this message");
                } else {
                    invalidChunksMeter.mark();
                    LOG.debug("Message chunk was not valid and discarded.");
                }
            } else {
                LOG.debug("Could not handle netty message {}, sending further upstream.", e);
                fireMessageReceived(ctx, message, remoteAddress);
            }
        }
    }

}

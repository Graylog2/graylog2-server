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
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;
import org.graylog.plugins.netflow.codecs.RemoteAddressCodecAggregator;
import org.graylog2.plugin.inputs.codecs.CodecAggregator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

public class NetflowMessageAggregationHandler extends SimpleChannelInboundHandler<DatagramPacket> {
    private static final Logger LOG = LoggerFactory.getLogger(NetflowMessageAggregationHandler.class);

    private final RemoteAddressCodecAggregator aggregator;
    private final Timer aggregationTimer;
    private final Meter invalidChunksMeter;

    public NetflowMessageAggregationHandler(RemoteAddressCodecAggregator aggregator, MetricRegistry metricRegistry) {
        this.aggregator = aggregator;
        aggregationTimer = metricRegistry.timer("aggregationTime");
        invalidChunksMeter = metricRegistry.meter("invalidMessages");
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, DatagramPacket msg) throws Exception {
        final SocketAddress remoteAddress = msg.sender();
        final CodecAggregator.Result result;
        try (Timer.Context ignored = aggregationTimer.time()) {
            result = aggregator.addChunk(msg.content(), remoteAddress);
        }
        final ByteBuf completeMessage = result.getMessage();
        if (completeMessage != null) {
            LOG.debug("Message aggregation completion, forwarding {}", completeMessage);
            ctx.fireChannelRead(completeMessage);
        } else if (result.isValid()) {
            LOG.debug("More chunks necessary to complete this message");
        } else {
            invalidChunksMeter.mark();
            LOG.debug("Message chunk was not valid and discarded.");
        }
    }
}

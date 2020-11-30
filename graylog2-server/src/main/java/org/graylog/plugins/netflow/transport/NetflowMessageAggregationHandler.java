/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
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
import org.graylog2.inputs.transports.netty.SenderEnvelope;
import org.graylog2.plugin.inputs.codecs.CodecAggregator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;

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
        final InetSocketAddress remoteAddress = msg.sender();
        final CodecAggregator.Result result;
        try (Timer.Context ignored = aggregationTimer.time()) {
            result = aggregator.addChunk(msg.content(), remoteAddress);
        }
        final ByteBuf completeMessage = result.getMessage();
        if (completeMessage != null) {
            LOG.debug("Message aggregation completion, forwarding {}", completeMessage);
            ctx.fireChannelRead(SenderEnvelope.of(completeMessage, remoteAddress));
        } else if (result.isValid()) {
            LOG.debug("More chunks necessary to complete this message");
        } else {
            invalidChunksMeter.mark();
            LOG.debug("Message chunk was not valid and discarded.");
        }
    }
}

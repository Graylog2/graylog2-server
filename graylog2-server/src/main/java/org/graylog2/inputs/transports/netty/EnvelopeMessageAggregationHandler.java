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
package org.graylog2.inputs.transports.netty;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import io.netty.buffer.ByteBuf;
import io.netty.channel.AddressedEnvelope;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.graylog2.plugin.inputs.codecs.CodecAggregator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;

public class EnvelopeMessageAggregationHandler extends SimpleChannelInboundHandler<AddressedEnvelope<ByteBuf, InetSocketAddress>> {
    private static final Logger LOG = LoggerFactory.getLogger(EnvelopeMessageAggregationHandler.class);

    private final CodecAggregator aggregator;
    private final Timer aggregationTimer;
    private final Meter invalidChunksMeter;

    public EnvelopeMessageAggregationHandler(CodecAggregator aggregator, MetricRegistry metricRegistry) {
        this.aggregator = aggregator;
        aggregationTimer = metricRegistry.timer("aggregationTime");
        invalidChunksMeter = metricRegistry.meter("invalidMessages");
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, AddressedEnvelope<ByteBuf, InetSocketAddress> envelope) throws Exception {
        final CodecAggregator.Result result;
        try (Timer.Context ignored = aggregationTimer.time()) {
            result = aggregator.addChunk(envelope.content());
        }
        final ByteBuf completeMessage = result.getMessage();
        if (completeMessage != null) {
            LOG.debug("Message aggregation completion, forwarding {}", completeMessage);
            ctx.fireChannelRead(SenderEnvelope.of(completeMessage, envelope.sender()));
        } else if (result.isValid()) {
            LOG.debug("More chunks necessary to complete this message");
        } else {
            invalidChunksMeter.mark();
            LOG.debug("Message chunk was not valid and discarded.");
        }
    }
}
/**
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
package org.graylog2.inputs.gelf.tcp;

import com.codahale.metrics.MetricRegistry;
import org.graylog2.inputs.gelf.GELFDispatcher;
import org.graylog2.inputs.gelf.gelf.GELFChunkManager;
import org.graylog2.inputs.network.PacketInformationDumper;
import org.graylog2.plugin.buffers.Buffer;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.plugin.inputs.util.ConnectionCounter;
import org.graylog2.plugin.inputs.util.ThroughputCounter;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.handler.codec.frame.DelimiterBasedFrameDecoder;
import org.jboss.netty.handler.codec.frame.Delimiters;

/**
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class GELFTCPPipelineFactory implements ChannelPipelineFactory {

    private final MetricRegistry metricRegistry;
    private final Buffer processBuffer;
    private final GELFChunkManager gelfChunkManager;
    private final MessageInput sourceInput;
    private final ThroughputCounter throughputCounter;
    private final ConnectionCounter connectionCounter;

    public GELFTCPPipelineFactory(MetricRegistry metricRegistry,
                                  Buffer processBuffer,
                                  GELFChunkManager gelfChunkManager,
                                  MessageInput sourceInput,
                                  ThroughputCounter throughputCounter,
                                  ConnectionCounter connectionCounter) {
        this.metricRegistry = metricRegistry;
        this.processBuffer = processBuffer;
        this.gelfChunkManager = gelfChunkManager;
        this.sourceInput = sourceInput;
        this.throughputCounter = throughputCounter;
        this.connectionCounter = connectionCounter;
    }

    @Override
    public ChannelPipeline getPipeline() throws Exception {
        ChannelPipeline p = Channels.pipeline();

        p.addLast("connection-counter", connectionCounter);
        p.addLast("packet-meta-dumper", new PacketInformationDumper(sourceInput));
        p.addLast("framer", new DelimiterBasedFrameDecoder(2 * 1024 * 1024, Delimiters.nulDelimiter()));
        p.addLast("traffic-counter", throughputCounter);
        p.addLast("handler", new GELFDispatcher(metricRegistry, gelfChunkManager, processBuffer, sourceInput));

        return p;
    }

}

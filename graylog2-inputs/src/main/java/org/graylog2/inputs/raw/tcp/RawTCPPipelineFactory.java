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
package org.graylog2.inputs.raw.tcp;

import org.graylog2.inputs.network.PacketInformationDumper;
import org.graylog2.inputs.raw.RawDispatcher;
import org.graylog2.plugin.InputHost;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.plugin.inputs.util.ConnectionCounter;
import org.graylog2.plugin.inputs.util.ThroughputCounter;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.handler.codec.frame.DelimiterBasedFrameDecoder;
import org.jboss.netty.handler.codec.frame.Delimiters;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class RawTCPPipelineFactory implements ChannelPipelineFactory {

    private final InputHost server;
    private final Configuration config;
    private final MessageInput sourceInput;
    private final ThroughputCounter throughputCounter;
    private final ConnectionCounter connectionCounter;

    public RawTCPPipelineFactory(InputHost server, Configuration config, MessageInput sourceInput, ThroughputCounter throughputCounter, ConnectionCounter connectionCounter) {
        this.server = server;
        this.config = config;
        this.sourceInput = sourceInput;
        this.throughputCounter = throughputCounter;
        this.connectionCounter = connectionCounter;
    }

    @Override
    public ChannelPipeline getPipeline() throws Exception {
        ChannelBuffer[] delimiter;

        if (config.getBoolean(RawTCPInput.CK_USE_NULL_DELIMITER)) {
            delimiter = Delimiters.nulDelimiter();
        } else {
            delimiter = Delimiters.lineDelimiter();
        }

        ChannelPipeline p = Channels.pipeline();
        p.addLast("packet-meta-dumper", new PacketInformationDumper(sourceInput));
        p.addLast("connection-counter", connectionCounter);
        p.addLast("traffic-counter", throughputCounter);
        p.addLast("framer", new DelimiterBasedFrameDecoder(2 * 1024 * 1024, delimiter));
        p.addLast("handler", new RawDispatcher(server, config, sourceInput));

        return p;
    }

}

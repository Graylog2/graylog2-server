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
package org.graylog2.inputs.raw.udp;

import org.graylog2.inputs.raw.RawDispatcher;
import org.graylog2.plugin.GraylogServer;
import org.graylog2.plugin.InputHost;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.plugin.inputs.util.ThroughputCounter;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class RawUDPPipelineFactory implements ChannelPipelineFactory {

    private final InputHost server;
    private final Configuration config;
    private final MessageInput sourceInput;
    private final ThroughputCounter throughputCounter;

    public RawUDPPipelineFactory(InputHost server, Configuration config, MessageInput sourceInput, ThroughputCounter throughputCounter) {
        this.server = server;
        this.config = config;
        this.sourceInput = sourceInput;
        this.throughputCounter = throughputCounter;
    }

    @Override
    public ChannelPipeline getPipeline() throws Exception {
        ChannelPipeline p = Channels.pipeline();
        p.addLast("traffic-counter", throughputCounter);
        p.addLast("handler", new RawDispatcher(server, config, sourceInput));

        return p;
    }

}

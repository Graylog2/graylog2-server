/**
 * Copyright 2012 Lennart Koopmann <lennart@socketfeed.com>
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

package org.graylog2.inputs.gelf.tcp;

import org.graylog2.Core;
import org.graylog2.inputs.ThroughputCounter;
import org.graylog2.inputs.gelf.GELFDispatcher;
import org.graylog2.plugin.inputs.MessageInput;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.handler.codec.frame.DelimiterBasedFrameDecoder;
import org.jboss.netty.handler.codec.frame.Delimiters;

/**
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class GELFTCPPipelineFactory implements ChannelPipelineFactory {

    private final Core server;
    private final MessageInput sourceInput;
    private final ThroughputCounter throughputCounter;

    public GELFTCPPipelineFactory(Core server, MessageInput sourceInput, ThroughputCounter throughputCounter) {
        this.server = server;
        this.sourceInput = sourceInput;
        this.throughputCounter = throughputCounter;
    }

    @Override
    public ChannelPipeline getPipeline() throws Exception {
        ChannelPipeline p = Channels.pipeline();
        p.addLast("framer", new DelimiterBasedFrameDecoder(2 * 1024 * 1024, Delimiters.nulDelimiter()));
        p.addLast("traffic-counter", throughputCounter);
        p.addLast("handler", new GELFDispatcher(server, sourceInput));
        return p;
    }

}

/**
 * Copyright 2012 Kay Roepke <kroepke@googlemail.com>
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
package org.graylog2.inputs.gelf.http;

import org.graylog2.plugin.GraylogServer;
import org.graylog2.plugin.InputHost;
import org.graylog2.plugin.inputs.util.ConnectionCounter;
import org.graylog2.plugin.inputs.util.ThroughputCounter;
import org.graylog2.plugin.inputs.MessageInput;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.handler.codec.http.HttpContentDecompressor;
import org.jboss.netty.handler.codec.http.HttpRequestDecoder;
import org.jboss.netty.handler.codec.http.HttpResponseEncoder;

public class GELFHttpPipelineFactory implements ChannelPipelineFactory {

    private final InputHost graylogServer;
    private final MessageInput sourceInput;
    private final ThroughputCounter throughputCounter;
    private final ConnectionCounter connectionCounter;

    public GELFHttpPipelineFactory(InputHost graylogServer, MessageInput sourceInput, ThroughputCounter throughputCounter, ConnectionCounter connectionCounter) {
        this.graylogServer = graylogServer;
        this.sourceInput = sourceInput;
        this.throughputCounter = throughputCounter;
        this.connectionCounter = connectionCounter;
    }

    @Override
    public ChannelPipeline getPipeline() throws Exception {
        final ChannelPipeline pipeline = Channels.pipeline();

        pipeline.addLast("connection-counter", connectionCounter);
        pipeline.addLast("traffic-counter", throughputCounter);
        pipeline.addLast("decoder", new HttpRequestDecoder());
        pipeline.addLast("encoder", new HttpResponseEncoder());

        // only add support for incoming compressed messages, we don't return much (if any) data to the client.
        pipeline.addLast("decompressor", new HttpContentDecompressor());

        pipeline.addLast("handler", new GELFHttpHandler(graylogServer, sourceInput));

        return pipeline;
    }
}

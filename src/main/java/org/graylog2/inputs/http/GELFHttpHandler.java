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
package org.graylog2.inputs.http;

import com.yammer.metrics.Metrics;
import com.yammer.metrics.core.Meter;
import org.apache.log4j.Logger;
import org.graylog2.Core;
import org.graylog2.gelf.GELFMessage;
import org.graylog2.gelf.GELFProcessor;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.*;
import org.jboss.netty.handler.codec.http.*;

import java.util.concurrent.TimeUnit;

import static org.jboss.netty.handler.codec.http.HttpHeaders.isKeepAlive;

public class GELFHttpHandler extends SimpleChannelHandler {

    private static final Logger LOG = Logger.getLogger(GELFHttpHandler.class);

    private final Core server;
    private final Meter receivedMessages = Metrics.newMeter(GELFHttpHandler.class, "ReceivedMessages", "messages", TimeUnit.SECONDS);
    private final Meter gelfMessages = Metrics.newMeter(GELFHttpHandler.class, "ReceivedGelfMessages", "messages", TimeUnit.SECONDS);
    private final Meter rawMessages = Metrics.newMeter(GELFHttpHandler.class, "ReceivedRawMessages", "messages", TimeUnit.SECONDS);
    private final GELFProcessor gelfProcessor;

    public GELFHttpHandler(Core server) {
        this.server = server;
        gelfProcessor = new GELFProcessor(server);
    }

    @Override
    public void messageReceived(final ChannelHandlerContext ctx, final MessageEvent e) throws Exception {
        receivedMessages.mark();

        final HttpRequest request = (HttpRequest) e.getMessage();
        final boolean keepAlive = isKeepAlive(request);
        final HttpVersion httpRequestVersion = request.getProtocolVersion();

        // to allow for future changes, let's be at least a little strict in what we accept here.
        if (request.getMethod() != HttpMethod.PUT) {
            writeResponse(e.getChannel(), keepAlive, httpRequestVersion, HttpResponseStatus.METHOD_NOT_ALLOWED);
        }

        // there are two variants to get data in via HTTP:
        // 1. just send a common GELF message, including the header bytes (see GELFMessage.Type)
        // 2. only sending the uncompressed "raw" message, i.e. only the actual JSON
        //    currently GELFMessage does not support "RAW", so we jump through some hoops to avoid System.arrayCopy
        final ChannelBuffer buffer = request.getContent();
        final byte[] message = new byte[buffer.readableBytes()];
        buffer.toByteBuffer().get(message, buffer.readerIndex(), buffer.readableBytes());

        final GELFMessage msg;
        if ("/gelf/raw".equals(request.getUri())) {
            rawMessages.mark();
            msg = new GELFMessage(message, true);
        } else if ("/gelf".equals(request.getUri())) {
            gelfMessages.mark();
            msg = new GELFMessage(message);
        } else {
            writeResponse(e.getChannel(), keepAlive, httpRequestVersion, HttpResponseStatus.NOT_FOUND);
            return;
        }

        gelfProcessor.messageReceived(msg);
        writeResponse(e.getChannel(), keepAlive, httpRequestVersion, HttpResponseStatus.ACCEPTED);
    }

    private void writeResponse(Channel channel, boolean keepAlive, HttpVersion httpRequestVersion, HttpResponseStatus status) {
        final HttpResponse response =
            new DefaultHttpResponse(httpRequestVersion, status);

        if (keepAlive) {
            response.setHeader(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
        }

        final ChannelFuture channelFuture = channel.write(response);
        if (!keepAlive) {
            channelFuture.addListener(ChannelFutureListener.CLOSE);
        }
    }

    @Override
    public void exceptionCaught(final ChannelHandlerContext ctx, final ExceptionEvent e) throws Exception {
        LOG.warn("Could not handle GELF message.", e.getCause());
    }
}

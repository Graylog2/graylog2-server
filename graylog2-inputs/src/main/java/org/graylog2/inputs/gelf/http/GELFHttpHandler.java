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

import com.codahale.metrics.Meter;
import org.graylog2.inputs.gelf.gelf.GELFMessage;
import org.graylog2.inputs.gelf.gelf.GELFProcessor;
import org.graylog2.plugin.InputHost;
import org.graylog2.plugin.inputs.MessageInput;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.*;
import org.jboss.netty.handler.codec.http.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.codahale.metrics.MetricRegistry.name;
import static org.jboss.netty.handler.codec.http.HttpHeaders.isKeepAlive;

public class GELFHttpHandler extends SimpleChannelHandler {

    private static final Logger LOG = LoggerFactory.getLogger(GELFHttpHandler.class);

    private final Meter receivedMessages;
    private final Meter gelfMessages;

    private final MessageInput sourceInput;

    private final GELFProcessor gelfProcessor;

    public GELFHttpHandler(InputHost server, MessageInput sourceInput) {
        this.gelfProcessor = new GELFProcessor(server);

        this.sourceInput = sourceInput;

        this.receivedMessages = server.metrics().meter(name(GELFHttpHandler.class, "receivedMessages"));
        this.gelfMessages = server.metrics().meter(name(GELFHttpHandler.class, "gelfMessages"));
    }

    @Override
    public void messageReceived(final ChannelHandlerContext ctx, final MessageEvent e) throws Exception {
        receivedMessages.mark();

        final HttpRequest request = (HttpRequest) e.getMessage();
        final boolean keepAlive = isKeepAlive(request);
        final HttpVersion httpRequestVersion = request.getProtocolVersion();

        // to allow for future changes, let's be at least a little strict in what we accept here.
        if (request.getMethod() != HttpMethod.POST) {
            writeResponse(e.getChannel(), keepAlive, httpRequestVersion, HttpResponseStatus.METHOD_NOT_ALLOWED);
        }

        final ChannelBuffer buffer = request.getContent();
        final byte[] message = new byte[buffer.readableBytes()];
        buffer.toByteBuffer().get(message, buffer.readerIndex(), buffer.readableBytes());

        final GELFMessage msg;
        if ("/gelf".equals(request.getUri())) {
            gelfMessages.mark();
            msg = new GELFMessage(message);
        } else {
            writeResponse(e.getChannel(), keepAlive, httpRequestVersion, HttpResponseStatus.NOT_FOUND);
            return;
        }

        gelfProcessor.messageReceived(msg, sourceInput);
        writeResponse(e.getChannel(), keepAlive, httpRequestVersion, HttpResponseStatus.ACCEPTED);
    }

    private void writeResponse(Channel channel, boolean keepAlive, HttpVersion httpRequestVersion, HttpResponseStatus status) {
        final HttpResponse response =
            new DefaultHttpResponse(httpRequestVersion, status);

        response.headers().set(HttpHeaders.Names.CONTENT_LENGTH, 0);
        response.headers().set(HttpHeaders.Names.CONNECTION,
                               keepAlive ? HttpHeaders.Values.KEEP_ALIVE : HttpHeaders.Values.CLOSE);

        final ChannelFuture channelFuture = channel.write(response);
        if (!keepAlive) {
            channelFuture.addListener(ChannelFutureListener.CLOSE);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
        LOG.debug("Could not handle GELF HTTP message.", e.getCause());

        if (ctx.getChannel() != null) {
            ctx.getChannel().close();
        }
    }
}

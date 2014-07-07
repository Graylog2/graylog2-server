/*
 * Copyright 2012-2014 TORCH GmbH
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
 */
package org.graylog2.inputs.gelf.http;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import org.graylog2.inputs.gelf.gelf.GELFMessage;
import org.graylog2.inputs.gelf.gelf.GELFProcessor;
import org.graylog2.plugin.buffers.Buffer;
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
    private final Boolean enableCors;

    private final GELFProcessor gelfProcessor;

    public GELFHttpHandler(MetricRegistry metricRegistry,
                           MessageInput sourceInput,
                           GELFProcessor gelfProcessor,
                           Boolean enableCors) {
        this.gelfProcessor = gelfProcessor;
        this.sourceInput = sourceInput;
        this.enableCors = enableCors;

        this.receivedMessages = metricRegistry.meter(name(GELFHttpHandler.class, "receivedMessages"));
        this.gelfMessages = metricRegistry.meter(name(GELFHttpHandler.class, "gelfMessages"));
    }

    @Override
    public void messageReceived(final ChannelHandlerContext ctx, final MessageEvent e) throws Exception {
        receivedMessages.mark();

        final HttpRequest request = (HttpRequest) e.getMessage();
        final boolean keepAlive = isKeepAlive(request);
        final HttpVersion httpRequestVersion = request.getProtocolVersion();
        String origin = request.headers().get(HttpHeaders.Names.ORIGIN);

        // to allow for future changes, let's be at least a little strict in what we accept here.
        if (request.getMethod() != HttpMethod.POST) {
            writeResponse(e.getChannel(), keepAlive, httpRequestVersion, HttpResponseStatus.METHOD_NOT_ALLOWED, origin);
            return;
        }

        final ChannelBuffer buffer = request.getContent();
        final byte[] message = new byte[buffer.readableBytes()];
        buffer.toByteBuffer().get(message, buffer.readerIndex(), buffer.readableBytes());

        final GELFMessage msg;
        if ("/gelf".equals(request.getUri())) {
            gelfMessages.mark();
            msg = new GELFMessage(message);
        } else {
            writeResponse(e.getChannel(), keepAlive, httpRequestVersion, HttpResponseStatus.NOT_FOUND, origin);
            return;
        }

        gelfProcessor.messageReceived(msg, sourceInput);
        writeResponse(e.getChannel(), keepAlive, httpRequestVersion, HttpResponseStatus.ACCEPTED, origin);
    }

    private void writeResponse(Channel channel, boolean keepAlive, HttpVersion httpRequestVersion, HttpResponseStatus status, String origin) {
        final HttpResponse response =
            new DefaultHttpResponse(httpRequestVersion, status);

        response.headers().set(HttpHeaders.Names.CONTENT_LENGTH, 0);
        response.headers().set(HttpHeaders.Names.CONNECTION,
                               keepAlive ? HttpHeaders.Values.KEEP_ALIVE : HttpHeaders.Values.CLOSE);

        if (enableCors) {
            if (origin != null && !origin.isEmpty()) {
                response.headers().set(HttpHeaders.Names.ACCESS_CONTROL_ALLOW_ORIGIN, origin);
                response.headers().set(HttpHeaders.Names.ACCESS_CONTROL_ALLOW_CREDENTIALS, true);
                response.headers().set(HttpHeaders.Names.ACCESS_CONTROL_ALLOW_HEADERS, "Authorization");
            }
        }

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

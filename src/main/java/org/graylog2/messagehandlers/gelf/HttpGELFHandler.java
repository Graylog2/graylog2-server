/**
 * Copyright 2012 Kay Roepke <kroepke@classdump.org>
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

package org.graylog2.messagehandlers.gelf;

import org.apache.log4j.Logger;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.handler.codec.http.DefaultHttpResponse;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.jboss.netty.handler.codec.http.HttpVersion;
import org.jboss.netty.util.CharsetUtil;

public class HttpGELFHandler extends SimpleChannelUpstreamHandler {
    private static final Logger LOG = Logger.getLogger(HttpGELFHandler.class);
    
    @Override
    public void messageReceived(final ChannelHandlerContext ctx, final MessageEvent event)
            throws Exception {
        final HttpRequest request = (HttpRequest) event.getMessage();
        final ChannelBuffer content = request.getContent();
        final String receivedGelfSentence = content.toString(CharsetUtil.UTF_8);
        LOG.info("received http message: " + receivedGelfSentence);
        new SimpleGELFClientHandler(receivedGelfSentence).handle();

        // Build the response object.
        final HttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.ACCEPTED);
        response.setContent(ChannelBuffers.copiedBuffer("", CharsetUtil.UTF_8));
        response.setHeader(HttpHeaders.Names.CONTENT_TYPE, "text/plain; charset=UTF-8");
        
        final ChannelFuture future = event.getChannel().write(response);
        future.addListener(ChannelFutureListener.CLOSE);
    }

    @Override
    public void exceptionCaught(final ChannelHandlerContext ctx, final ExceptionEvent e)
            throws Exception {
        LOG.error("Exception caught while reading HTTP GELF message", e.getCause());
        e.getChannel().close();
    }

}

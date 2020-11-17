/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
package org.graylog2.inputs.transports.netty;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.HttpVersion;

public class HttpHandler extends SimpleChannelInboundHandler<HttpRequest> {
    private final boolean enableCors;

    public HttpHandler(boolean enableCors) {
        this.enableCors = enableCors;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, HttpRequest request) throws Exception {
        final Channel channel = ctx.channel();
        final boolean keepAlive = HttpUtil.isKeepAlive(request);
        final HttpVersion httpRequestVersion = request.protocolVersion();
        final String origin = request.headers().get(HttpHeaderNames.ORIGIN);

        // to allow for future changes, let's be at least a little strict in what we accept here.
        if (HttpMethod.OPTIONS.equals(request.method())) {
            writeResponse(channel, keepAlive, httpRequestVersion, HttpResponseStatus.OK, origin);
            return;
        } else if (!HttpMethod.POST.equals(request.method())) {
            writeResponse(channel, keepAlive, httpRequestVersion, HttpResponseStatus.METHOD_NOT_ALLOWED, origin);
            return;
        }

        final boolean correctPath = "/gelf".equals(request.uri());
        if (correctPath && request instanceof FullHttpRequest) {
            final FullHttpRequest fullHttpRequest = (FullHttpRequest) request;
            final ByteBuf buffer = fullHttpRequest.content();

            // send on to raw message handler
            writeResponse(channel, keepAlive, httpRequestVersion, HttpResponseStatus.ACCEPTED, origin);
            ctx.fireChannelRead(buffer.retain());
        } else {
            writeResponse(channel, keepAlive, httpRequestVersion, HttpResponseStatus.NOT_FOUND, origin);
        }
    }

    private void writeResponse(Channel channel,
                               boolean keepAlive,
                               HttpVersion httpRequestVersion,
                               HttpResponseStatus status,
                               String origin) {
        final HttpResponse response = new DefaultFullHttpResponse(httpRequestVersion, status);

        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, 0);
        response.headers().set(HttpHeaderNames.CONNECTION, keepAlive ? HttpHeaderValues.KEEP_ALIVE : HttpHeaderValues.CLOSE);

        if (enableCors && origin != null && !origin.isEmpty()) {
            response.headers().set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN, origin);
            response.headers().set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_CREDENTIALS, true);
            response.headers().set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_HEADERS, "Authorization, Content-Type");
        }

        final ChannelFuture channelFuture = channel.writeAndFlush(response);

        channelFuture.addListener(keepAlive ? ChannelFutureListener.CLOSE_ON_FAILURE : ChannelFutureListener.CLOSE);
    }
}

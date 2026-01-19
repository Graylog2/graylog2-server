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
package org.graylog.plugins.sidecar.opamp.server;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.codec.http.websocketx.extensions.compression.WebSocketServerCompressionHandler;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
public class OpAMPChannelInitializer extends ChannelInitializer<SocketChannel> {
    private static final String WEBSOCKET_PATH = "/v1/opamp";
    private static final int MAX_FRAME_SIZE = 65536;
    private static final int MAX_HTTP_CONTENT_LENGTH = 65536;

    private final OpAMPAuthHandler authHandler;
    private final OpAMPFrameHandler frameHandler;

    @Inject
    public OpAMPChannelInitializer(OpAMPAuthHandler authHandler, OpAMPFrameHandler frameHandler) {
        this.authHandler = authHandler;
        this.frameHandler = frameHandler;
    }

    @Override
    protected void initChannel(SocketChannel ch) {
        final ChannelPipeline pipeline = ch.pipeline();

        // HTTP codec for WebSocket upgrade handshake
        pipeline.addLast("http-codec", new HttpServerCodec());
        pipeline.addLast("http-aggregator", new HttpObjectAggregator(MAX_HTTP_CONTENT_LENGTH));

        // Authentication - validates token before WebSocket upgrade
        pipeline.addLast("auth", authHandler);

        // WebSocket compression (optional, but recommended)
        pipeline.addLast("ws-compression", new WebSocketServerCompressionHandler());

        // WebSocket protocol handler - handles upgrade, ping/pong, close frames
        pipeline.addLast("ws-protocol", new WebSocketServerProtocolHandler(
                WEBSOCKET_PATH,
                null,  // subprotocols
                true,  // allowExtensions
                MAX_FRAME_SIZE
        ));

        // Our custom handler for OpAMP binary frames
        pipeline.addLast("opamp-handler", frameHandler);
    }
}

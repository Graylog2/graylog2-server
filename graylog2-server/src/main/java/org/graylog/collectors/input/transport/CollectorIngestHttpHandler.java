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
package org.graylog.collectors.input.transport;

import com.google.protobuf.AbstractMessageLite;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.HttpVersion;
import io.opentelemetry.proto.collector.logs.v1.ExportLogsServiceRequest;
import org.graylog.collectors.input.CollectorJournalRecordFactory;
import org.graylog.inputs.otel.transport.OtlpHttpUtils;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.plugin.journal.RawMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.function.Function;

/**
 * HTTP handler for collector-managed agents. Validates agent identity from the
 * channel attribute set by {@link AgentCertChannelHandler} during TLS handshake.
 * <p>
 * This handler is independent of {@link org.graylog2.inputs.transports.netty.HttpHandler}
 * — it does not need auth, CORS, OPTIONS, or forwarded-for handling since the collector
 * input uses mTLS for authentication and is only accessed by collector agents.
 * <p>
 * OTLP protocol logic (parsing) is shared via {@link OtlpHttpUtils}.
 */
public class CollectorIngestHttpHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
    private static final Logger LOG = LoggerFactory.getLogger(CollectorIngestHttpHandler.class);
    private static final String LOGS_PATH = OtlpHttpUtils.LOGS_PATH;

    private final MessageInput input;

    public CollectorIngestHttpHandler(MessageInput input) {
        this.input = input;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) {
        final boolean keepAlive = HttpUtil.isKeepAlive(request);

        if (!HttpMethod.POST.equals(request.method())) {
            sendError(ctx, HttpResponseStatus.METHOD_NOT_ALLOWED, keepAlive);
            return;
        }

        if (!LOGS_PATH.equals(request.uri())) {
            sendError(ctx, HttpResponseStatus.NOT_FOUND, keepAlive);
            return;
        }

        final String instanceUid = ctx.channel().attr(AgentCertChannelHandler.AGENT_INSTANCE_UID).get();
        if (instanceUid == null) {
            LOG.warn("Rejecting request without agent identity (no valid client certificate)");
            sendError(ctx, HttpResponseStatus.UNAUTHORIZED, keepAlive);
            return;
        }

        final ExportLogsServiceRequest exportRequest;
        try {
            exportRequest = OtlpHttpUtils.parse(request);
        } catch (OtlpHttpUtils.UnsupportedContentTypeException e) {
            sendError(ctx, HttpResponseStatus.UNSUPPORTED_MEDIA_TYPE, keepAlive);
            return;
        } catch (Exception e) {
            LOG.debug("Failed to parse OTLP request", e);
            sendError(ctx, HttpResponseStatus.BAD_REQUEST, keepAlive);
            return;
        }

        final boolean isProtobuf = OtlpHttpUtils.isProtobuf(request);
        try {
            final Function<byte[], RawMessage> createRawMessage;
            if (ctx.channel().remoteAddress() instanceof InetSocketAddress address) {
                createRawMessage = bytes -> new RawMessage(bytes, address);
            } else {
                createRawMessage = RawMessage::new;
            }

            CollectorJournalRecordFactory.createFromRequest(exportRequest, instanceUid).stream()
                    .map(AbstractMessageLite::toByteArray)
                    .map(createRawMessage)
                    .forEach(input::processRawMessage);
        } catch (Exception e) {
            LOG.error("Failed to process OTLP request", e);
            sendError(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR, keepAlive);
            return;
        }

        sendSuccess(ctx, isProtobuf, keepAlive);
    }

    private void sendSuccess(ChannelHandlerContext ctx, boolean protobuf, boolean keepAlive) {
        final byte[] body = protobuf ? OtlpHttpUtils.SUCCESS_RESPONSE_PROTOBUF : OtlpHttpUtils.SUCCESS_RESPONSE_JSON;
        final String contentType = protobuf ? OtlpHttpUtils.PROTOBUF_CONTENT_TYPE : OtlpHttpUtils.JSON_CONTENT_TYPE;
        final DefaultFullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1, HttpResponseStatus.OK, Unpooled.wrappedBuffer(body));
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, contentType);
        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, body.length);
        response.headers().set(HttpHeaderNames.CONNECTION,
                keepAlive ? HttpHeaderValues.KEEP_ALIVE : HttpHeaderValues.CLOSE);
        ctx.writeAndFlush(response)
                .addListener(keepAlive ? ChannelFutureListener.CLOSE_ON_FAILURE : ChannelFutureListener.CLOSE);
    }

    private void sendError(ChannelHandlerContext ctx, HttpResponseStatus status, boolean keepAlive) {
        final byte[] body = OtlpHttpUtils.buildErrorStatusJson(status, null);
        final DefaultFullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1, status, Unpooled.wrappedBuffer(body));
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, OtlpHttpUtils.JSON_CONTENT_TYPE);
        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, body.length);
        response.headers().set(HttpHeaderNames.CONNECTION,
                keepAlive ? HttpHeaderValues.KEEP_ALIVE : HttpHeaderValues.CLOSE);
        ctx.writeAndFlush(response)
                .addListener(keepAlive ? ChannelFutureListener.CLOSE_ON_FAILURE : ChannelFutureListener.CLOSE);
    }
}

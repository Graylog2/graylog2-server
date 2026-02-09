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
package org.graylog.inputs.otel.transport;

import com.google.protobuf.AbstractMessageLite;
import com.google.protobuf.util.JsonFormat;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.opentelemetry.proto.collector.logs.v1.ExportLogsPartialSuccess;
import io.opentelemetry.proto.collector.logs.v1.ExportLogsServiceRequest;
import io.opentelemetry.proto.collector.logs.v1.ExportLogsServiceResponse;
import org.graylog.inputs.otel.OTelJournalRecordFactory;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.plugin.journal.RawMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.function.Function;

public class OTelHttpHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
    private static final Logger LOG = LoggerFactory.getLogger(OTelHttpHandler.class);

    static final String LOGS_PATH = "/v1/logs";
    static final String PROTOBUF_CONTENT_TYPE = "application/x-protobuf";
    static final String JSON_CONTENT_TYPE = "application/json";

    private final OTelJournalRecordFactory journalRecordFactory;
    private final MessageInput input;

    public OTelHttpHandler(OTelJournalRecordFactory journalRecordFactory, MessageInput input) {
        this.journalRecordFactory = journalRecordFactory;
        this.input = input;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) {
        // 1. Validate method
        if (!HttpMethod.POST.equals(request.method())) {
            sendError(ctx, HttpResponseStatus.METHOD_NOT_ALLOWED);
            return;
        }

        // 2. Validate path
        final String uri = request.uri();
        if (!LOGS_PATH.equals(uri) && !uri.startsWith(LOGS_PATH + "?")) {
            sendError(ctx, HttpResponseStatus.NOT_FOUND);
            return;
        }

        // 3. Determine content type
        final String contentType = request.headers().get(HttpHeaderNames.CONTENT_TYPE);
        final boolean isProtobuf = contentType != null && contentType.startsWith(PROTOBUF_CONTENT_TYPE);
        final boolean isJson = contentType != null && contentType.startsWith(JSON_CONTENT_TYPE);
        if (!isProtobuf && !isJson) {
            sendError(ctx, HttpResponseStatus.UNSUPPORTED_MEDIA_TYPE);
            return;
        }

        try {
            // 4. Parse request
            final ExportLogsServiceRequest exportRequest;
            if (isProtobuf) {
                final byte[] bytes = new byte[request.content().readableBytes()];
                request.content().readBytes(bytes);
                exportRequest = ExportLogsServiceRequest.parseFrom(bytes);
            } else {
                final String json = request.content().toString(StandardCharsets.UTF_8);
                final ExportLogsServiceRequest.Builder builder = ExportLogsServiceRequest.newBuilder();
                JsonFormat.parser().merge(json, builder);
                exportRequest = builder.build();
            }

            // 5. Create journal records and process
            final Function<byte[], RawMessage> createRawMessage;
            if (ctx.channel().remoteAddress() instanceof InetSocketAddress address) {
                createRawMessage = bytes -> new RawMessage(bytes, address);
            } else {
                createRawMessage = RawMessage::new;
            }

            journalRecordFactory.createFromRequest(exportRequest).stream()
                    .map(AbstractMessageLite::toByteArray)
                    .map(createRawMessage)
                    .forEach(input::processRawMessage);

            // 6. Send success response
            sendSuccess(ctx, isProtobuf);
        } catch (Exception e) {
            LOG.debug("Failed to parse OTLP request", e);
            sendError(ctx, HttpResponseStatus.BAD_REQUEST);
        }
    }

    private void sendSuccess(ChannelHandlerContext ctx, boolean protobuf) {
        final ExportLogsServiceResponse response = ExportLogsServiceResponse.newBuilder()
                .setPartialSuccess(ExportLogsPartialSuccess.newBuilder()
                        .setRejectedLogRecords(0)
                        .build())
                .build();

        final byte[] body;
        final String responseContentType;
        if (protobuf) {
            body = response.toByteArray();
            responseContentType = PROTOBUF_CONTENT_TYPE;
        } else {
            try {
                body = JsonFormat.printer().print(response).getBytes(StandardCharsets.UTF_8);
                responseContentType = JSON_CONTENT_TYPE;
            } catch (Exception e) {
                LOG.error("Failed to serialize OTLP JSON response", e);
                sendError(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR);
                return;
            }
        }

        final DefaultFullHttpResponse httpResponse = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1, HttpResponseStatus.OK,
                Unpooled.wrappedBuffer(body));
        httpResponse.headers().set(HttpHeaderNames.CONTENT_TYPE, responseContentType);
        httpResponse.headers().set(HttpHeaderNames.CONTENT_LENGTH, body.length);
        ctx.writeAndFlush(httpResponse).addListener(ChannelFutureListener.CLOSE);
    }

    private void sendError(ChannelHandlerContext ctx, HttpResponseStatus status) {
        final DefaultFullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1, status);
        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, 0);
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }
}

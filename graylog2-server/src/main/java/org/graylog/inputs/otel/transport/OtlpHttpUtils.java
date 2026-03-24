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

import com.google.protobuf.util.JsonFormat;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.opentelemetry.proto.collector.logs.v1.ExportLogsPartialSuccess;
import io.opentelemetry.proto.collector.logs.v1.ExportLogsServiceRequest;
import io.opentelemetry.proto.collector.logs.v1.ExportLogsServiceResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;

/**
 * Stateless utility for OTLP HTTP protocol concerns: request parsing and response formatting.
 * Used by both the generic OTel HTTP handler and the collector ingest handler.
 */
public final class OtlpHttpUtils {
    private static final Logger LOG = LoggerFactory.getLogger(OtlpHttpUtils.class);

    public static final String LOGS_PATH = "/v1/logs";
    public static final String PROTOBUF_CONTENT_TYPE = "application/x-protobuf";
    public static final String JSON_CONTENT_TYPE = "application/json";

    // Pre-computed success response bodies — the OTLP response with zero rejected records is invariant.
    public static final byte[] SUCCESS_RESPONSE_PROTOBUF = ExportLogsServiceResponse.newBuilder()
            .setPartialSuccess(ExportLogsPartialSuccess.newBuilder()
                    .setRejectedLogRecords(0)
                    .build())
            .build()
            .toByteArray();
    public static final byte[] SUCCESS_RESPONSE_JSON = computeJsonSuccessResponse();

    private OtlpHttpUtils() {}

    private static byte[] computeJsonSuccessResponse() {
        try {
            final ExportLogsServiceResponse response = ExportLogsServiceResponse.newBuilder()
                    .setPartialSuccess(ExportLogsPartialSuccess.newBuilder()
                            .setRejectedLogRecords(0)
                            .build())
                    .build();
            return JsonFormat.printer().print(response).getBytes(StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize OTLP JSON success response", e);
        }
    }

    /**
     * Returns true if the request's Content-Type indicates protobuf encoding.
     */
    public static boolean isProtobuf(FullHttpRequest request) {
        final String contentType = request.headers().get(HttpHeaderNames.CONTENT_TYPE);
        return contentType != null && contentType.startsWith(PROTOBUF_CONTENT_TYPE);
    }

    /**
     * Parses an OTLP ExportLogsServiceRequest from the HTTP request body.
     * Determines encoding (protobuf or JSON) from the Content-Type header.
     *
     * @throws UnsupportedContentTypeException if Content-Type is not protobuf or JSON
     * @throws com.google.protobuf.InvalidProtocolBufferException if the payload is malformed
     * @throws com.google.protobuf.util.JsonFormat.ParseException if JSON parsing fails
     */
    public static ExportLogsServiceRequest parse(FullHttpRequest request) throws Exception {
        final String contentType = request.headers().get(HttpHeaderNames.CONTENT_TYPE);
        final boolean isProtobuf = contentType != null && contentType.startsWith(PROTOBUF_CONTENT_TYPE);
        final boolean isJson = contentType != null && contentType.startsWith(JSON_CONTENT_TYPE);

        if (!isProtobuf && !isJson) {
            throw new UnsupportedContentTypeException(contentType);
        }

        if (isProtobuf) {
            final byte[] bytes = new byte[request.content().readableBytes()];
            request.content().readBytes(bytes);
            return ExportLogsServiceRequest.parseFrom(bytes);
        } else {
            final String json = request.content().toString(StandardCharsets.UTF_8);
            final ExportLogsServiceRequest.Builder builder = ExportLogsServiceRequest.newBuilder();
            JsonFormat.parser().merge(json, builder);
            return builder.build();
        }
    }

    /**
     * Sends a successful OTLP response with zero rejected records.
     * Used by handlers that don't inherit from {@link org.graylog2.inputs.transports.netty.HttpHandler}
     * (e.g., the collector ingest handler).
     */
    public static void sendSuccess(ChannelHandlerContext ctx, boolean protobuf, boolean keepAlive) {
        final byte[] body = protobuf ? SUCCESS_RESPONSE_PROTOBUF : SUCCESS_RESPONSE_JSON;
        final String contentType = protobuf ? PROTOBUF_CONTENT_TYPE : JSON_CONTENT_TYPE;
        final DefaultFullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1, HttpResponseStatus.OK, Unpooled.wrappedBuffer(body));
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, contentType);
        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, body.length);
        response.headers().set(HttpHeaderNames.CONNECTION,
                keepAlive ? HttpHeaderValues.KEEP_ALIVE : HttpHeaderValues.CLOSE);
        ctx.writeAndFlush(response)
                .addListener(keepAlive ? ChannelFutureListener.CLOSE_ON_FAILURE : ChannelFutureListener.CLOSE);
    }

    /**
     * Sends an error response with the given HTTP status and empty body.
     * Used by handlers that don't inherit from {@link org.graylog2.inputs.transports.netty.HttpHandler}
     * (e.g., the collector ingest handler).
     */
    public static void sendError(ChannelHandlerContext ctx, HttpResponseStatus status, boolean keepAlive) {
        final DefaultFullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1, status);
        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, 0);
        response.headers().set(HttpHeaderNames.CONNECTION,
                keepAlive ? HttpHeaderValues.KEEP_ALIVE : HttpHeaderValues.CLOSE);
        ctx.writeAndFlush(response)
                .addListener(keepAlive ? ChannelFutureListener.CLOSE_ON_FAILURE : ChannelFutureListener.CLOSE);
    }

    /**
     * Thrown when the request Content-Type is neither protobuf nor JSON.
     */
    public static class UnsupportedContentTypeException extends Exception {
        public UnsupportedContentTypeException(String contentType) {
            super("Unsupported content type: " + contentType);
        }
    }
}

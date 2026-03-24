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
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.opentelemetry.proto.collector.logs.v1.ExportLogsPartialSuccess;
import io.opentelemetry.proto.collector.logs.v1.ExportLogsServiceRequest;
import io.opentelemetry.proto.collector.logs.v1.ExportLogsServiceResponse;

import java.nio.charset.StandardCharsets;

/**
 * Stateless utility for OTLP HTTP protocol concerns: request parsing, content-type
 * detection, and pre-computed success response bodies.
 */
public final class OtlpHttpUtils {

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
     * Thrown when the request Content-Type is neither protobuf nor JSON.
     */
    public static class UnsupportedContentTypeException extends Exception {
        public UnsupportedContentTypeException(String contentType) {
            super("Unsupported content type: " + contentType);
        }
    }
}

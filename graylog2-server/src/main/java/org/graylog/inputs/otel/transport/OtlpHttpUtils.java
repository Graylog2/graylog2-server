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
import io.grpc.Status;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponseStatus;
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

    // Pre-computed success response bodies. Per OTLP spec, partial_success must not be set
    // when all records are accepted — so the response is an empty ExportLogsServiceResponse.
    public static final byte[] SUCCESS_RESPONSE_PROTOBUF = ExportLogsServiceResponse.getDefaultInstance().toByteArray();
    public static final byte[] SUCCESS_RESPONSE_JSON = computeJsonSuccessResponse();

    private OtlpHttpUtils() {}

    private static byte[] computeJsonSuccessResponse() {
        try {
            return JsonFormat.printer().print(ExportLogsServiceResponse.getDefaultInstance())
                    .getBytes(StandardCharsets.UTF_8);
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
     * Builds a JSON-serialized {@link Status} body for OTLP error responses.
     * Per the OTLP/HTTP spec, error responses should contain a Status message.
     * JSON is used as the default encoding since the request encoding may not be known
     * (e.g., for 415 Unsupported Media Type or pre-handler errors like 401/404/405).
     */
    public static byte[] buildErrorStatusJson(HttpResponseStatus httpStatus, String message) {
        final int grpcCode = httpStatusToGrpcCode(httpStatus);
        final String msg = message != null ? message : httpStatus.reasonPhrase();
        // Build google.rpc.Status JSON manually to avoid protobuf version conflicts
        // with the com.google.rpc.Status class (compiled against a different protobuf runtime).
        final String json = "{\"code\":" + grpcCode + ",\"message\":\"" + escapeJson(msg) + "\"}";
        return json.getBytes(StandardCharsets.UTF_8);
    }

    private static String escapeJson(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static int httpStatusToGrpcCode(HttpResponseStatus httpStatus) {
        final int code = httpStatus.code();
        if (code == 400 || code == 415) {
            return Status.INVALID_ARGUMENT.getCode().value();
        } else if (code == 401) {
            return Status.UNAUTHENTICATED.getCode().value();
        } else if (code == 404 || code == 405) {
            return Status.UNIMPLEMENTED.getCode().value();
        } else if (code == 429) {
            return Status.RESOURCE_EXHAUSTED.getCode().value();
        } else if (code == 503) {
            return Status.UNAVAILABLE.getCode().value();
        } else {
            return Status.INTERNAL.getCode().value();
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

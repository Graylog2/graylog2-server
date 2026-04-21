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
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;
import io.opentelemetry.proto.collector.logs.v1.ExportLogsServiceRequest;
import io.opentelemetry.proto.collector.logs.v1.ExportLogsServiceResponse;
import io.opentelemetry.proto.common.v1.AnyValue;
import io.opentelemetry.proto.logs.v1.LogRecord;
import io.opentelemetry.proto.logs.v1.ResourceLogs;
import io.opentelemetry.proto.logs.v1.ScopeLogs;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OtlpHttpUtilsTest {

    @Test
    void isProtobufReturnsTrueForProtobufContentType() {
        final FullHttpRequest request = createRequest("application/x-protobuf", new byte[0]);
        assertThat(OtlpHttpUtils.isProtobuf(request)).isTrue();
        request.release();
    }

    @Test
    void isProtobufReturnsFalseForJsonContentType() {
        final FullHttpRequest request = createRequest("application/json", new byte[0]);
        assertThat(OtlpHttpUtils.isProtobuf(request)).isFalse();
        request.release();
    }

    @Test
    void parseProtobufRequest() throws Exception {
        final ExportLogsServiceRequest expected = createTestRequest();
        final FullHttpRequest request = createRequest("application/x-protobuf", expected.toByteArray());

        final ExportLogsServiceRequest parsed = OtlpHttpUtils.parse(request);

        assertThat(parsed).isEqualTo(expected);
        request.release();
    }

    @Test
    void parseJsonRequest() throws Exception {
        final ExportLogsServiceRequest expected = createTestRequest();
        final String json = JsonFormat.printer().print(expected);
        final FullHttpRequest request = createRequest("application/json",
                json.getBytes(StandardCharsets.UTF_8));

        final ExportLogsServiceRequest parsed = OtlpHttpUtils.parse(request);

        assertThat(parsed.getResourceLogs(0).getScopeLogs(0).getLogRecords(0).getBody().getStringValue())
                .isEqualTo("test log message");
        request.release();
    }

    @Test
    void parseUnsupportedContentTypeThrows() {
        final FullHttpRequest request = createRequest("text/plain", "hello".getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> OtlpHttpUtils.parse(request))
                .isInstanceOf(OtlpHttpUtils.UnsupportedContentTypeException.class);
        request.release();
    }

    @Test
    void parseInvalidProtobufThrows() {
        final FullHttpRequest request = createRequest("application/x-protobuf",
                new byte[]{0x01, 0x02, (byte) 0xFF});

        assertThatThrownBy(() -> OtlpHttpUtils.parse(request))
                .isInstanceOf(com.google.protobuf.InvalidProtocolBufferException.class);
        request.release();
    }

    @Test
    void preComputedProtobufResponseIsValid() throws Exception {
        final ExportLogsServiceResponse response = ExportLogsServiceResponse.parseFrom(
                OtlpHttpUtils.SUCCESS_RESPONSE_PROTOBUF);
        // Per OTLP spec, partial_success must not be set when all records are accepted
        assertThat(response.hasPartialSuccess()).isFalse();
    }

    @Test
    void preComputedJsonResponseIsValid() {
        final String json = new String(OtlpHttpUtils.SUCCESS_RESPONSE_JSON, StandardCharsets.UTF_8);
        // Empty default instance serializes to "{}" in JSON
        assertThat(json).isNotEmpty();
    }

    private FullHttpRequest createRequest(String contentType, byte[] body) {
        final FullHttpRequest request = new DefaultFullHttpRequest(
                HttpVersion.HTTP_1_1, HttpMethod.POST, "/v1/logs",
                Unpooled.wrappedBuffer(body));
        request.headers().set(HttpHeaderNames.CONTENT_TYPE, contentType);
        request.headers().set(HttpHeaderNames.CONTENT_LENGTH, body.length);
        return request;
    }

    private ExportLogsServiceRequest createTestRequest() {
        return ExportLogsServiceRequest.newBuilder()
                .addResourceLogs(ResourceLogs.newBuilder()
                        .addScopeLogs(ScopeLogs.newBuilder()
                                .addLogRecords(LogRecord.newBuilder()
                                        .setBody(AnyValue.newBuilder().setStringValue("test log message"))
                                        .build())))
                .build();
    }
}

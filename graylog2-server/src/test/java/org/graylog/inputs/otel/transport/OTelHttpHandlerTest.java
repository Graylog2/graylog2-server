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

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.opentelemetry.proto.collector.logs.v1.ExportLogsServiceRequest;
import io.opentelemetry.proto.collector.logs.v1.ExportLogsServiceResponse;
import io.opentelemetry.proto.common.v1.AnyValue;
import io.opentelemetry.proto.logs.v1.LogRecord;
import io.opentelemetry.proto.logs.v1.ResourceLogs;
import io.opentelemetry.proto.logs.v1.ScopeLogs;
import org.graylog.inputs.otel.OTelJournalRecordFactory;
import org.graylog2.plugin.inputs.MessageInput;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class OTelHttpHandlerTest {

    @Mock
    private MessageInput input;

    private OTelJournalRecordFactory journalRecordFactory;

    @BeforeEach
    void setUp() {
        journalRecordFactory = new OTelJournalRecordFactory();
    }

    @Test
    void postProtobufRequestReturns200WithProtobufResponse() throws InvalidProtocolBufferException {
        final EmbeddedChannel channel = createChannel();
        final ExportLogsServiceRequest request = createTestRequest();

        final FullHttpRequest httpRequest = new DefaultFullHttpRequest(
                HttpVersion.HTTP_1_1, HttpMethod.POST, "/v1/logs",
                Unpooled.wrappedBuffer(request.toByteArray()));
        httpRequest.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/x-protobuf");
        httpRequest.headers().set(HttpHeaderNames.CONTENT_LENGTH, request.toByteArray().length);

        channel.writeInbound(httpRequest);

        final FullHttpResponse response = channel.readOutbound();
        assertThat(response.status()).isEqualTo(HttpResponseStatus.OK);
        assertThat(response.headers().get(HttpHeaderNames.CONTENT_TYPE)).isEqualTo("application/x-protobuf");

        final byte[] responseBytes = new byte[response.content().readableBytes()];
        response.content().readBytes(responseBytes);
        final ExportLogsServiceResponse exportResponse = ExportLogsServiceResponse.parseFrom(responseBytes);
        assertThat(exportResponse.getPartialSuccess().getRejectedLogRecords()).isEqualTo(0);

        verify(input, times(1)).processRawMessage(any());
        response.release();
    }

    @Test
    void postJsonRequestReturns200WithJsonResponse() throws Exception {
        final EmbeddedChannel channel = createChannel();
        final ExportLogsServiceRequest request = createTestRequest();
        final String json = JsonFormat.printer().print(request);

        final FullHttpRequest httpRequest = new DefaultFullHttpRequest(
                HttpVersion.HTTP_1_1, HttpMethod.POST, "/v1/logs",
                Unpooled.wrappedBuffer(json.getBytes(StandardCharsets.UTF_8)));
        httpRequest.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json");
        httpRequest.headers().set(HttpHeaderNames.CONTENT_LENGTH, json.getBytes(StandardCharsets.UTF_8).length);

        channel.writeInbound(httpRequest);

        final FullHttpResponse response = channel.readOutbound();
        assertThat(response.status()).isEqualTo(HttpResponseStatus.OK);
        assertThat(response.headers().get(HttpHeaderNames.CONTENT_TYPE)).isEqualTo("application/json");

        final String responseJson = response.content().toString(StandardCharsets.UTF_8);
        assertThat(responseJson).contains("partialSuccess");

        verify(input, times(1)).processRawMessage(any());
        response.release();
    }

    @Test
    void getRequestReturns405() {
        final EmbeddedChannel channel = createChannel();

        final FullHttpRequest httpRequest = new DefaultFullHttpRequest(
                HttpVersion.HTTP_1_1, HttpMethod.GET, "/v1/logs");
        httpRequest.headers().set(HttpHeaderNames.CONTENT_LENGTH, 0);

        channel.writeInbound(httpRequest);

        final FullHttpResponse response = channel.readOutbound();
        assertThat(response.status()).isEqualTo(HttpResponseStatus.METHOD_NOT_ALLOWED);
        verifyNoInteractions(input);
        response.release();
    }

    @Test
    void wrongPathReturns404() {
        final EmbeddedChannel channel = createChannel();

        final FullHttpRequest httpRequest = new DefaultFullHttpRequest(
                HttpVersion.HTTP_1_1, HttpMethod.POST, "/v1/wrong-path",
                Unpooled.wrappedBuffer(new byte[0]));
        httpRequest.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/x-protobuf");
        httpRequest.headers().set(HttpHeaderNames.CONTENT_LENGTH, 0);

        channel.writeInbound(httpRequest);

        final FullHttpResponse response = channel.readOutbound();
        assertThat(response.status()).isEqualTo(HttpResponseStatus.NOT_FOUND);
        verifyNoInteractions(input);
        response.release();
    }

    @Test
    void unsupportedContentTypeReturns415() {
        final EmbeddedChannel channel = createChannel();

        final FullHttpRequest httpRequest = new DefaultFullHttpRequest(
                HttpVersion.HTTP_1_1, HttpMethod.POST, "/v1/logs",
                Unpooled.wrappedBuffer("test".getBytes(StandardCharsets.UTF_8)));
        httpRequest.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain");
        httpRequest.headers().set(HttpHeaderNames.CONTENT_LENGTH, 4);

        channel.writeInbound(httpRequest);

        final FullHttpResponse response = channel.readOutbound();
        assertThat(response.status()).isEqualTo(HttpResponseStatus.UNSUPPORTED_MEDIA_TYPE);
        verifyNoInteractions(input);
        response.release();
    }

    @Test
    void invalidProtobufReturns400() {
        final EmbeddedChannel channel = createChannel();

        final byte[] invalidBytes = new byte[]{0x01, 0x02, 0x03, (byte) 0xFF, (byte) 0xFE};
        final FullHttpRequest httpRequest = new DefaultFullHttpRequest(
                HttpVersion.HTTP_1_1, HttpMethod.POST, "/v1/logs",
                Unpooled.wrappedBuffer(invalidBytes));
        httpRequest.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/x-protobuf");
        httpRequest.headers().set(HttpHeaderNames.CONTENT_LENGTH, invalidBytes.length);

        channel.writeInbound(httpRequest);

        final FullHttpResponse response = channel.readOutbound();
        assertThat(response.status()).isEqualTo(HttpResponseStatus.BAD_REQUEST);
        verifyNoInteractions(input);
        response.release();
    }

    private EmbeddedChannel createChannel() {
        return new EmbeddedChannel(new OTelHttpHandler(journalRecordFactory, input));
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

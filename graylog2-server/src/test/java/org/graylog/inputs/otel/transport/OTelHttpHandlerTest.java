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
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.opentelemetry.proto.collector.logs.v1.ExportLogsServiceRequest;
import io.opentelemetry.proto.collector.logs.v1.ExportLogsServiceResponse;
import io.opentelemetry.proto.common.v1.AnyValue;
import io.opentelemetry.proto.logs.v1.LogRecord;
import io.opentelemetry.proto.logs.v1.ResourceLogs;
import io.opentelemetry.proto.logs.v1.ScopeLogs;
import org.graylog2.inputs.transports.netty.RawMessageHandler;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.plugin.journal.RawMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class OTelHttpHandlerTest {

    @Mock
    private MessageInput input;


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
        assertThat(exportResponse.hasPartialSuccess()).isFalse();

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
        assertThat(responseJson).isNotEmpty();

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
    void keepAliveRequestKeepsChannelOpen() throws Exception {
        final EmbeddedChannel channel = createChannel();
        final ExportLogsServiceRequest request = createTestRequest();

        final FullHttpRequest httpRequest = new DefaultFullHttpRequest(
                HttpVersion.HTTP_1_1, HttpMethod.POST, "/v1/logs",
                Unpooled.wrappedBuffer(request.toByteArray()));
        httpRequest.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/x-protobuf");
        httpRequest.headers().set(HttpHeaderNames.CONTENT_LENGTH, request.toByteArray().length);
        httpRequest.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);

        channel.writeInbound(httpRequest);

        final FullHttpResponse response = channel.readOutbound();
        assertThat(response.status()).isEqualTo(HttpResponseStatus.OK);
        assertThat(response.headers().get(HttpHeaderNames.CONNECTION))
                .isEqualToIgnoringCase(HttpHeaderValues.KEEP_ALIVE.toString());
        assertThat(channel.isOpen()).isTrue();

        verify(input, times(1)).processRawMessage(any());
        response.release();
    }

    @Test
    void invalidProtobufReturns400WithProtobufContentType() {
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
        assertThat(response.headers().get(HttpHeaderNames.CONTENT_TYPE)).isEqualTo("application/x-protobuf");
        verifyNoInteractions(input);
        response.release();
    }

    @Test
    void invalidJsonReturns400WithJsonContentType() {
        final EmbeddedChannel channel = createChannel();

        final FullHttpRequest httpRequest = new DefaultFullHttpRequest(
                HttpVersion.HTTP_1_1, HttpMethod.POST, "/v1/logs",
                Unpooled.wrappedBuffer("not valid json".getBytes(StandardCharsets.UTF_8)));
        httpRequest.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json");
        httpRequest.headers().set(HttpHeaderNames.CONTENT_LENGTH, 14);

        channel.writeInbound(httpRequest);

        final FullHttpResponse response = channel.readOutbound();
        assertThat(response.status()).isEqualTo(HttpResponseStatus.BAD_REQUEST);
        assertThat(response.headers().get(HttpHeaderNames.CONTENT_TYPE)).isEqualTo("application/json");
        verifyNoInteractions(input);
        response.release();
    }

    @Test
    void requestWithValidAuthReturns200() throws Exception {
        final EmbeddedChannel channel = createChannel(false, "Authorization", "Bearer secret");
        final ExportLogsServiceRequest request = createTestRequest();

        final FullHttpRequest httpRequest = new DefaultFullHttpRequest(
                HttpVersion.HTTP_1_1, HttpMethod.POST, "/v1/logs",
                Unpooled.wrappedBuffer(request.toByteArray()));
        httpRequest.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/x-protobuf");
        httpRequest.headers().set(HttpHeaderNames.CONTENT_LENGTH, request.toByteArray().length);
        httpRequest.headers().set("Authorization", "Bearer secret");

        channel.writeInbound(httpRequest);

        final FullHttpResponse response = channel.readOutbound();
        assertThat(response.status()).isEqualTo(HttpResponseStatus.OK);
        verify(input).processRawMessage(any());
        response.release();
    }

    @Test
    void requestWithBadAuthReturns401() {
        final EmbeddedChannel channel = createChannel(false, "Authorization", "Bearer secret");

        final FullHttpRequest httpRequest = new DefaultFullHttpRequest(
                HttpVersion.HTTP_1_1, HttpMethod.POST, "/v1/logs",
                Unpooled.wrappedBuffer(createTestRequest().toByteArray()));
        httpRequest.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/x-protobuf");
        httpRequest.headers().set(HttpHeaderNames.CONTENT_LENGTH, 1);
        httpRequest.headers().set("Authorization", "Bearer wrong");

        channel.writeInbound(httpRequest);

        final FullHttpResponse response = channel.readOutbound();
        assertThat(response.status()).isEqualTo(HttpResponseStatus.UNAUTHORIZED);
        verifyNoInteractions(input);
        response.release();
    }

    @Test
    void requestWithMissingAuthReturns401() {
        final EmbeddedChannel channel = createChannel(false, "Authorization", "Bearer secret");

        final FullHttpRequest httpRequest = new DefaultFullHttpRequest(
                HttpVersion.HTTP_1_1, HttpMethod.POST, "/v1/logs",
                Unpooled.wrappedBuffer(createTestRequest().toByteArray()));
        httpRequest.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/x-protobuf");
        httpRequest.headers().set(HttpHeaderNames.CONTENT_LENGTH, 1);

        channel.writeInbound(httpRequest);

        final FullHttpResponse response = channel.readOutbound();
        assertThat(response.status()).isEqualTo(HttpResponseStatus.UNAUTHORIZED);
        verifyNoInteractions(input);
        response.release();
    }

    @Test
    void optionsPreflightSucceedsEvenWithAuthConfigured() {
        final EmbeddedChannel channel = createChannel(true, "Authorization", "Bearer secret");

        final FullHttpRequest httpRequest = new DefaultFullHttpRequest(
                HttpVersion.HTTP_1_1, HttpMethod.OPTIONS, "/v1/logs");
        httpRequest.headers().set(HttpHeaderNames.CONTENT_LENGTH, 0);
        httpRequest.headers().set(HttpHeaderNames.ORIGIN, "http://example.com");
        // No Authorization header — browser preflight doesn't send credentials

        channel.writeInbound(httpRequest);

        final FullHttpResponse response = channel.readOutbound();
        assertThat(response.status()).isEqualTo(HttpResponseStatus.OK);
        assertThat(response.headers().get(HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN))
                .isEqualTo("http://example.com");
        verifyNoInteractions(input);
        response.release();
    }

    @Test
    void optionsRequestReturns200() {
        final EmbeddedChannel channel = createChannel(true, null, null);

        final FullHttpRequest httpRequest = new DefaultFullHttpRequest(
                HttpVersion.HTTP_1_1, HttpMethod.OPTIONS, "/v1/logs");
        httpRequest.headers().set(HttpHeaderNames.CONTENT_LENGTH, 0);
        httpRequest.headers().set(HttpHeaderNames.ORIGIN, "http://example.com");

        channel.writeInbound(httpRequest);

        final FullHttpResponse response = channel.readOutbound();
        assertThat(response.status()).isEqualTo(HttpResponseStatus.OK);
        assertThat(response.headers().get(HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN))
                .isEqualTo("http://example.com");
        verifyNoInteractions(input);
        response.release();
    }

    @Test
    void corsHeadersOnSuccessResponse() throws Exception {
        final EmbeddedChannel channel = createChannel(true, null, null);
        final ExportLogsServiceRequest request = createTestRequest();

        final FullHttpRequest httpRequest = new DefaultFullHttpRequest(
                HttpVersion.HTTP_1_1, HttpMethod.POST, "/v1/logs",
                Unpooled.wrappedBuffer(request.toByteArray()));
        httpRequest.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/x-protobuf");
        httpRequest.headers().set(HttpHeaderNames.CONTENT_LENGTH, request.toByteArray().length);
        httpRequest.headers().set(HttpHeaderNames.ORIGIN, "http://example.com");

        channel.writeInbound(httpRequest);

        final FullHttpResponse response = channel.readOutbound();
        assertThat(response.status()).isEqualTo(HttpResponseStatus.OK);
        assertThat(response.headers().get(HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN))
                .isEqualTo("http://example.com");
        response.release();
    }

    @Test
    void forwardedForIpUsedAsSourceAddress() throws Exception {
        final EmbeddedChannel channel = createChannel();
        // Simulate HttpForwardedForHandler having set the original IP
        channel.attr(RawMessageHandler.ORIGINAL_IP_KEY).set(
                new InetSocketAddress(InetAddress.getByName("10.0.0.1"), 0));

        final ExportLogsServiceRequest request = createTestRequest();
        final FullHttpRequest httpRequest = new DefaultFullHttpRequest(
                HttpVersion.HTTP_1_1, HttpMethod.POST, "/v1/logs",
                Unpooled.wrappedBuffer(request.toByteArray()));
        httpRequest.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/x-protobuf");
        httpRequest.headers().set(HttpHeaderNames.CONTENT_LENGTH, request.toByteArray().length);

        channel.writeInbound(httpRequest);

        final FullHttpResponse response = channel.readOutbound();
        assertThat(response.status()).isEqualTo(HttpResponseStatus.OK);

        final ArgumentCaptor<RawMessage> captor = ArgumentCaptor.forClass(RawMessage.class);
        verify(input).processRawMessage(captor.capture());
        assertThat(captor.getValue().getRemoteAddress().getAddress().getHostAddress()).isEqualTo("10.0.0.1");
        response.release();
    }

    @Test
    void processingFailureReturns500WithMatchingContentType() {
        final EmbeddedChannel channel = createChannel();
        doThrow(new RuntimeException("journal full")).when(input).processRawMessage(any());

        final ExportLogsServiceRequest request = createTestRequest();
        final FullHttpRequest httpRequest = new DefaultFullHttpRequest(
                HttpVersion.HTTP_1_1, HttpMethod.POST, "/v1/logs",
                Unpooled.wrappedBuffer(request.toByteArray()));
        httpRequest.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/x-protobuf");
        httpRequest.headers().set(HttpHeaderNames.CONTENT_LENGTH, request.toByteArray().length);

        channel.writeInbound(httpRequest);

        final FullHttpResponse response = channel.readOutbound();
        assertThat(response.status()).isEqualTo(HttpResponseStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.headers().get(HttpHeaderNames.CONTENT_TYPE)).isEqualTo("application/x-protobuf");
        response.release();
    }

    private EmbeddedChannel createChannel() {
        return createChannel(false, null, null);
    }

    private EmbeddedChannel createChannel(boolean enableCors, String authHeader, String authHeaderValue) {
        return new EmbeddedChannel(new OTelHttpHandler(enableCors, authHeader, authHeaderValue,
                OTelHttpHandler.LOGS_PATH, input));
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

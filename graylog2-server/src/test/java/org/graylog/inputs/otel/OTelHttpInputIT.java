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
package org.graylog.inputs.otel;

import com.google.protobuf.util.JsonFormat;
import io.netty.channel.nio.NioEventLoopGroup;
import io.opentelemetry.proto.collector.logs.v1.ExportLogsServiceRequest;
import io.opentelemetry.proto.collector.logs.v1.ExportLogsServiceResponse;
import io.opentelemetry.proto.common.v1.AnyValue;
import io.opentelemetry.proto.logs.v1.LogRecord;
import io.opentelemetry.proto.logs.v1.ResourceLogs;
import io.opentelemetry.proto.logs.v1.ScopeLogs;
import org.graylog.inputs.otel.transport.OTelHttpTransport;
import org.graylog2.configuration.TLSProtocolsConfiguration;
import org.graylog2.inputs.transports.NettyTransportConfiguration;
import org.graylog2.inputs.transports.netty.EventLoopGroupFactory;
import org.graylog2.plugin.LocalMetricRegistry;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.plugin.inputs.util.ThroughputCounter;
import org.graylog2.plugin.journal.RawMessage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Integration tests for the generic OTLP HTTP input using the real transport pipeline.
 * <p>
 * These tests launch {@link OTelHttpTransport} which constructs the full Netty handler
 * chain (including auth, CORS, forwarded-for, HTTP codecs, and the OTel handler).
 * This ensures the pipeline wiring is correct — the bugs this restructuring fixes
 * were invisible to tests that manually wired the pipeline.
 */
@ExtendWith(MockitoExtension.class)
class OTelHttpInputIT {

    @Mock
    private MessageInput input;

    @Mock
    private TLSProtocolsConfiguration tlsConfiguration;

    private OTelHttpTransport transport;
    private NioEventLoopGroup eventLoopGroup;
    private HttpClient httpClient;
    private int port;

    private void launchTransport(Map<String, Object> extraConfig) throws Exception {
        eventLoopGroup = new NioEventLoopGroup(1);
        final var eventLoopGroupFactory = new EventLoopGroupFactory(
                new NettyTransportConfiguration("nio", "jdk", 2));
        final var throughputCounter = new ThroughputCounter(eventLoopGroup);
        final var localRegistry = new LocalMetricRegistry();
        httpClient = HttpClient.newBuilder().build();

        final var configMap = new HashMap<String, Object>();
        configMap.put("bind_address", "127.0.0.1");
        configMap.put("port", 0);
        configMap.put("max_chunk_size", 4 * 1024 * 1024);
        configMap.putAll(extraConfig);
        final var configuration = new Configuration(configMap);

        transport = new OTelHttpTransport(configuration, eventLoopGroup, eventLoopGroupFactory,
                new NettyTransportConfiguration("nio", "jdk", 2),
                throughputCounter, localRegistry, tlsConfiguration, Collections.emptySet());
        transport.launch(input);

        await().atMost(5, TimeUnit.SECONDS).until(() -> transport.getLocalAddress() != null);
        port = ((InetSocketAddress) transport.getLocalAddress()).getPort();
    }

    @AfterEach
    void tearDown() {
        if (transport != null) {
            transport.stop();
        }
        if (eventLoopGroup != null) {
            eventLoopGroup.shutdownGracefully();
        }
    }

    @Test
    void protobufRequestReturns200() throws Exception {
        launchTransport(Map.of());
        final ExportLogsServiceRequest request = createTestRequest();

        final HttpResponse<byte[]> response = httpClient.send(
                HttpRequest.newBuilder()
                        .uri(URI.create("http://127.0.0.1:" + port + "/v1/logs"))
                        .header("Content-Type", "application/x-protobuf")
                        .POST(HttpRequest.BodyPublishers.ofByteArray(request.toByteArray()))
                        .build(),
                HttpResponse.BodyHandlers.ofByteArray());

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().firstValue("content-type")).hasValue("application/x-protobuf");
        final ExportLogsServiceResponse exportResponse = ExportLogsServiceResponse.parseFrom(response.body());
        assertThat(exportResponse.getPartialSuccess().getRejectedLogRecords()).isEqualTo(0);
        verify(input).processRawMessage(any());
    }

    @Test
    void jsonRequestReturns200() throws Exception {
        launchTransport(Map.of());
        final String json = JsonFormat.printer().print(createTestRequest());

        final HttpResponse<String> response = httpClient.send(
                HttpRequest.newBuilder()
                        .uri(URI.create("http://127.0.0.1:" + port + "/v1/logs"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(json))
                        .build(),
                HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().firstValue("content-type")).hasValue("application/json");
        assertThat(response.body()).contains("partialSuccess");
        verify(input).processRawMessage(any());
    }

    @Test
    void authRequiredAndValidHeaderReturns200() throws Exception {
        launchTransport(Map.of(
                "authorization_header_name", "Authorization",
                "authorization_header_value", "Bearer secret"));
        final ExportLogsServiceRequest request = createTestRequest();

        final HttpResponse<byte[]> response = httpClient.send(
                HttpRequest.newBuilder()
                        .uri(URI.create("http://127.0.0.1:" + port + "/v1/logs"))
                        .header("Content-Type", "application/x-protobuf")
                        .header("Authorization", "Bearer secret")
                        .POST(HttpRequest.BodyPublishers.ofByteArray(request.toByteArray()))
                        .build(),
                HttpResponse.BodyHandlers.ofByteArray());

        assertThat(response.statusCode()).isEqualTo(200);
        verify(input).processRawMessage(any());
    }

    @Test
    void authRequiredAndMissingHeaderReturns401() throws Exception {
        launchTransport(Map.of(
                "authorization_header_name", "Authorization",
                "authorization_header_value", "Bearer secret"));

        final HttpResponse<byte[]> response = httpClient.send(
                HttpRequest.newBuilder()
                        .uri(URI.create("http://127.0.0.1:" + port + "/v1/logs"))
                        .header("Content-Type", "application/x-protobuf")
                        .POST(HttpRequest.BodyPublishers.ofByteArray(createTestRequest().toByteArray()))
                        .build(),
                HttpResponse.BodyHandlers.ofByteArray());

        assertThat(response.statusCode()).isEqualTo(401);
        verify(input, never()).processRawMessage(any());
    }

    @Test
    void authRequiredAndWrongHeaderReturns401() throws Exception {
        launchTransport(Map.of(
                "authorization_header_name", "Authorization",
                "authorization_header_value", "Bearer secret"));

        final HttpResponse<byte[]> response = httpClient.send(
                HttpRequest.newBuilder()
                        .uri(URI.create("http://127.0.0.1:" + port + "/v1/logs"))
                        .header("Content-Type", "application/x-protobuf")
                        .header("Authorization", "Bearer wrong")
                        .POST(HttpRequest.BodyPublishers.ofByteArray(createTestRequest().toByteArray()))
                        .build(),
                HttpResponse.BodyHandlers.ofByteArray());

        assertThat(response.statusCode()).isEqualTo(401);
        verify(input, never()).processRawMessage(any());
    }

    @Test
    void corsEnabledOptionsReturns200WithHeaders() throws Exception {
        launchTransport(Map.of("enable_cors", true));

        final HttpResponse<byte[]> response = httpClient.send(
                HttpRequest.newBuilder()
                        .uri(URI.create("http://127.0.0.1:" + port + "/v1/logs"))
                        .header("Content-Type", "application/x-protobuf")
                        .header("Origin", "http://example.com")
                        .POST(HttpRequest.BodyPublishers.ofByteArray(createTestRequest().toByteArray()))
                        .build(),
                HttpResponse.BodyHandlers.ofByteArray());

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().firstValue("access-control-allow-origin"))
                .hasValue("http://example.com");
    }

    @Test
    void forwardedForIpUsedAsSourceAddress() throws Exception {
        launchTransport(Map.of("enable_forwarded_for", true));

        final HttpResponse<byte[]> response = httpClient.send(
                HttpRequest.newBuilder()
                        .uri(URI.create("http://127.0.0.1:" + port + "/v1/logs"))
                        .header("Content-Type", "application/x-protobuf")
                        .header("X-Forwarded-For", "10.0.0.1")
                        .POST(HttpRequest.BodyPublishers.ofByteArray(createTestRequest().toByteArray()))
                        .build(),
                HttpResponse.BodyHandlers.ofByteArray());

        assertThat(response.statusCode()).isEqualTo(200);
        final ArgumentCaptor<RawMessage> captor = ArgumentCaptor.forClass(RawMessage.class);
        verify(input).processRawMessage(captor.capture());
        assertThat(captor.getValue().getRemoteAddress().getAddress().getHostAddress())
                .isEqualTo("10.0.0.1");
    }

    @Test
    void wrongPathReturns404() throws Exception {
        launchTransport(Map.of());

        final HttpResponse<byte[]> response = httpClient.send(
                HttpRequest.newBuilder()
                        .uri(URI.create("http://127.0.0.1:" + port + "/v1/wrong"))
                        .header("Content-Type", "application/x-protobuf")
                        .POST(HttpRequest.BodyPublishers.ofByteArray(new byte[0]))
                        .build(),
                HttpResponse.BodyHandlers.ofByteArray());

        assertThat(response.statusCode()).isEqualTo(404);
        verify(input, never()).processRawMessage(any());
    }

    @Test
    void unsupportedContentTypeReturns415() throws Exception {
        launchTransport(Map.of());

        final HttpResponse<byte[]> response = httpClient.send(
                HttpRequest.newBuilder()
                        .uri(URI.create("http://127.0.0.1:" + port + "/v1/logs"))
                        .header("Content-Type", "text/plain")
                        .POST(HttpRequest.BodyPublishers.ofString("hello"))
                        .build(),
                HttpResponse.BodyHandlers.ofByteArray());

        assertThat(response.statusCode()).isEqualTo(415);
        verify(input, never()).processRawMessage(any());
    }

    @Test
    void getMethodReturns405() throws Exception {
        launchTransport(Map.of());

        final HttpResponse<byte[]> response = httpClient.send(
                HttpRequest.newBuilder()
                        .uri(URI.create("http://127.0.0.1:" + port + "/v1/logs"))
                        .GET()
                        .build(),
                HttpResponse.BodyHandlers.ofByteArray());

        assertThat(response.statusCode()).isEqualTo(405);
        verify(input, never()).processRawMessage(any());
    }

    private ExportLogsServiceRequest createTestRequest() {
        return ExportLogsServiceRequest.newBuilder()
                .addResourceLogs(ResourceLogs.newBuilder()
                        .addScopeLogs(ScopeLogs.newBuilder()
                                .addLogRecords(LogRecord.newBuilder()
                                        .setBody(AnyValue.newBuilder().setStringValue("test log message"))
                                        .setTimeUnixNano(System.nanoTime())
                                        .setSeverityText("INFO"))))
                .build();
    }
}

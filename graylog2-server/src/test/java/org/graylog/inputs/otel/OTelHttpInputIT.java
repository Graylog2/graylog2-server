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
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.opentelemetry.proto.collector.logs.v1.ExportLogsServiceRequest;
import io.opentelemetry.proto.collector.logs.v1.ExportLogsServiceResponse;
import io.opentelemetry.proto.common.v1.AnyValue;
import io.opentelemetry.proto.logs.v1.LogRecord;
import io.opentelemetry.proto.logs.v1.ResourceLogs;
import io.opentelemetry.proto.logs.v1.ScopeLogs;
import org.graylog.inputs.otel.transport.OTelHttpHandler;
import org.graylog2.plugin.inputs.MessageInput;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

/**
 * Integration tests for the generic OTLP HTTP input (no mTLS).
 * <p>
 * These tests start a real Netty HTTP server with the {@link OTelHttpHandler}
 * and send real HTTP requests to verify end-to-end behavior.
 */
@ExtendWith(MockitoExtension.class)
class OTelHttpInputIT {

    @Mock
    private MessageInput input;

    private Channel serverChannel;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private HttpClient httpClient;
    private int port;

    @BeforeEach
    void setUp() throws Exception {
        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup(2);
        httpClient = HttpClient.newBuilder().build();

        final OTelJournalRecordFactory journalRecordFactory = new OTelJournalRecordFactory();

        final ServerBootstrap bootstrap = new ServerBootstrap()
                .group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        final ChannelPipeline pipeline = ch.pipeline();
                        pipeline.addLast("http-codec", new HttpServerCodec());
                        pipeline.addLast("http-aggregator", new HttpObjectAggregator(1024 * 1024));
                        pipeline.addLast("http-handler", new OTelHttpHandler(journalRecordFactory, input));
                    }
                });

        serverChannel = bootstrap.bind("127.0.0.1", 0).sync().channel();
        port = ((InetSocketAddress) serverChannel.localAddress()).getPort();
    }

    @AfterEach
    void tearDown() throws Exception {
        if (serverChannel != null) {
            serverChannel.close().sync();
        }
        bossGroup.shutdownGracefully().sync();
        workerGroup.shutdownGracefully().sync();
    }

    @Test
    void protobufContentTypeReturns200() throws Exception {
        final ExportLogsServiceRequest request = createTestRequest();

        final HttpResponse<byte[]> response = httpClient.send(
                HttpRequest.newBuilder()
                        .uri(URI.create("http://127.0.0.1:" + port + "/v1/logs"))
                        .header("Content-Type", "application/x-protobuf")
                        .POST(HttpRequest.BodyPublishers.ofByteArray(request.toByteArray()))
                        .build(),
                HttpResponse.BodyHandlers.ofByteArray()
        );

        assertThat(response.statusCode()).isEqualTo(200);
        verify(input).processRawMessage(any());
    }

    @Test
    void jsonContentTypeReturns200() throws Exception {
        final ExportLogsServiceRequest request = createTestRequest();
        final String json = JsonFormat.printer().print(request);

        final HttpResponse<byte[]> response = httpClient.send(
                HttpRequest.newBuilder()
                        .uri(URI.create("http://127.0.0.1:" + port + "/v1/logs"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofByteArray(json.getBytes(StandardCharsets.UTF_8)))
                        .build(),
                HttpResponse.BodyHandlers.ofByteArray()
        );

        assertThat(response.statusCode()).isEqualTo(200);
        verify(input).processRawMessage(any());
    }

    @Test
    void protobufResponseContentTypeMirrorsRequest() throws Exception {
        final ExportLogsServiceRequest request = createTestRequest();

        final HttpResponse<byte[]> response = httpClient.send(
                HttpRequest.newBuilder()
                        .uri(URI.create("http://127.0.0.1:" + port + "/v1/logs"))
                        .header("Content-Type", "application/x-protobuf")
                        .POST(HttpRequest.BodyPublishers.ofByteArray(request.toByteArray()))
                        .build(),
                HttpResponse.BodyHandlers.ofByteArray()
        );

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().firstValue("content-type")).hasValue("application/x-protobuf");

        // Verify the response can be parsed as protobuf
        final ExportLogsServiceResponse exportResponse = ExportLogsServiceResponse.parseFrom(response.body());
        assertThat(exportResponse.getPartialSuccess().getRejectedLogRecords()).isEqualTo(0);
    }

    @Test
    void jsonResponseContentTypeMirrorsRequest() throws Exception {
        final ExportLogsServiceRequest request = createTestRequest();
        final String json = JsonFormat.printer().print(request);

        final HttpResponse<String> response = httpClient.send(
                HttpRequest.newBuilder()
                        .uri(URI.create("http://127.0.0.1:" + port + "/v1/logs"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofByteArray(json.getBytes(StandardCharsets.UTF_8)))
                        .build(),
                HttpResponse.BodyHandlers.ofString()
        );

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().firstValue("content-type")).hasValue("application/json");

        // Verify the response is valid JSON
        assertThat(response.body()).contains("partialSuccess");
    }

    private ExportLogsServiceRequest createTestRequest() {
        return ExportLogsServiceRequest.newBuilder()
                .addResourceLogs(ResourceLogs.newBuilder()
                        .addScopeLogs(ScopeLogs.newBuilder()
                                .addLogRecords(LogRecord.newBuilder()
                                        .setBody(AnyValue.newBuilder().setStringValue("test log message"))
                                        .setTimeUnixNano(System.nanoTime())
                                        .setSeverityText("INFO")
                                )))
                .build();
    }
}

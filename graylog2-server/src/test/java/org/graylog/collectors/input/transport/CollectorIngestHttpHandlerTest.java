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
import io.opentelemetry.proto.common.v1.AnyValue;
import io.opentelemetry.proto.logs.v1.LogRecord;
import io.opentelemetry.proto.logs.v1.ResourceLogs;
import io.opentelemetry.proto.logs.v1.ScopeLogs;
import org.graylog.collectors.CollectorJournal;
import org.graylog.inputs.otel.OTelJournalRecordFactory;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.plugin.journal.RawMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class CollectorIngestHttpHandlerTest {

    @Mock
    private MessageInput input;

    private OTelJournalRecordFactory journalRecordFactory;

    @BeforeEach
    void setUp() {
        journalRecordFactory = new OTelJournalRecordFactory();
    }

    @Test
    void rejectsRequestWithoutAgentIdentity() {
        final EmbeddedChannel channel = createChannel(null);
        final ExportLogsServiceRequest request = createTestRequest();

        final FullHttpRequest httpRequest = new DefaultFullHttpRequest(
                HttpVersion.HTTP_1_1, HttpMethod.POST, "/v1/logs",
                Unpooled.wrappedBuffer(request.toByteArray()));
        httpRequest.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/x-protobuf");
        httpRequest.headers().set(HttpHeaderNames.CONTENT_LENGTH, request.toByteArray().length);

        channel.writeInbound(httpRequest);

        final FullHttpResponse response = channel.readOutbound();
        assertThat(response.status()).isEqualTo(HttpResponseStatus.UNAUTHORIZED);
        verifyNoInteractions(input);
        response.release();
    }

    @Test
    void collectorInstanceUidIsEmbeddedInJournalRecord() throws Exception {
        final String agentUid = "agent-uid-42";
        final EmbeddedChannel channel = createChannel(agentUid);
        final ExportLogsServiceRequest request = createTestRequest();

        final FullHttpRequest httpRequest = new DefaultFullHttpRequest(
                HttpVersion.HTTP_1_1, HttpMethod.POST, "/v1/logs",
                Unpooled.wrappedBuffer(request.toByteArray()));
        httpRequest.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/x-protobuf");
        httpRequest.headers().set(HttpHeaderNames.CONTENT_LENGTH, request.toByteArray().length);

        channel.writeInbound(httpRequest);

        final ArgumentCaptor<RawMessage> captor = ArgumentCaptor.forClass(RawMessage.class);
        verify(input, times(1)).processRawMessage(captor.capture());

        final RawMessage rawMessage = captor.getValue();
        final CollectorJournal.Record record = CollectorJournal.Record.parseFrom(rawMessage.getPayload());
        assertThat(record.hasCollectorInstanceUid()).isTrue();
        assertThat(record.getCollectorInstanceUid()).isEqualTo(agentUid);

        final FullHttpResponse response = channel.readOutbound();
        response.release();
    }

    @Test
    void multipleLogRecordsEachGetCollectorInstanceUid() throws Exception {
        final String agentUid = "multi-agent-001";
        final EmbeddedChannel channel = createChannel(agentUid);

        final ExportLogsServiceRequest request = ExportLogsServiceRequest.newBuilder()
                .addResourceLogs(ResourceLogs.newBuilder()
                        .addScopeLogs(ScopeLogs.newBuilder()
                                .addLogRecords(LogRecord.newBuilder()
                                        .setBody(AnyValue.newBuilder().setStringValue("log message 1"))
                                        .build())
                                .addLogRecords(LogRecord.newBuilder()
                                        .setBody(AnyValue.newBuilder().setStringValue("log message 2"))
                                        .build())))
                .build();

        final FullHttpRequest httpRequest = new DefaultFullHttpRequest(
                HttpVersion.HTTP_1_1, HttpMethod.POST, "/v1/logs",
                Unpooled.wrappedBuffer(request.toByteArray()));
        httpRequest.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/x-protobuf");
        httpRequest.headers().set(HttpHeaderNames.CONTENT_LENGTH, request.toByteArray().length);

        channel.writeInbound(httpRequest);

        final ArgumentCaptor<RawMessage> captor = ArgumentCaptor.forClass(RawMessage.class);
        verify(input, times(2)).processRawMessage(captor.capture());

        for (final RawMessage rawMessage : captor.getAllValues()) {
            final CollectorJournal.Record record = CollectorJournal.Record.parseFrom(rawMessage.getPayload());
            assertThat(record.getCollectorInstanceUid()).isEqualTo(agentUid);
        }

        final FullHttpResponse response = channel.readOutbound();
        response.release();
    }

    @Test
    void keepAliveRequestKeepsChannelOpen() throws Exception {
        final String agentUid = "keep-alive-agent";
        final EmbeddedChannel channel = createChannel(agentUid);
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

    private EmbeddedChannel createChannel(String agentInstanceUid) {
        final EmbeddedChannel channel = new EmbeddedChannel(
                new CollectorIngestHttpHandler(journalRecordFactory, input));
        if (agentInstanceUid != null) {
            channel.attr(AgentCertChannelHandler.AGENT_INSTANCE_UID).set(agentInstanceUid);
        }
        return channel;
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

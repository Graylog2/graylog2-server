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

import com.google.common.io.Resources;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import io.grpc.Context;
import io.grpc.stub.StreamObserver;
import io.opentelemetry.proto.collector.logs.v1.ExportLogsServiceRequest;
import io.opentelemetry.proto.collector.logs.v1.ExportLogsServiceResponse;
import org.graylog.collectors.CollectorJournal;
import org.graylog.inputs.otel.OTelGrpcInput;
import org.graylog.inputs.otel.OTelJournalRecordFactory;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.plugin.inputs.transports.ThrottleableTransport2;
import org.graylog2.plugin.journal.RawMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CollectorIngestLogsServiceTest {

    @Mock
    private ThrottleableTransport2 transport;
    @Mock
    private MessageInput input;
    @Mock
    private StreamObserver<ExportLogsServiceResponse> responseObserver;

    private CollectorIngestLogsService logsService;

    @BeforeEach
    void setUp() {
        logsService = new CollectorIngestLogsService(transport, input, new OTelJournalRecordFactory());
    }

    @Test
    void exportSetsCollectorInstanceUidWhenPresentInContext() throws IOException {
        final var request = buildLogsRequest();
        final String expectedUid = "test-agent-uid-123";

        final Context contextWithUid = Context.current().withValue(AgentCertAuthInterceptor.AGENT_INSTANCE_UID, expectedUid);
        contextWithUid.run(() -> logsService.export(request, responseObserver));

        final ArgumentCaptor<RawMessage> captor = ArgumentCaptor.forClass(RawMessage.class);
        verify(input).processRawMessage(captor.capture());

        final CollectorJournal.Record record = parseCollectorJournalRecord(captor.getValue().getPayload());
        assertThat(record.hasCollectorInstanceUid()).isTrue();
        assertThat(record.getCollectorInstanceUid()).isEqualTo(expectedUid);

        verify(responseObserver).onNext(eq(ExportLogsServiceResponse.newBuilder().build()));
        verify(responseObserver).onCompleted();
    }

    @Test
    void exportDoesNotSetCollectorInstanceUidWhenAbsentFromContext() throws IOException {
        final var request = buildLogsRequest();

        // Run without setting AGENT_INSTANCE_UID in context
        logsService.export(request, responseObserver);

        final ArgumentCaptor<RawMessage> captor = ArgumentCaptor.forClass(RawMessage.class);
        verify(input).processRawMessage(captor.capture());

        final CollectorJournal.Record record = parseCollectorJournalRecord(captor.getValue().getPayload());
        assertThat(record.hasCollectorInstanceUid()).isFalse();

        verify(responseObserver).onNext(eq(ExportLogsServiceResponse.newBuilder().build()));
        verify(responseObserver).onCompleted();
    }

    private ExportLogsServiceRequest buildLogsRequest() throws IOException {
        final var requestBuilder = ExportLogsServiceRequest.newBuilder();
        JsonFormat.parser().merge(
                Resources.toString(Resources.getResource(OTelGrpcInput.class, "logs.json"), StandardCharsets.UTF_8),
                requestBuilder);
        return requestBuilder.build();
    }

    private CollectorJournal.Record parseCollectorJournalRecord(byte[] payload) {
        try {
            return CollectorJournal.Record.parseFrom(payload);
        } catch (InvalidProtocolBufferException e) {
            throw new RuntimeException(e);
        }
    }
}

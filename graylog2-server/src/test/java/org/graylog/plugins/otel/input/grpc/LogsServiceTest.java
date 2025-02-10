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
package org.graylog.plugins.otel.input.grpc;

import com.google.common.io.Resources;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import io.grpc.stub.StreamObserver;
import io.opentelemetry.proto.collector.logs.v1.ExportLogsServiceRequest;
import io.opentelemetry.proto.collector.logs.v1.ExportLogsServiceResponse;
import io.opentelemetry.proto.common.v1.AnyValue;
import io.opentelemetry.proto.common.v1.KeyValue;
import org.graylog.grpc.auth.CallAuthorizer;
import org.graylog.plugins.otel.input.Journal;
import org.graylog.plugins.otel.input.JournalRecordFactory;
import org.graylog.plugins.otel.input.OpenTelemetryGrpcInput;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.plugin.inputs.transports.ThrottleableTransport2;
import org.graylog2.shared.security.RestPermissions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LogsServiceTest {

    @Mock
    private ThrottleableTransport2 transport;
    @Mock
    private MessageInput input;
    @Mock
    private StreamObserver<ExportLogsServiceResponse> responseObserver;
    @Mock
    private CallAuthorizer callAuthorizer;

    private LogsService logsService;

    @BeforeEach
    void setUp() {
        when(callAuthorizer.verifyPermitted(any(StreamObserver.class), eq(RestPermissions.MESSAGES_READ)))
                .thenReturn(true);
        logsService = new LogsService(transport, input, new JournalRecordFactory(), callAuthorizer);
    }

    // Test processing a request using the official example from
    // https://github.com/open-telemetry/opentelemetry-proto/blob/7312bdf63218acf27fe96430b7231de37fd091f2/examples/logs.json
    @Test
    void testExport() throws IOException {
        final var requestBuilder = ExportLogsServiceRequest.newBuilder();
        JsonFormat.parser().merge(
                Resources.toString(Resources.getResource(OpenTelemetryGrpcInput.class, "logs.json"), StandardCharsets.UTF_8),
                requestBuilder);

        logsService.export(requestBuilder.build(), responseObserver);

        verify(input).processRawMessage(argThat(arg -> {
            assertThat(arg.getPayload()).isNotEmpty();
            assertThat(parseJournalRecord(arg.getPayload())).satisfies(jr -> {
                assertThat(jr.getLog().getResource().getAttributesList()).contains(
                        KeyValue.newBuilder().setKey("service.name").setValue(
                                AnyValue.newBuilder().setStringValue("my.service")).build());
                assertThat(jr.getLog().getScope().getName()).isEqualTo("my.library");
                assertThat(jr.getLog().getLogRecord().getAttributesList()).contains(
                        KeyValue.newBuilder().setKey("double.attribute").setValue(
                                AnyValue.newBuilder().setDoubleValue(637.704)).build());
            });
            return true;
        }));

        verify(responseObserver).onNext(eq(ExportLogsServiceResponse.newBuilder().build()));
        verify(responseObserver).onCompleted();
    }

    private Journal.Record parseJournalRecord(byte[] payload) {
        try {
            return Journal.Record.parseFrom(payload);
        } catch (InvalidProtocolBufferException e) {
            throw new RuntimeException(e);
        }
    }
}

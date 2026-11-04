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
package org.graylog.inputs.grpc;

import com.google.common.io.Resources;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import com.google.rpc.RetryInfo;
import io.grpc.Context;
import io.grpc.Metadata;
import io.grpc.Status;
import io.grpc.protobuf.StatusProto;
import io.grpc.stub.MetadataUtils;
import io.grpc.stub.StreamObserver;
import io.opentelemetry.proto.collector.logs.v1.ExportLogsServiceRequest;
import io.opentelemetry.proto.collector.logs.v1.ExportLogsServiceResponse;
import io.opentelemetry.proto.collector.logs.v1.LogsServiceGrpc;
import io.opentelemetry.proto.common.v1.AnyValue;
import io.opentelemetry.proto.common.v1.InstrumentationScope;
import io.opentelemetry.proto.common.v1.KeyValue;
import io.opentelemetry.proto.logs.v1.LogRecord;
import io.opentelemetry.proto.logs.v1.ResourceLogs;
import io.opentelemetry.proto.logs.v1.ScopeLogs;
import io.opentelemetry.proto.resource.v1.Resource;
import org.graylog.inputs.otel.OTelGrpcInput;
import org.graylog.inputs.otel.OTelJournal;
import org.graylog.inputs.otel.transport.OTelLogsService;
import org.graylog.testing.grpc.InProcessGrpc;
import org.graylog.testing.grpc.InProcessGrpcExtension;
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
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.graylog.inputs.grpc.RemoteAddressProviderInterceptor.REMOTE_ADDRESS;
import static org.graylog.testing.grpc.GrpcAssertions.assertStatus;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@ExtendWith(InProcessGrpcExtension.class)
class OTelLogsServiceTest {

    @Mock
    private ThrottleableTransport2 transport;
    @Mock
    private MessageInput input;
    @Mock
    private StreamObserver<ExportLogsServiceResponse> responseObserver;

    private OTelLogsService logsService;

    @BeforeEach
    void setUp() {
        logsService = new OTelLogsService(transport, input);
    }

    // Test processing a request using the official example from
    // https://github.com/open-telemetry/opentelemetry-proto/blob/7312bdf63218acf27fe96430b7231de37fd091f2/examples/logs.json
    @Test
    void testExport() throws IOException {
        final var requestBuilder = ExportLogsServiceRequest.newBuilder();
        JsonFormat.parser().merge(
                Resources.toString(Resources.getResource(OTelGrpcInput.class, "logs.json"), StandardCharsets.UTF_8),
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

    @Test
    void exportDistributesRequestSizeAcrossRecords() {
        final ExportLogsServiceRequest request = ExportLogsServiceRequest.newBuilder()
                .addResourceLogs(ResourceLogs.newBuilder()
                        .setResource(Resource.newBuilder()
                                .addAttributes(KeyValue.newBuilder()
                                        .setKey("service.name")
                                        .setValue(AnyValue.newBuilder().setStringValue("test-service"))))
                        .addScopeLogs(ScopeLogs.newBuilder()
                                .setScope(InstrumentationScope.newBuilder().setName("test-scope"))
                                .addLogRecords(LogRecord.newBuilder()
                                        .setBody(AnyValue.newBuilder().setStringValue("log message 1")))
                                .addLogRecords(LogRecord.newBuilder()
                                        .setBody(AnyValue.newBuilder().setStringValue("log message 2")))
                                .addLogRecords(LogRecord.newBuilder()
                                        .setBody(AnyValue.newBuilder().setStringValue("log message 3")))))
                .build();

        logsService.export(request, responseObserver);

        final ArgumentCaptor<RawMessage> captor = ArgumentCaptor.forClass(RawMessage.class);
        verify(input, times(3)).processRawMessage(captor.capture());

        final List<RawMessage> captured = captor.getAllValues();

        for (final RawMessage raw : captured) {
            assertThat(raw.getInputMessageSize())
                    .as("Each RawMessage should carry a non-zero proportional request size")
                    .isGreaterThan(0);
        }

        final long totalAssigned = captured.stream()
                .mapToLong(RawMessage::getInputMessageSize)
                .sum();
        assertThat(totalAssigned)
                .as("Total distributed size should equal the original request size")
                .isEqualTo(request.getSerializedSize());
    }

    @Test
    void exportWithSingleLogRecordAssignsFullRequestSize() {
        final ExportLogsServiceRequest request = singleRecordRequest();

        logsService.export(request, responseObserver);

        final ArgumentCaptor<RawMessage> captor = ArgumentCaptor.forClass(RawMessage.class);
        verify(input).processRawMessage(captor.capture());

        assertThat(captor.getValue().getInputMessageSize())
                .as("Single log record should get the full request serialized size")
                .isEqualTo(request.getSerializedSize());
    }

    @Test
    void exportFailsWithCancelledIfCallWasCancelled() {
        final Context.CancellableContext context = Context.current().withCancellation();
        context.cancel(null);

        context.run(() -> logsService.export(singleRecordRequest(), responseObserver));

        final ArgumentCaptor<Throwable> captor = ArgumentCaptor.forClass(Throwable.class);
        verify(responseObserver).onError(captor.capture());
        verifyNoMoreInteractions(responseObserver);
        assertThat(Status.fromThrowable(captor.getValue()).getCode()).isEqualTo(Status.Code.CANCELLED);
        verifyNoInteractions(input);
    }

    @Test
    void exportFailsWithUnavailableIfTransportIsThrottled() {
        when(transport.isThrottled()).thenReturn(true);

        logsService.export(singleRecordRequest(), responseObserver);

        final ArgumentCaptor<Throwable> captor = ArgumentCaptor.forClass(Throwable.class);
        verify(responseObserver).onError(captor.capture());
        verifyNoMoreInteractions(responseObserver);
        assertThat(Status.fromThrowable(captor.getValue()).getCode()).isEqualTo(Status.Code.UNAVAILABLE);
        verifyNoInteractions(input);
    }

    @Test
    void exportAddsRemoteAddressFromContextToRawMessage() {
        final var remoteAddress = new InetSocketAddress("192.0.2.10", 54321);

        Context.current().withValue(REMOTE_ADDRESS, remoteAddress)
                .run(() -> logsService.export(singleRecordRequest(), responseObserver));

        final ArgumentCaptor<RawMessage> captor = ArgumentCaptor.forClass(RawMessage.class);
        verify(input).processRawMessage(captor.capture());
        assertThat(captor.getValue().getRemoteAddress()).isNotNull();
        assertThat(captor.getValue().getRemoteAddress().getInetSocketAddress()).isEqualTo(remoteAddress);
    }

    @Test
    void exportWithoutRemoteAddressInContextCreatesRawMessageWithoutRemoteAddress() {
        logsService.export(singleRecordRequest(), responseObserver);

        final ArgumentCaptor<RawMessage> captor = ArgumentCaptor.forClass(RawMessage.class);
        verify(input).processRawMessage(captor.capture());
        assertThat(captor.getValue().getRemoteAddress()).isNull();
    }

    @Test
    void exportOverGrpcWithInterceptorsProcessesRecords(InProcessGrpc grpc) throws IOException {
        final var headers = new Metadata();
        headers.put(Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER), "Bearer s3cr3t");

        // Same interceptor setup as AbstractGrpcTransport with a configured bearer token.
        final var stub = LogsServiceGrpc.newBlockingStub(grpc.start(logsService,
                        new RemoteAddressProviderInterceptor(),
                        new BearerTokenAuthInterceptor("s3cr3t")))
                .withInterceptors(MetadataUtils.newAttachHeadersInterceptor(headers));

        assertThat(stub.export(singleRecordRequest())).isEqualTo(ExportLogsServiceResponse.getDefaultInstance());

        final ArgumentCaptor<RawMessage> captor = ArgumentCaptor.forClass(RawMessage.class);
        verify(input).processRawMessage(captor.capture());
        // The in-process transport doesn't provide an InetSocketAddress.
        assertThat(captor.getValue().getRemoteAddress()).isNull();
    }

    @Test
    void throttledStatusWithRetryInfoReachesGrpcClient(InProcessGrpc grpc) throws IOException {
        when(transport.isThrottled()).thenReturn(true);

        final var stub = LogsServiceGrpc.newBlockingStub(grpc.start(logsService));

        final var e = assertStatus(() -> stub.export(singleRecordRequest()), Status.Code.UNAVAILABLE);

        final com.google.rpc.Status status = StatusProto.fromThrowable(e);
        assertThat(status).isNotNull();
        assertThat(status.getDetailsList()).singleElement().satisfies(detail ->
                assertThat(detail.unpack(RetryInfo.class).getRetryDelay().getSeconds()).isEqualTo(30));
        verifyNoInteractions(input);
    }

    private static ExportLogsServiceRequest singleRecordRequest() {
        return ExportLogsServiceRequest.newBuilder()
                .addResourceLogs(ResourceLogs.newBuilder()
                        .setResource(Resource.newBuilder()
                                .addAttributes(KeyValue.newBuilder()
                                        .setKey("service.name")
                                        .setValue(AnyValue.newBuilder().setStringValue("test-service"))))
                        .addScopeLogs(ScopeLogs.newBuilder()
                                .setScope(InstrumentationScope.newBuilder().setName("test-scope"))
                                .addLogRecords(LogRecord.newBuilder()
                                        .setBody(AnyValue.newBuilder().setStringValue("single log message")))))
                .build();
    }

    private OTelJournal.Record parseJournalRecord(byte[] payload) {
        try {
            return OTelJournal.Record.parseFrom(payload);
        } catch (InvalidProtocolBufferException e) {
            throw new RuntimeException(e);
        }
    }
}

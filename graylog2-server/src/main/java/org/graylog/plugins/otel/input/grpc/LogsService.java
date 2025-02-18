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

import com.google.inject.assistedinject.Assisted;
import com.google.protobuf.AbstractMessageLite;
import io.grpc.Context;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import io.opentelemetry.proto.collector.logs.v1.ExportLogsServiceRequest;
import io.opentelemetry.proto.collector.logs.v1.ExportLogsServiceResponse;
import io.opentelemetry.proto.collector.logs.v1.LogsServiceGrpc;
import jakarta.inject.Inject;
import org.graylog.plugins.otel.input.JournalRecordFactory;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.plugin.inputs.transports.ThrottleableTransport2;
import org.graylog2.plugin.journal.RawMessage;

import java.net.InetSocketAddress;
import java.util.function.Function;

import static org.graylog.plugins.otel.input.grpc.RemoteAddressProviderInterceptor.REMOTE_ADDRESS;
import static org.graylog.plugins.otel.input.grpc.Utils.createThrottledStatusRuntimeException;

public class LogsService extends LogsServiceGrpc.LogsServiceImplBase {
    private final JournalRecordFactory journalRecordFactory;
    private final ThrottleableTransport2 transport;
    private final MessageInput input;

    @Inject
    public LogsService(@Assisted ThrottleableTransport2 transport, @Assisted MessageInput input,
                       JournalRecordFactory journalRecordFactory) {
        this.transport = transport;
        this.input = input;
        this.journalRecordFactory = journalRecordFactory;
    }

    public interface Factory {
        LogsService create(ThrottleableTransport2 transport, MessageInput input);
    }

    @Override
    public void export(ExportLogsServiceRequest request,
                       StreamObserver<ExportLogsServiceResponse> responseObserver) {

        if (Context.current().isCancelled()) {
            responseObserver.onError(Status.CANCELLED.withDescription("Cancelled by client").asRuntimeException());
            return;
        }

        if (transport.isThrottled()) {
            responseObserver.onError(createThrottledStatusRuntimeException());
            return;
        }

        final Function<byte[], RawMessage> createRawMessage;
        if (REMOTE_ADDRESS.get() instanceof InetSocketAddress address) {
            createRawMessage = bytes -> new RawMessage(bytes, address);
        } else {
            createRawMessage = RawMessage::new;
        }

        journalRecordFactory.createFromRequest(request).stream()
                .map(AbstractMessageLite::toByteArray)
                .map(createRawMessage)
                .forEach(input::processRawMessage);

        responseObserver.onNext(ExportLogsServiceResponse.newBuilder().build());
        responseObserver.onCompleted();
    }
}

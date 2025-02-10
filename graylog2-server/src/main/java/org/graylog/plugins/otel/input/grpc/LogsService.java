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
import io.grpc.Context;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import io.opentelemetry.proto.collector.logs.v1.ExportLogsServiceRequest;
import io.opentelemetry.proto.collector.logs.v1.ExportLogsServiceResponse;
import io.opentelemetry.proto.collector.logs.v1.LogsServiceGrpc;
import jakarta.inject.Inject;
import org.graylog.grpc.auth.CallAuthorizer;
import org.graylog.plugins.otel.input.JournalRecordFactory;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.plugin.inputs.transports.ThrottleableTransport2;
import org.graylog2.plugin.journal.RawMessage;
import org.graylog2.shared.security.RestPermissions;

import static org.graylog.plugins.otel.input.grpc.Utils.createThrottledStatusRuntimeException;

public class LogsService extends LogsServiceGrpc.LogsServiceImplBase {
    private final JournalRecordFactory journalRecordFactory;
    private final ThrottleableTransport2 transport;
    private final MessageInput input;
    private final CallAuthorizer callAuthorizer;

    @Inject
    public LogsService(@Assisted ThrottleableTransport2 transport, @Assisted MessageInput input,
                       JournalRecordFactory journalRecordFactory, CallAuthorizer callAuthorizer) {
        this.transport = transport;
        this.input = input;
        this.journalRecordFactory = journalRecordFactory;
        this.callAuthorizer = callAuthorizer;
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

        // TODO: we can't use messages:read permission. add dedicated permission like e.g. messages:ingest
        if (!callAuthorizer.verifyPermitted(responseObserver, RestPermissions.MESSAGES_READ)) {
            return;
        }

        if (transport.isThrottled()) {
            responseObserver.onError(createThrottledStatusRuntimeException());
            return;
        }

        // TODO: get client IP and use RawMessage(byte[], java.net.InetSocketAddress) constructor
        journalRecordFactory.createFromRequest(request).forEach(record ->
                input.processRawMessage(new RawMessage(record.toByteArray())));

        responseObserver.onNext(ExportLogsServiceResponse.newBuilder().build());
        responseObserver.onCompleted();
    }

}

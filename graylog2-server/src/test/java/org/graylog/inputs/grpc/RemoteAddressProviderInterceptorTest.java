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

import io.grpc.Attributes;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.inprocess.InProcessSocketAddress;
import io.grpc.stub.StreamObserver;
import io.opentelemetry.proto.collector.logs.v1.ExportLogsServiceRequest;
import io.opentelemetry.proto.collector.logs.v1.ExportLogsServiceResponse;
import io.opentelemetry.proto.collector.logs.v1.LogsServiceGrpc;
import org.graylog.testing.grpc.InProcessGrpc;
import org.graylog.testing.grpc.InProcessGrpcExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.net.SocketAddress;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.graylog.inputs.grpc.RemoteAddressProviderInterceptor.REMOTE_ADDRESS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(InProcessGrpcExtension.class)
class RemoteAddressProviderInterceptorTest {
    @Test
    void providesRemoteAddressToServiceCall(InProcessGrpc grpc) throws IOException {
        final var remoteAddress = new AtomicReference<SocketAddress>();
        final var service = new LogsServiceGrpc.LogsServiceImplBase() {
            @Override
            public void export(ExportLogsServiceRequest request,
                               StreamObserver<ExportLogsServiceResponse> responseObserver) {
                // Unary service methods run in the listener's onHalfClose() callback, so this also verifies that the
                // context is propagated to listener callbacks, not just to startCall().
                remoteAddress.set(REMOTE_ADDRESS.get());
                responseObserver.onNext(ExportLogsServiceResponse.getDefaultInstance());
                responseObserver.onCompleted();
            }
        };

        LogsServiceGrpc.newBlockingStub(grpc.start(service, new RemoteAddressProviderInterceptor()))
                .export(ExportLogsServiceRequest.getDefaultInstance());

        assertThat(remoteAddress.get()).isInstanceOf(InProcessSocketAddress.class);
    }

    @Test
    void proceedsWithoutRemoteAddressIfTransportDoesNotProvideOne() {
        @SuppressWarnings("unchecked")
        final ServerCall<Object, Object> call = mock(ServerCall.class);
        when(call.getAttributes()).thenReturn(Attributes.EMPTY);

        final var handlerCalled = new AtomicBoolean();
        final var remoteAddress = new AtomicReference<SocketAddress>();

        new RemoteAddressProviderInterceptor().interceptCall(call, new Metadata(), (c, headers) -> {
            handlerCalled.set(true);
            remoteAddress.set(REMOTE_ADDRESS.get());
            return new ServerCall.Listener<>() {};
        });

        assertThat(handlerCalled).isTrue();
        assertThat(remoteAddress.get()).isNull();
    }
}

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

import io.grpc.ManagedChannel;
import io.grpc.Metadata;
import io.grpc.Status;
import io.grpc.stub.MetadataUtils;
import io.grpc.stub.StreamObserver;
import io.opentelemetry.proto.collector.logs.v1.ExportLogsServiceRequest;
import io.opentelemetry.proto.collector.logs.v1.ExportLogsServiceResponse;
import io.opentelemetry.proto.collector.logs.v1.LogsServiceGrpc;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.graylog.testing.grpc.InProcessGrpc;
import org.graylog.testing.grpc.InProcessGrpcExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.graylog.testing.grpc.GrpcAssertions.assertStatus;

@ExtendWith(InProcessGrpcExtension.class)
class BearerTokenAuthInterceptorTest {
    private static final String TOKEN = "s3cr3t";
    private static final Metadata.Key<String> AUTHORIZATION =
            Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER);

    private final AtomicInteger exportCalls = new AtomicInteger();
    private ManagedChannel channel;

    @BeforeEach
    void setUp(InProcessGrpc grpc) throws IOException {
        final var service = new LogsServiceGrpc.LogsServiceImplBase() {
            @Override
            public void export(ExportLogsServiceRequest request,
                               StreamObserver<ExportLogsServiceResponse> responseObserver) {
                exportCalls.incrementAndGet();
                responseObserver.onNext(ExportLogsServiceResponse.getDefaultInstance());
                responseObserver.onCompleted();
            }
        };
        channel = grpc.start(service, new BearerTokenAuthInterceptor(TOKEN));
    }

    @Test
    void acceptsValidBearerToken() {
        assertThat(exportWithAuthorization("Bearer " + TOKEN)).isEqualTo(ExportLogsServiceResponse.getDefaultInstance());
        assertThat(exportCalls).hasValue(1);
    }

    // HTTP/2 forbids leading and trailing whitespace in field values (RFC 9113, section 8.2.1), but not every
    // client or proxy enforces it. The interceptor strips the header value, so we test that here.
    @Test
    void acceptsValidBearerTokenWithSurroundingWhitespace() {
        exportWithAuthorization("  Bearer " + TOKEN + "  ");

        assertThat(exportCalls).hasValue(1);
    }

    @Test
    void rejectsCallWithoutAuthorizationHeader() {
        assertUnauthenticated(() -> LogsServiceGrpc.newBlockingStub(channel)
                .export(ExportLogsServiceRequest.getDefaultInstance()));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "",
            "   ",
            TOKEN,
            "Basic " + TOKEN,
            "Bearer",
            "Bearer ",
            "Bearer wrong",
            "Bearer " + TOKEN + "-suffix",
            "Bearer  " + TOKEN,
    })
    void rejectsInvalidAuthorizationHeader(String authorization) {
        assertUnauthenticated(() -> exportWithAuthorization(authorization));
    }

    private ExportLogsServiceResponse exportWithAuthorization(String authorization) {
        final var headers = new Metadata();
        headers.put(AUTHORIZATION, authorization);

        return LogsServiceGrpc.newBlockingStub(channel)
                .withInterceptors(MetadataUtils.newAttachHeadersInterceptor(headers))
                .export(ExportLogsServiceRequest.getDefaultInstance());
    }

    private void assertUnauthenticated(ThrowingCallable call) {
        // The description must be generic for every failure reason to not reveal any details to the client.
        assertStatus(call, Status.Code.UNAUTHENTICATED, "Authentication required");
        assertThat(exportCalls).hasValue(0);
    }
}

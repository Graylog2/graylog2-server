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
package org.graylog.collectors.opamp.transport;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.glassfish.grizzly.http.server.HttpServer;
import org.graylog.collectors.CollectorsConfigService;
import org.graylog.collectors.db.EnrollmentTokenCreator;
import org.graylog.collectors.db.EnrollmentTokenDTO;
import org.graylog.collectors.opamp.OpAmpConstants;
import org.graylog.collectors.opamp.OpAmpService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.ServerSocket;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OpAmpAuthCheckHttpHandlerIT {

    private static final EnrollmentTokenDTO TEST_TOKEN_DTO = new EnrollmentTokenDTO(
            "token-id",
            "test-token",
            "jti-1",
            "kid-1",
            "test-fleet",
            new EnrollmentTokenCreator("user-id", "admin"),
            Instant.now(),
            null,
            0,
            null
    );

    private HttpServer httpServer;
    private OpAmpService opAmpService;
    private OkHttpClient client;
    private int port;

    @BeforeEach
    void setUp() throws Exception {
        port = findFreePort();
        httpServer = HttpServer.createSimpleServer(null, port);

        opAmpService = mock(OpAmpService.class);
        final var collectorsConfigService = mock(CollectorsConfigService.class);
        final var executor = Executors.newVirtualThreadPerTaskExecutor();

        // Register both handlers like OpAmpHttpServerExtension does, to verify that the
        // auth-check mapping routes to its own handler next to the regular OpAMP endpoint.
        final var opAmpHandler = new OpAmpHttpHandler(opAmpService, collectorsConfigService, executor);
        final var authCheckHandler = new OpAmpAuthCheckHttpHandler(opAmpService, executor);
        httpServer.getServerConfiguration().addHttpHandler(opAmpHandler, OpAmpConstants.PATH);
        httpServer.getServerConfiguration().addHttpHandler(authCheckHandler, OpAmpConstants.AUTH_CHECK_PATH);

        httpServer.start();

        client = new OkHttpClient.Builder()
                .callTimeout(Duration.ofSeconds(10))
                .build();
    }

    @AfterEach
    void tearDown() {
        if (httpServer != null) {
            httpServer.shutdownNow();
        }
    }

    @Test
    void returnsOkForValidEnrollmentToken() throws Exception {
        when(opAmpService.authenticate(eq("Bearer valid"), eq(OpAmpAuthContext.Transport.HTTP)))
                .thenReturn(Optional.of(new OpAmpAuthContext.Enrollment(TEST_TOKEN_DTO, OpAmpAuthContext.Transport.HTTP)));

        try (var response = client.newCall(authCheckRequest("Bearer valid")).execute()) {
            assertThat(response.code()).isEqualTo(200);
        }

        // The auth-check must not cause any state changes.
        verify(opAmpService, never()).handleMessage(any(), any());
    }

    @Test
    void returnsOkForValidAgentToken() throws Exception {
        when(opAmpService.authenticate(eq("Bearer valid"), eq(OpAmpAuthContext.Transport.HTTP)))
                .thenReturn(Optional.of(new OpAmpAuthContext.Identified(UUID.randomUUID().toString(), OpAmpAuthContext.Transport.HTTP)));

        try (var response = client.newCall(authCheckRequest("Bearer valid")).execute()) {
            assertThat(response.code()).isEqualTo(200);
        }

        verify(opAmpService, never()).handleMessage(any(), any());
    }

    @Test
    void rejectsMissingAuth() throws Exception {
        when(opAmpService.authenticate(any(), any())).thenReturn(Optional.empty());

        final var request = new Request.Builder()
                .url(authCheckUrl())
                .get()
                .build();

        try (var response = client.newCall(request).execute()) {
            assertThat(response.code()).isEqualTo(401);
        }
    }

    @Test
    void rejectsInvalidAuth() throws Exception {
        when(opAmpService.authenticate(eq("Bearer invalid"), any())).thenReturn(Optional.empty());

        try (var response = client.newCall(authCheckRequest("Bearer invalid")).execute()) {
            assertThat(response.code()).isEqualTo(401);
        }
    }

    @Test
    void rejectsPostRequest() throws Exception {
        final var request = new Request.Builder()
                .url(authCheckUrl())
                .post(RequestBody.create(new byte[1], MediaType.parse("application/x-protobuf")))
                .header("Authorization", "Bearer valid")
                .build();

        try (var response = client.newCall(request).execute()) {
            assertThat(response.code()).isEqualTo(405);
        }

        verify(opAmpService, never()).authenticate(any(), any());
    }

    private Request authCheckRequest(String authorization) {
        return new Request.Builder()
                .url(authCheckUrl())
                .header("Authorization", authorization)
                .get()
                .build();
    }

    private String authCheckUrl() {
        return "http://localhost:" + port + OpAmpConstants.AUTH_CHECK_PATH;
    }

    private static int findFreePort() throws IOException {
        try (var socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }
}

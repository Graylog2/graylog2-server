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
package org.graylog2.opamp.transport;

import com.github.joschi.jadconfig.util.Size;
import com.google.protobuf.ByteString;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import opamp.proto.Opamp.AgentToServer;
import opamp.proto.Opamp.ServerErrorResponse;
import opamp.proto.Opamp.ServerErrorResponseType;
import opamp.proto.Opamp.ServerToAgent;
import org.glassfish.grizzly.http.server.HttpServer;
import org.graylog2.opamp.OpAmpService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.ServerSocket;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OpAmpHttpHandlerIT {

    private static final MediaType PROTOBUF = MediaType.parse("application/x-protobuf");
    private static final int TEST_MAX_MESSAGE_SIZE = 1024; // 1 KB for tests

    private HttpServer httpServer;
    private OpAmpService opAmpService;
    private OkHttpClient client;
    private int port;

    @BeforeEach
    void setUp() throws Exception {
        port = findFreePort();
        httpServer = HttpServer.createSimpleServer(null, port);

        opAmpService = mock(OpAmpService.class);
        final OpAmpHttpHandler handler = new OpAmpHttpHandler(opAmpService, Size.bytes(TEST_MAX_MESSAGE_SIZE));
        httpServer.getServerConfiguration().addHttpHandler(handler, "/opamp");

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
    void rejectsGetRequest() throws Exception {
        final Request request = new Request.Builder()
                .url(opampUrl())
                .get()
                .build();

        try (Response response = client.newCall(request).execute()) {
            assertThat(response.code()).isEqualTo(405);
        }
    }

    @Test
    void rejectsMissingAuth() throws Exception {
        when(opAmpService.validateToken(null)).thenReturn(false);

        final Request request = new Request.Builder()
                .url(opampUrl())
                .post(RequestBody.create(new byte[1], PROTOBUF))
                .build();

        try (Response response = client.newCall(request).execute()) {
            assertThat(response.code()).isEqualTo(401);
        }
    }

    @Test
    void rejectsInvalidAuth() throws Exception {
        when(opAmpService.validateToken("Bearer invalid")).thenReturn(false);

        final Request request = new Request.Builder()
                .url(opampUrl())
                .post(RequestBody.create(new byte[1], PROTOBUF))
                .header("Authorization", "Bearer invalid")
                .build();

        try (Response response = client.newCall(request).execute()) {
            assertThat(response.code()).isEqualTo(401);
        }
    }

    @Test
    void rejectsEmptyBody() throws Exception {
        when(opAmpService.validateToken("Bearer valid")).thenReturn(true);

        final Request request = new Request.Builder()
                .url(opampUrl())
                .post(RequestBody.create(new byte[0], PROTOBUF))
                .header("Authorization", "Bearer valid")
                .build();

        try (Response response = client.newCall(request).execute()) {
            assertThat(response.code()).isEqualTo(400);
            assertThat(response.body().string()).isEqualTo("Empty request body");
        }
    }

    @Test
    void rejectsInvalidProtobuf() throws Exception {
        when(opAmpService.validateToken("Bearer valid")).thenReturn(true);

        final Request request = new Request.Builder()
                .url(opampUrl())
                .post(RequestBody.create(new byte[]{0x01, 0x02, 0x03}, PROTOBUF))
                .header("Authorization", "Bearer valid")
                .build();

        try (Response response = client.newCall(request).execute()) {
            assertThat(response.code()).isEqualTo(400);
            assertThat(response.body().string()).contains("Protocol message");
        }
    }

    @Test
    void processesValidMessage() throws Exception {
        when(opAmpService.validateToken("Bearer valid")).thenReturn(true);

        final AgentToServer agentMsg = AgentToServer.newBuilder()
                .setInstanceUid(ByteString.copyFromUtf8("test-instance-uid"))
                .setSequenceNum(1)
                .build();

        final ServerToAgent expectedReply = ServerToAgent.newBuilder()
                .setInstanceUid(agentMsg.getInstanceUid())
                .build();

        when(opAmpService.handleMessage(agentMsg)).thenReturn(expectedReply);

        final Request request = new Request.Builder()
                .url(opampUrl())
                .post(RequestBody.create(agentMsg.toByteArray(), PROTOBUF))
                .header("Authorization", "Bearer valid")
                .build();

        try (Response response = client.newCall(request).execute()) {
            assertThat(response.code()).isEqualTo(200);
            assertThat(response.header("Content-Type")).isEqualTo("application/x-protobuf");

            final ServerToAgent reply = ServerToAgent.parseFrom(response.body().bytes());
            assertThat(reply.getInstanceUid()).isEqualTo(expectedReply.getInstanceUid());
        }
    }

    @Test
    void returnsServerErrorResponseOnServiceException() throws Exception {
        when(opAmpService.validateToken("Bearer valid")).thenReturn(true);

        final AgentToServer agentMsg = AgentToServer.newBuilder()
                .setInstanceUid(ByteString.copyFromUtf8("test-instance-uid"))
                .build();

        when(opAmpService.handleMessage(any())).thenThrow(new RuntimeException("Database unavailable"));

        final Request request = new Request.Builder()
                .url(opampUrl())
                .post(RequestBody.create(agentMsg.toByteArray(), PROTOBUF))
                .header("Authorization", "Bearer valid")
                .build();

        try (Response response = client.newCall(request).execute()) {
            assertThat(response.code()).isEqualTo(200);
            assertThat(response.header("Content-Type")).isEqualTo("application/x-protobuf");

            final ServerToAgent reply = ServerToAgent.parseFrom(response.body().bytes());
            assertThat(reply.getInstanceUid()).isEqualTo(agentMsg.getInstanceUid());
            assertThat(reply.hasErrorResponse()).isTrue();

            final ServerErrorResponse error = reply.getErrorResponse();
            assertThat(error.getType()).isEqualTo(ServerErrorResponseType.ServerErrorResponseType_Unknown);
            assertThat(error.getErrorMessage()).isEqualTo("Database unavailable");
        }
    }

    @Test
    void rejectsOversizedBody() throws Exception {
        when(opAmpService.validateToken("Bearer valid")).thenReturn(true);

        // Create a body larger than TEST_MAX_MESSAGE_SIZE (1 KB)
        final byte[] oversizedBody = new byte[TEST_MAX_MESSAGE_SIZE + 100];

        final Request request = new Request.Builder()
                .url(opampUrl())
                .post(RequestBody.create(oversizedBody, PROTOBUF))
                .header("Authorization", "Bearer valid")
                .build();

        try (Response response = client.newCall(request).execute()) {
            assertThat(response.code()).isEqualTo(413);
            assertThat(response.body().string()).contains("exceeds maximum");
        }
    }

    private String opampUrl() {
        return "http://localhost:" + port + "/opamp";
    }

    private static int findFreePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }
}

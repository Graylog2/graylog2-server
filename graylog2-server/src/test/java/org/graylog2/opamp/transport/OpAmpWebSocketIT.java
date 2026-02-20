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

import com.google.protobuf.ByteString;
import opamp.proto.Opamp.AgentToServer;
import opamp.proto.Opamp.ServerToAgent;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.websockets.WebSocketEngine;
import org.graylog2.opamp.OpAmpService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OpAmpWebSocketIT {

    private HttpServer httpServer;
    private OpAmpService opAmpService;
    private HttpClient httpClient;
    private int port;

    @BeforeEach
    void setUp() throws Exception {
        // Clean up any previous registrations (WebSocketEngine is singleton)
        WebSocketEngine.getEngine().unregisterAll();

        port = findFreePort();
        httpServer = HttpServer.createSimpleServer(null, port);

        final var listener = httpServer.getListener("grizzly");

        opAmpService = mock(OpAmpService.class);
        final var executor = Executors.newVirtualThreadPerTaskExecutor();

        // Enable WebSocket addon with auth filter (matches production setup)
        listener.registerAddOn(new OpAmpAddOn(new OpAmpWebSocketAuthFilter(opAmpService, executor)));

        final var wsApp = new OpAmpWebSocketApplication(opAmpService, executor);

        // Register WebSocket application (auth handled in createSocket)
        WebSocketEngine.getEngine().register("", "/v1/opamp", wsApp);

        httpServer.start();

        httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    @AfterEach
    void tearDown() {
        WebSocketEngine.getEngine().unregisterAll();
        if (httpServer != null) {
            httpServer.shutdownNow();
        }
    }

    @Test
    void rejectsConnectionWithInvalidAuth() {
        when(opAmpService.authenticate(eq("Bearer invalid"), any())).thenReturn(Optional.empty());

        final var listener = new TestWebSocketListener(new CompletableFuture<>());

        // When auth fails, WebSocket upgrade should fail
        // Java HttpClient should throw during handshake
        assertThatThrownBy(() -> httpClient.newWebSocketBuilder()
                .header("Authorization", "Bearer invalid")
                .buildAsync(wsUri(), listener)
                .join())
                .isInstanceOf(Exception.class);
    }

    @Test
    void rejectsConnectionWithMissingAuth() {
        when(opAmpService.authenticate(any(), any())).thenReturn(Optional.empty());

        final var listener = new TestWebSocketListener(new CompletableFuture<>());

        // When auth fails, WebSocket upgrade should fail
        assertThatThrownBy(() -> httpClient.newWebSocketBuilder()
                .buildAsync(wsUri(), listener)
                .join())
                .isInstanceOf(Exception.class);
    }

    @Test
    void acceptsConnectionWithValidAuth() throws Exception {
        when(opAmpService.authenticate(eq("Bearer valid"), any())).thenReturn(Optional.of(new OpAmpAuthContext.Enrollment("test-fleet", OpAmpAuthContext.Transport.WEBSOCKET)));
        when(opAmpService.handleMessage(eq(AgentToServer.getDefaultInstance()), any()))
                .thenReturn(ServerToAgent.getDefaultInstance());

        final var closeFuture = new CompletableFuture<Integer>();
        final var listener = new TestWebSocketListener(closeFuture);

        final var ws = httpClient.newWebSocketBuilder()
                .header("Authorization", "Bearer valid")
                .buildAsync(wsUri(), listener)
                .get(5, TimeUnit.SECONDS);

        // Send probe and wait for response - proves auth passed
        ws.sendBinary(ByteBuffer.wrap(probeMessage()), true).join();
        assertThat(listener.messages.poll(2, TimeUnit.SECONDS)).isNotNull();

        // Connection should remain open
        assertThat(ws.isInputClosed()).isFalse();
        assertThat(ws.isOutputClosed()).isFalse();

        ws.sendClose(WebSocket.NORMAL_CLOSURE, "done").join();
    }

    @Test
    void processesValidMessage() throws Exception {
        when(opAmpService.authenticate(eq("Bearer valid"), any())).thenReturn(Optional.of(new OpAmpAuthContext.Enrollment("test-fleet", OpAmpAuthContext.Transport.WEBSOCKET)));

        final var agentMsg = AgentToServer.newBuilder()
                .setInstanceUid(ByteString.copyFromUtf8("test-instance-uid"))
                .setSequenceNum(1)
                .build();

        final var expectedReply = ServerToAgent.newBuilder()
                .setInstanceUid(agentMsg.getInstanceUid())
                .build();

        when(opAmpService.handleMessage(eq(AgentToServer.getDefaultInstance()), any()))
                .thenReturn(ServerToAgent.getDefaultInstance());
        when(opAmpService.handleMessage(eq(agentMsg), any())).thenReturn(expectedReply);

        final var closeFuture = new CompletableFuture<Integer>();
        final var listener = new TestWebSocketListener(closeFuture);

        final var ws = httpClient.newWebSocketBuilder()
                .header("Authorization", "Bearer valid")
                .buildAsync(wsUri(), listener)
                .get(5, TimeUnit.SECONDS);

        // Send probe and wait - proves auth passed
        ws.sendBinary(ByteBuffer.wrap(probeMessage()), true).join();
        listener.messages.poll(2, TimeUnit.SECONDS);  // discard probe response

        // Send real message
        ws.sendBinary(ByteBuffer.wrap(frameMessage(agentMsg)), true).join();

        // Wait for response
        final var responseBytes = listener.messages.poll(2, TimeUnit.SECONDS);
        final var reply = parseFramedResponse(Objects.requireNonNull(responseBytes));

        assertThat(reply.getInstanceUid()).isEqualTo(expectedReply.getInstanceUid());

        ws.sendClose(WebSocket.NORMAL_CLOSURE, "done").join();
    }

    @Test
    void handlesInvalidProtobufGracefully() throws Exception {
        when(opAmpService.authenticate(eq("Bearer valid"), any())).thenReturn(Optional.of(new OpAmpAuthContext.Enrollment("test-fleet", OpAmpAuthContext.Transport.WEBSOCKET)));
        when(opAmpService.handleMessage(eq(AgentToServer.getDefaultInstance()), any()))
                .thenReturn(ServerToAgent.getDefaultInstance());

        final var closeFuture = new CompletableFuture<Integer>();
        final var listener = new TestWebSocketListener(closeFuture);

        final var ws = httpClient.newWebSocketBuilder()
                .header("Authorization", "Bearer valid")
                .buildAsync(wsUri(), listener)
                .get(5, TimeUnit.SECONDS);

        // Send valid header + invalid protobuf (server logs error but keeps connection)
        ws.sendBinary(ByteBuffer.wrap(new byte[]{0x00, 0x01, 0x02, 0x03}), true).join();

        // Send probe and wait - proves connection still works after invalid message
        ws.sendBinary(ByteBuffer.wrap(probeMessage()), true).join();
        assertThat(listener.messages.poll(2, TimeUnit.SECONDS)).isNotNull();

        // Connection should remain open (graceful handling)
        assertThat(ws.isOutputClosed()).isFalse();

        ws.sendClose(WebSocket.NORMAL_CLOSURE, "done").join();
    }

    @Test
    void rejectsMessageWithInvalidHeader() throws Exception {
        when(opAmpService.authenticate(eq("Bearer valid"), any())).thenReturn(Optional.of(new OpAmpAuthContext.Enrollment("test-fleet", OpAmpAuthContext.Transport.WEBSOCKET)));
        when(opAmpService.handleMessage(eq(AgentToServer.getDefaultInstance()), any()))
                .thenReturn(ServerToAgent.getDefaultInstance());

        final var closeFuture = new CompletableFuture<Integer>();
        final var listener = new TestWebSocketListener(closeFuture);

        final var ws = httpClient.newWebSocketBuilder()
                .header("Authorization", "Bearer valid")
                .buildAsync(wsUri(), listener)
                .get(5, TimeUnit.SECONDS);

        // Send probe and wait - proves auth passed
        ws.sendBinary(ByteBuffer.wrap(probeMessage()), true).join();
        listener.messages.poll(2, TimeUnit.SECONDS);  // discard probe response

        // Send message with invalid header (1 instead of 0)
        final var invalidMessage = AgentToServer.newBuilder()
                .setInstanceUid(ByteString.copyFromUtf8("test"))
                .build();
        var out = new ByteArrayOutputStream();
        out.write(1);  // invalid header
        invalidMessage.writeTo(out);

        ws.sendBinary(ByteBuffer.wrap(out.toByteArray()), true).join();

        // Server should close the connection
        final var closeCode = closeFuture.get(5, TimeUnit.SECONDS);
        assertThat(closeCode).isNotNull();
    }

    private URI wsUri() {
        return URI.create("ws://localhost:" + port + "/v1/opamp");
    }

    private static int findFreePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }

    private static byte[] frameMessage(AgentToServer message) throws IOException {
        var out = new ByteArrayOutputStream();
        out.write(0);  // header
        message.writeTo(out);
        return out.toByteArray();
    }

    private static byte[] probeMessage() throws IOException {
        var out = new ByteArrayOutputStream();
        out.write(0);  // header
        AgentToServer.getDefaultInstance().writeTo(out);
        return out.toByteArray();
    }

    private static ServerToAgent parseFramedResponse(byte[] data) throws Exception {
        assertThat(data.length).isGreaterThan(0);
        assertThat(data[0]).isEqualTo((byte) 0);  // verify header
        byte[] protobufData = new byte[data.length - 1];
        System.arraycopy(data, 1, protobufData, 0, protobufData.length);
        return ServerToAgent.parseFrom(protobufData);
    }

    private static class TestWebSocketListener implements WebSocket.Listener {
        private final CompletableFuture<Integer> closeFuture;
        private final BlockingQueue<byte[]> messages = new LinkedBlockingQueue<>();
        private ByteBuffer messageBuffer = ByteBuffer.allocate(0);

        TestWebSocketListener(CompletableFuture<Integer> closeFuture) {
            this.closeFuture = closeFuture;
        }

        @Override
        public CompletionStage<?> onBinary(WebSocket webSocket, ByteBuffer data, boolean last) {
            // Accumulate data
            final var newBuffer = ByteBuffer.allocate(messageBuffer.remaining() + data.remaining());
            newBuffer.put(messageBuffer);
            newBuffer.put(data);
            newBuffer.flip();
            messageBuffer = newBuffer;

            if (last) {
                final var bytes = new byte[messageBuffer.remaining()];
                messageBuffer.get(bytes);
                messages.add(bytes);
                messageBuffer = ByteBuffer.allocate(0);
            }

            webSocket.request(1);
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
            closeFuture.complete(statusCode);
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            closeFuture.completeExceptionally(error);
        }
    }
}

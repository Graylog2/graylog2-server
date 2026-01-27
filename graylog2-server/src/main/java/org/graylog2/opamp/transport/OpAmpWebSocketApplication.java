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

import com.google.protobuf.CodedInputStream;
import com.google.protobuf.InvalidProtocolBufferException;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import opamp.proto.Opamp.AgentToServer;
import org.glassfish.grizzly.http.HttpRequestPacket;
import org.glassfish.grizzly.websockets.DataFrame;
import org.glassfish.grizzly.websockets.ProtocolHandler;
import org.glassfish.grizzly.websockets.SimpleWebSocket;
import org.glassfish.grizzly.websockets.WebSocket;
import org.glassfish.grizzly.websockets.WebSocketApplication;
import org.glassfish.grizzly.websockets.WebSocketListener;
import org.graylog2.opamp.OpAmpExecutor;
import org.graylog2.opamp.OpAmpService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.util.concurrent.ExecutorService;

import static org.graylog2.shared.utilities.StringUtils.f;

@Singleton
public class OpAmpWebSocketApplication extends WebSocketApplication {
    private static final Logger LOG = LoggerFactory.getLogger(OpAmpWebSocketApplication.class);

    private final OpAmpService opAmpService;
    private final ExecutorService executor;

    @Inject
    public OpAmpWebSocketApplication(OpAmpService opAmpService, @OpAmpExecutor ExecutorService executor) {
        this.opAmpService = opAmpService;
        this.executor = executor;
    }

    @Override
    public WebSocket createSocket(ProtocolHandler handler,
                                  HttpRequestPacket request,
                                  WebSocketListener... listeners) {

        final var authContext = OpAmpAuthContext.fromRequest(request)
                .orElseThrow(() -> new IllegalStateException(
                        f("OpAMP auth context missing - is {} configured?",
                                OpAmpWebSocketAuthFilter.class.getSimpleName())));

        return new OpAmpWebSocket(handler, listeners, authContext);
    }

    @Override
    public void onConnect(WebSocket socket) {
        LOG.debug("OpAMP WebSocket connected");
    }


    @Override
    public void onMessage(WebSocket socket, byte[] data) {
        final OpAmpWebSocket opAmpSocket = (OpAmpWebSocket) socket;
        final OpAmpAuthContext authContext = opAmpSocket.authContext();

        executor.submit(() -> {
            try {
                if (!authContext.authenticated()) {
                    LOG.warn("Received message on unauthenticated socket");
                    socket.close();
                    return;
                }

                // Read OpAMP header (must be 0 per spec)
                final var input = CodedInputStream.newInstance(data);
                final long header = input.readUInt64();
                if (header != 0) {
                    LOG.warn("Invalid OpAMP WebSocket header: {}, expected 0", header);
                    socket.close();
                    return;
                }

                final var message = AgentToServer.parseFrom(input);
                final var reply = opAmpService.handleMessage(message);

                // Encode response with header
                final var out = new ByteArrayOutputStream();
                out.write(0);  // header
                reply.writeTo(out);
                socket.send(out.toByteArray());
            } catch (InvalidProtocolBufferException e) {
                LOG.warn("Invalid protobuf in WebSocket message", e);
            } catch (Exception e) {
                LOG.error("Error processing WebSocket message", e);
            }
        });
    }

    @Override
    public void onClose(WebSocket socket, DataFrame frame) {
        LOG.debug("OpAMP WebSocket closed");
    }

    /**
     * Custom WebSocket subclass that carries auth context from createSocket to onMessage.
     * Extends SimpleWebSocket (no servlet dependencies) rather than DefaultWebSocket.
     */
    private static class OpAmpWebSocket extends SimpleWebSocket {
        private final OpAmpAuthContext authContext;

        OpAmpWebSocket(ProtocolHandler handler, WebSocketListener[] listeners, OpAmpAuthContext authContext) {
            super(handler, listeners);
            this.authContext = authContext;
        }

        OpAmpAuthContext authContext() {
            return authContext;
        }
    }
}

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
import com.google.protobuf.InvalidProtocolBufferException;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import opamp.proto.Opamp.AgentToServer;
import opamp.proto.Opamp.ServerErrorResponse;
import opamp.proto.Opamp.ServerErrorResponseType;
import opamp.proto.Opamp.ServerToAgent;
import org.glassfish.grizzly.http.Method;
import org.glassfish.grizzly.http.server.HttpHandler;
import org.glassfish.grizzly.http.server.Request;
import org.glassfish.grizzly.http.server.Response;
import org.glassfish.grizzly.http.util.HttpStatus;
import org.graylog2.opamp.OpAmpService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.graylog2.shared.utilities.StringUtils.f;

@Singleton
public class OpAmpHttpHandler extends HttpHandler {
    private static final Logger LOG = LoggerFactory.getLogger(OpAmpHttpHandler.class);
    private static final String CONTENT_TYPE_PROTOBUF = "application/x-protobuf";

    private final OpAmpService opAmpService;
    private final ExecutorService executor;
    private final int maxMessageSize;

    @Inject
    public OpAmpHttpHandler(OpAmpService opAmpService,
                            @Named("opamp_max_request_body_size") Size maxRequestBodySize) {
        this.opAmpService = opAmpService;
        this.maxMessageSize = (int) maxRequestBodySize.toBytes();
        this.executor = Executors.newVirtualThreadPerTaskExecutor();
    }

    @Override
    public void service(Request request, Response response) throws Exception {
        // Only accept POST
        if (request.getMethod() != Method.POST) {
            response.setStatus(HttpStatus.METHOD_NOT_ALLOWED_405);
            response.finish();
            return;
        }

        // Validate auth
        final var authHeader = request.getHeader("Authorization");
        if (!opAmpService.validateToken(authHeader)) {
            response.setStatus(HttpStatus.UNAUTHORIZED_401);
            response.finish();
            return;
        }

        // Suspend response and dispatch to virtual thread
        response.suspend();

        executor.submit(() -> {
            try {
                processRequest(request, response);
            } finally {
                response.resume();
            }
        });
    }

    private void processRequest(Request request, Response response) {
        // Check Content-Length header first (if present)
        final int contentLength = request.getContentLength();
        if (contentLength > maxMessageSize) {
            LOG.warn("OpAMP request Content-Length {} exceeds maximum {}", contentLength, maxMessageSize);
            sendError(response, HttpStatus.REQUEST_ENTITY_TOO_LARGE_413,
                    f("Message size exceeds maximum of %d bytes", maxMessageSize));
            return;
        }

        // Read body with limit as safety net (Content-Length can be spoofed or absent)
        final byte[] body;
        try {
            body = request.getInputStream().readNBytes(maxMessageSize + 1);
        } catch (IOException e) {
            LOG.warn("Failed to read OpAMP request body", e);
            sendError(response, HttpStatus.INTERNAL_SERVER_ERROR_500, e.getMessage());
            return;
        }

        if (body.length == 0) {
            sendError(response, HttpStatus.BAD_REQUEST_400, "Empty request body");
            return;
        }

        if (body.length > maxMessageSize) {
            LOG.warn("OpAMP request body size {} exceeds maximum {}", body.length, maxMessageSize);
            sendError(response, HttpStatus.REQUEST_ENTITY_TOO_LARGE_413,
                    f("Message size exceeds maximum of %d bytes", maxMessageSize));
            return;
        }

        final AgentToServer message;
        try {
            message = AgentToServer.parseFrom(body);
        } catch (InvalidProtocolBufferException e) {
            LOG.warn("Invalid protobuf in OpAMP request", e);
            sendError(response, HttpStatus.BAD_REQUEST_400, e.getMessage());
            return;
        }

        ServerToAgent reply;
        try {
            reply = opAmpService.handleMessage(message);
        } catch (RuntimeException e) {
            LOG.error("Error processing OpAMP message: {}", message, e);
            reply = ServerToAgent.newBuilder()
                    .setInstanceUid(message.getInstanceUid())
                    .setErrorResponse(ServerErrorResponse.newBuilder()
                            .setType(ServerErrorResponseType.ServerErrorResponseType_Unknown)
                            .setErrorMessage(e.getMessage())
                            .build())
                    .build();
        }

        response.setContentType(CONTENT_TYPE_PROTOBUF);
        try {
            response.getOutputStream().write(reply.toByteArray());
            response.setStatus(200);
        } catch (IOException e) {
            LOG.error("Failed to write OpAMP response", e);
            sendError(response, HttpStatus.INTERNAL_SERVER_ERROR_500, e.getMessage());
        }
    }

    private void sendError(Response response, HttpStatus status, String message) {
        response.setStatus(status);
        response.setContentType("text/plain; charset=utf-8");
        try {
            response.getOutputStream().write(message.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            LOG.warn("Failed to write error response", e);
        }
    }
}

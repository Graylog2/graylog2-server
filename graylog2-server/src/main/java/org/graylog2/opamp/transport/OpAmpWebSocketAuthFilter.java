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

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.glassfish.grizzly.filterchain.BaseFilter;
import org.glassfish.grizzly.filterchain.FilterChainContext;
import org.glassfish.grizzly.filterchain.NextAction;
import org.glassfish.grizzly.http.HttpContent;
import org.glassfish.grizzly.http.HttpRequestPacket;
import org.glassfish.grizzly.http.HttpResponsePacket;
import org.glassfish.grizzly.http.util.HttpStatus;
import org.graylog2.opamp.OpAmpConstants;
import org.graylog2.opamp.OpAmpExecutor;
import org.graylog2.opamp.OpAmpService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;

@Singleton
public class OpAmpWebSocketAuthFilter extends BaseFilter {
    private static final Logger LOG = LoggerFactory.getLogger(OpAmpWebSocketAuthFilter.class);

    private final OpAmpService opAmpService;
    private final ExecutorService executor;

    @Inject
    public OpAmpWebSocketAuthFilter(OpAmpService opAmpService,
                                    @OpAmpExecutor ExecutorService executor) {
        this.opAmpService = opAmpService;
        this.executor = executor;
    }

    @Override
    public NextAction handleRead(FilterChainContext ctx) {
        if (!(ctx.getMessage() instanceof HttpContent httpContent)) {
            return ctx.getInvokeAction();
        }

        final HttpRequestPacket request = (HttpRequestPacket) httpContent.getHttpHeader();
        if (!OpAmpConstants.PATH.equals(request.getRequestURI())) {
            return ctx.getInvokeAction();
        }

        if (!request.isUpgrade()) {
            return ctx.getInvokeAction();
        }

        final String authHeader = request.getHeader("Authorization");

        ctx.suspend();
        executor.submit(() -> {
            try {
                final var authContext = opAmpService.authenticate(authHeader, OpAmpAuthContext.Transport.WEBSOCKET);
                if (authContext.isEmpty()) {
                    LOG.debug("OpAMP auth failed");
                    send401(ctx, request);
                } else {
                    request.setAttribute(OpAmpAuthContext.REQUEST_ATTRIBUTE, authContext.get());
                    ctx.resume(ctx.getInvokeAction());
                }
            } catch (Exception e) {
                LOG.warn("OpAMP auth error", e);
                send401(ctx, request);
            }
        });

        return ctx.getSuspendAction();
    }

    private void send401(FilterChainContext ctx, HttpRequestPacket request) {
        try {
            final HttpResponsePacket response = request.getResponse();
            response.setStatus(HttpStatus.UNAUTHORIZED_401);
            response.setContentLength(0);
            ctx.write(response);
        } finally {
            ctx.completeAndRecycle();
        }
    }
}

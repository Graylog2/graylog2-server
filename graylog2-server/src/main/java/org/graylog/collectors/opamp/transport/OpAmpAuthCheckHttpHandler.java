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

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.glassfish.grizzly.http.Method;
import org.glassfish.grizzly.http.server.HttpHandler;
import org.glassfish.grizzly.http.server.Request;
import org.glassfish.grizzly.http.server.Response;
import org.glassfish.grizzly.http.util.HttpStatus;
import org.graylog.collectors.opamp.OpAmpExecutor;
import org.graylog.collectors.opamp.OpAmpService;

import java.util.concurrent.ExecutorService;

@Singleton
public class OpAmpAuthCheckHttpHandler extends HttpHandler {
    private final OpAmpService opAmpService;
    private final ExecutorService executor;

    @Inject
    public OpAmpAuthCheckHttpHandler(OpAmpService opAmpService,
                                     @OpAmpExecutor ExecutorService executor) {
        this.opAmpService = opAmpService;
        this.executor = executor;
    }

    @Override
    public void service(Request request, Response response) throws Exception {
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
        if (request.getMethod() != Method.GET) {
            response.setStatus(HttpStatus.METHOD_NOT_ALLOWED_405);
            response.finish();
            return;
        }

        final var authContext = opAmpService.authenticate(request.getHeader("Authorization"), OpAmpAuthContext.Transport.HTTP);
        if (authContext.isEmpty()) {
            response.setStatus(HttpStatus.UNAUTHORIZED_401);
            response.finish();
            return;
        }

        response.setStatus(HttpStatus.OK_200);
        response.finish();
    }
}

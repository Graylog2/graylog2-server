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
package org.graylog.collectors.opamp.http;

import jakarta.inject.Inject;
import org.glassfish.grizzly.http.server.HttpServer;
import org.graylog.collectors.opamp.OpAmpConstants;
import org.graylog.collectors.opamp.transport.OpAmpHttpHandler;
import org.graylog2.jersey.HttpServerExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.Objects.requireNonNull;

public class OpAmpHttpServerExtension implements HttpServerExtension {
    public static final String TYPE = "opamp";

    private static final Logger LOG = LoggerFactory.getLogger(OpAmpHttpServerExtension.class);

    private final OpAmpHttpHandler opAmpHttpHandler;

    @Inject
    public OpAmpHttpServerExtension(OpAmpHttpHandler opAmpHttpHandler) {
        this.opAmpHttpHandler = requireNonNull(opAmpHttpHandler, "opAmpHttpHandler");
    }

    /**
     * Configure OpAMP endpoint at /v1/opamp for HTTP transport.
     */
    public void configure(HttpServer httpServer) {
        httpServer.getServerConfiguration().addHttpHandler(opAmpHttpHandler, OpAmpConstants.PATH);

        LOG.info("OpAMP endpoint enabled at {}", OpAmpConstants.PATH);
    }
}

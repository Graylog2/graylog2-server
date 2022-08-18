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
package org.graylog.metrics.prometheus;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.net.HostAndPort;
import com.google.inject.assistedinject.Assisted;
import com.sun.net.httpserver.HttpServer;
import io.prometheus.client.Collector;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.exporter.HTTPServer;
import org.graylog2.shared.SuppressForbidden;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class PrometheusExporterHTTPServer {
    private static final Logger LOG = LoggerFactory.getLogger(PrometheusExporterHTTPServer.class);

    private static final int DEFAULT_HTTP_SERVER_BACKLOG = 3;

    private final HostAndPort bindAddress;
    private final AtomicReference<CollectorRegistry> registryRef = new AtomicReference<>(newCollectorRegistry());
    private final AtomicBoolean isRunning = new AtomicBoolean(false);

    private HTTPServer server;

    public interface Factory {
        PrometheusExporterHTTPServer create(HostAndPort bindAddress);
    }

    @Inject
    public PrometheusExporterHTTPServer(@Assisted HostAndPort bindAddress) {
        this.bindAddress = bindAddress;
    }

    private static CollectorRegistry newCollectorRegistry() {
        return new CollectorRegistry(true);
    }

    public void replaceCollector(Collector collector) {
        final CollectorRegistry newRegistry = newCollectorRegistry();
        newRegistry.register(collector);

        registryRef.set(newRegistry);
    }

    @SuppressForbidden("Deliberate usage of HttpServer")
    public void start() {
        try {
            final InetSocketAddress addr = new InetSocketAddress(bindAddress.getHost(), bindAddress.getPort());
            final HttpServer httpServer = HttpServer.create(addr, DEFAULT_HTTP_SERVER_BACKLOG);
            final ReplaceableCollectorRegistry replaceableRegistry = new ReplaceableCollectorRegistry(registryRef);

            this.server = new HTTPServer(httpServer, replaceableRegistry, true);
            isRunning.set(true);
            LOG.info("Exporting Prometheus metrics on <{}> via HTTP", bindAddress);
        } catch (IOException e) {
            LOG.error("Couldn't start Prometheus HTTP exporter", e);
        }
    }

    public void stop() {
        if (server != null) {
            isRunning.set(false);
            server.stop();
        }
    }

    @VisibleForTesting
    Optional<Integer> getPort() {
        return Optional.ofNullable(server).map(HTTPServer::getPort);
    }
}

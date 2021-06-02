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

import com.codahale.metrics.Counter;
import com.codahale.metrics.MetricRegistry;
import com.google.common.net.HostAndPort;
import io.prometheus.client.dropwizard.DropwizardExports;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PrometheusExporterHTTPServerTest {
    PrometheusExporterHTTPServer server;

    @BeforeEach
    void setUp() {
        this.server = new PrometheusExporterHTTPServer(HostAndPort.fromParts("127.0.0.1", 0));

        server.start();
    }

    @AfterEach
    void tearDown() {
        server.stop();
    }

    @Test
    void runWithEmptyCollector() throws Exception {
        doGET(server, "/metrics", response -> {
            assertThat(response.body()).isNotNull();
            assertThat(response.body().string()).isEmpty();
        });
        doGET(server, "/", response -> {
            assertThat(response.body()).isNotNull();
            assertThat(response.body().string()).isEmpty();
        });
        doGET(server, "/-/healthy", response -> {
            assertThat(response.body()).isNotNull();
            assertThat(response.body().string()).containsIgnoringCase("exporter is healthy");
        });
    }

    @Test
    void runWithPopulatedCollector() throws Exception {
        final MetricRegistry metricRegistry = new MetricRegistry();
        final Counter counter = metricRegistry.counter("counter");

        counter.inc();

        server.replaceCollector(new DropwizardExports(metricRegistry));

        doGET(server, "/metrics", response -> {
            assertThat(response.body()).isNotNull();
            assertThat(response.body().string()).contains("counter 1.0");
        });
        doGET(server, "/", response -> {
            assertThat(response.body()).isNotNull();
            assertThat(response.body().string()).contains("counter 1.0");
        });
        doGET(server, "/-/healthy", response -> {
            assertThat(response.body()).isNotNull();
            assertThat(response.body().string()).containsIgnoringCase("exporter is healthy");
        });
    }

    void doGET(PrometheusExporterHTTPServer server,
               String path,
               ExceptionalConsumer<Response> consumer) throws Exception {
        final OkHttpClient httpClient = new OkHttpClient();

        final int serverPort = server.getPort().orElseThrow(() -> new IllegalStateException("Missing server port"));

        final Request request = new Request.Builder()
                .get()
                .url("http://127.0.0.1:" + serverPort + path)
                .build();
        try (final Response response = httpClient.newCall(request).execute()) {
            consumer.accept(response);
        }
    }

    interface ExceptionalConsumer<T> {
        void accept(T t) throws Exception;
    }
}

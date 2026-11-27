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
package org.graylog2.jersey;

import com.google.inject.Guice;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;
import org.graylog2.shared.bindings.GuiceInjectorHolder;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.graylog2.shared.utilities.StringUtils.f;
import static org.mockito.Mockito.mockStatic;

class IgnoreH2cUpgradeAddOnTest {
    @Path("/")
    public static class EchoResource {
        @POST
        public String echo(String body) {
            return body;
        }
    }

    @Test
    void readsPostBodyWithCleartextUpgrade() throws Exception {
        try (final var holder = mockStatic(GuiceInjectorHolder.class)) {
            holder.when(GuiceInjectorHolder::getInjector).thenReturn(Guice.createInjector());
            final var server = GrizzlyHttpServerFactory.createHttpServer(URI.create("http://127.0.0.1:0/"),
                    new ResourceConfig(EchoResource.class), false);
            try (final AutoCloseable shutdown = server::shutdownNow;
                 final var client = HttpClient.newHttpClient()) {
                final var listener = server.getListener("grizzly");
                listener.registerAddOn(new IgnoreH2cUpgradeAddOn());
                server.start();
                final var body = "{\"message\":\"hello\"}";
                final var request = HttpRequest.newBuilder(URI.create(f("http://127.0.0.1:%d/", listener.getPort())))
                        .timeout(Duration.ofSeconds(5))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(body))
                        .build();
                final var response = client.send(request, HttpResponse.BodyHandlers.ofString());
                assertThat(response.statusCode()).isEqualTo(200);
                assertThat(response.body()).isEqualTo(body);
            }
        }
    }
}

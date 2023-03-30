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
package org.graylog2.shared.rest;

import org.glassfish.jersey.internal.MapPropertiesDelegate;
import org.glassfish.jersey.server.ContainerRequest;
import org.glassfish.jersey.server.ContainerResponse;
import org.junit.jupiter.api.Test;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.net.URI;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

class EmbeddingControlFilterTest {
    private static final ContainerRequest requestContext = new ContainerRequest(URI.create("http://localhost"), URI.create("/"), HttpMethod.GET, null, new MapPropertiesDelegate(), null);

    @Test
    void disallowsEmbeddingIfConfigurationSettingIsFalse() throws IOException {
        final EmbeddingControlFilter filter = new EmbeddingControlFilter(false);

        final ContainerResponseContext responseContext = new ContainerResponse(requestContext, Response.ok().build());
        filter.filter(requestContext, responseContext);

        assertThat(responseContext.getHeaders())
                .containsEntry("X-Frame-Options", Collections.singletonList("DENY"));
    }

    @Test
    void allowsEmbeddingForSameOriginIfConfigurationSettingIsTrue() throws IOException {
        final EmbeddingControlFilter filter = new EmbeddingControlFilter(true);

        final ContainerResponseContext responseContext = new ContainerResponse(requestContext, Response.ok().build());
        filter.filter(requestContext, responseContext);

        assertThat(responseContext.getHeaders())
                .containsEntry("X-Frame-Options", Collections.singletonList("SAMEORIGIN"));
    }
}

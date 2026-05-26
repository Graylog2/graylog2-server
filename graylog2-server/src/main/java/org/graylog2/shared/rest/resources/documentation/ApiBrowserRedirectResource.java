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
package org.graylog2.shared.rest.resources.documentation;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import org.graylog2.configuration.HttpConfiguration;
import org.graylog2.rest.RestTools;

import java.net.URI;

import static java.util.Objects.requireNonNull;

/**
 * Redirects the old API browser URLs (served under the REST API prefix) to the
 * new frontend route at {@code /api-browser}, so that existing bookmarks keep
 * working after the server-side Swagger UI was removed.
 */
@Path("/api-browser{route: .*}")
public class ApiBrowserRedirectResource {
    private final HttpConfiguration httpConfiguration;

    @Inject
    public ApiBrowserRedirectResource(HttpConfiguration httpConfiguration) {
        this.httpConfiguration = requireNonNull(httpConfiguration, "httpConfiguration");
    }

    @GET
    public Response redirect(@Context HttpHeaders httpHeaders) {
        final URI base = RestTools.buildExternalUri(httpHeaders.getRequestHeaders(),
                httpConfiguration.getHttpExternalUri());
        return Response.status(Response.Status.MOVED_PERMANENTLY)
                .location(base.resolve("api-browser"))
                .build();
    }
}

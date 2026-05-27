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

import io.swagger.v3.oas.annotations.Hidden;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import org.graylog2.rest.URIHelper;

/**
 * Redirects the old API browser URLs (served under the REST API prefix) to the
 * new frontend route at {@code /api-browser}, so that existing bookmarks keep
 * working after the server-side Swagger UI was removed.
 */
@Hidden
@Path("/api-browser{route: .*}")
public class ApiBrowserRedirectResource {

    @GET
    public Response redirect(@Context URIHelper uriHelper) {
        return Response.status(Response.Status.MOVED_PERMANENTLY)
                .location(uriHelper.resolve("api-browser"))
                .build();
    }
}

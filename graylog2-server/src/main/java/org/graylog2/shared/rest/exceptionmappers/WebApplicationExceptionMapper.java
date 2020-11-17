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
package org.graylog2.shared.rest.exceptionmappers;

import org.glassfish.jersey.spi.ExtendedExceptionMapper;
import org.graylog2.plugin.rest.ApiError;

import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

public class WebApplicationExceptionMapper implements ExtendedExceptionMapper<WebApplicationException> {
    @Override
    public boolean isMappable(WebApplicationException e) {
        return !(e instanceof NotAuthorizedException) && e != null;
    }

    @Override
    public Response toResponse(WebApplicationException e) {
        final ApiError apiError = ApiError.create(e.getMessage());

        return Response.fromResponse(e.getResponse())
                .type(MediaType.APPLICATION_JSON_TYPE)
                .entity(apiError).build();
    }
}

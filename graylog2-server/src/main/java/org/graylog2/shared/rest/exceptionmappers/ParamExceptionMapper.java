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

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.glassfish.jersey.server.ParamException;
import org.graylog2.plugin.rest.ApiError;

import static org.graylog2.shared.utilities.StringUtils.f;

/**
 * Maps Jersey {@link ParamException} (including invalid query parameters) to HTTP 400.
 * <p>
 * Jersey defaults URI parameter conversion failures to 404; for request parameters that
 * is misleading. See Graylog issue #4721.
 */
@Provider
public class ParamExceptionMapper implements ExceptionMapper<ParamException> {
    @Override
    public Response toResponse(ParamException exception) {
        final String message = f("Invalid value for %s parameter %s",
                exception.getParameterType().getSimpleName(),
                exception.getParameterName());
        final ApiError apiError = ApiError.create(message);

        return Response.status(Response.Status.BAD_REQUEST)
                .type(MediaType.APPLICATION_JSON_TYPE)
                .entity(apiError)
                .build();
    }
}

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
package org.graylog.plugins.views.search.rest.exceptionmappers;

import com.google.common.collect.ImmutableMap;
import org.graylog.plugins.views.search.errors.MissingCapabilitiesException;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;

import java.util.Map;

public class MissingCapabilitiesExceptionMapper implements ExceptionMapper<MissingCapabilitiesException> {
    @Override
    public Response toResponse(MissingCapabilitiesException exception) {
        final Map<String, Object> error = ImmutableMap.of(
                "error", "Unable to execute this search, the following capabilities are missing:",
                "missing", exception.getMissingRequirements()
        );
        return Response.status(Response.Status.CONFLICT).entity(error).build();
    }
}

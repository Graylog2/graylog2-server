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

import org.graylog2.plugin.rest.MissingStreamPermissionError;
import org.graylog2.shared.rest.exceptions.MissingStreamPermissionException;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

public class MissingStreamPermissionExceptionMapper implements ExceptionMapper<MissingStreamPermissionException> {
    @Override
    public Response toResponse(MissingStreamPermissionException e) {
        final MissingStreamPermissionError missingStreamPermissionError = MissingStreamPermissionError.builder()
                .errorMessage(e.getMessage())
                .streams(e.streamsWithMissingPermissions())
                .build();
        return Response.status(Response.Status.FORBIDDEN)
                .type(MediaType.APPLICATION_JSON_TYPE)
                .entity(missingStreamPermissionError)
                .build();
    }
}


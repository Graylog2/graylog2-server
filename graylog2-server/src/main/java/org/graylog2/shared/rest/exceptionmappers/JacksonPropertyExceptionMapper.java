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

import com.fasterxml.jackson.databind.exc.PropertyBindingException;
import com.google.common.base.Joiner;
import org.graylog2.plugin.rest.ApiError;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.Collection;
import java.util.Collections;

import static com.google.common.base.MoreObjects.firstNonNull;
import static javax.ws.rs.core.Response.status;

@Provider
public class JacksonPropertyExceptionMapper implements ExceptionMapper<PropertyBindingException> {
    @Override
    public Response toResponse(PropertyBindingException e) {
        final Collection<Object> knownPropertyIds = firstNonNull(e.getKnownPropertyIds(), Collections.emptyList());
        final StringBuilder message = new StringBuilder("Unable to map property ")
                .append(e.getPropertyName())
                .append(".\nKnown properties include: ");
        Joiner.on(", ").appendTo(message, knownPropertyIds);
        final ApiError apiError = ApiError.create(message.toString());
        return status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON_TYPE).entity(apiError).build();
    }
}

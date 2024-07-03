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

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import org.glassfish.jersey.spi.ExtendedExceptionMapper;
import org.graylog2.plugin.rest.ApiError;
import org.graylog2.rest.MapExceptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Optional;

import static com.google.common.base.Strings.nullToEmpty;
import static java.util.Objects.nonNull;

@Provider
public class AnyExceptionClassMapper implements ExtendedExceptionMapper<Exception> {
    private static final Logger LOG = LoggerFactory.getLogger(AnyExceptionClassMapper.class);

    @Context
    private ResourceInfo resourceInfo;

    @Override
    public boolean isMappable(Exception exception) {
        // we map anything except WebApplicationException to a response, WAEs are handled by the framework.
        return !(exception instanceof WebApplicationException);
    }

    @Override
    public Response toResponse(Exception exception) {
        final Optional<Response.Status> status = getStatus(exception);

        if (status.isPresent()) {
            return toResponse(exception, status.get());
        }

        LOG.error("Unhandled exception in REST resource", exception);
        return toResponse(exception, Response.Status.INTERNAL_SERVER_ERROR);
    }

    private Response toResponse(Exception exception, Response.Status status) {
        final String message = nullToEmpty(exception.getMessage());
        final ApiError apiError = ApiError.create(message);

        return Response.status(status)
                .type(MediaType.APPLICATION_JSON_TYPE)
                .entity(apiError)
                .build();
    }

    private Optional<Response.Status> getStatus(Exception exception) {
        final var annotation = resourceInfo.getResourceClass().getAnnotation(MapExceptions.class);
        if (annotation != null && annotation.value().length > 0) {
            return Arrays.stream(annotation.value())
                    .filter(type -> nonNull(type.value()))
                    .filter(type -> type.value().isAssignableFrom(exception.getClass()))
                    .map(MapExceptions.Type::status)
                    .findFirst();
        }
        return Optional.empty();
    }
}

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

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.fasterxml.jackson.databind.exc.ValueInstantiationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.graylog2.plugin.rest.ApiError;

import java.util.stream.Collectors;

import static com.google.common.base.MoreObjects.firstNonNull;
import static jakarta.ws.rs.core.Response.status;

@Provider
public class JsonMappingExceptionMapper implements ExceptionMapper<JsonMappingException> {
    @Override
    public Response toResponse(JsonMappingException e) {
        final String messagePrefix = errorWithJsonPath(e);
        final String problemMessage = firstNonNull(e.getCause().getMessage(), e.getMessage());
        final String location = "[" + e.getLocation().getLineNr() + ", " + e.getLocation().getColumnNr() + "]";
        final String message = firstNonNull(problemMessage, "Couldn't process JSON input");
        final ApiError apiError = ApiError.create("Error at " + location + ": " + messagePrefix + ": " + message);
        return status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON_TYPE).entity(apiError).build();
    }

    private String errorWithJsonPath(final JsonMappingException e) {
        final String pathToErrorField = e.getPath().stream()
                .map(path -> {
                    final var fieldName = path.getFieldName();
                    if (fieldName == null && path.getIndex() != -1) {
                        return "[" + path.getIndex() + "]";
                    }
                    return fieldName;
                })
                .collect(Collectors.joining("."));
        final var quotedPath = "\"" + pathToErrorField + "\"";
        if (e instanceof MismatchedInputException mismatchedInputException) {
            return quotedPath + " must be of type " + mismatchedInputException.getTargetType().getSimpleName();
        }
        if (e instanceof ValueInstantiationException) {
            return "Unable to construct " + quotedPath;
        } else {
            return "Error for " + quotedPath;
        }
    }
}

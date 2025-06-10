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
import com.fasterxml.jackson.databind.exc.PropertyBindingException;
import com.fasterxml.jackson.databind.exc.ValueInstantiationException;
import com.google.common.base.Joiner;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.graylog2.plugin.rest.RequestError;

import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;

import static com.google.common.base.MoreObjects.firstNonNull;
import static jakarta.ws.rs.core.Response.status;

@Provider
public class JsonMappingExceptionMapper implements ExceptionMapper<JsonMappingException> {
    @Override
    public Response toResponse(JsonMappingException e) {
        final var errorPath = errorPath(e);
        final var location = e.getLocation();
        final var lineNr = location != null ? location.getLineNr() : -1;
        final var columnNr = location != null ? location.getColumnNr() : -1;
        final String message = errorWithJsonPath(e, errorPath, lineNr, columnNr);
        final var referencePath = referencePath(e);
        final var apiError = RequestError.create(message, lineNr, columnNr, errorPath, referencePath);
        return status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON_TYPE).entity(apiError).build();
    }

    private String referencePath(JsonMappingException e) {
        final var sb = new StringBuilder().append(e.getPathReference());

        if (e instanceof ValueInstantiationException vie) {
            if (!sb.isEmpty()) {
                sb.append("->");
            }
            sb.append(vie.getType().getRawClass().getCanonicalName());
        }

        return sb.toString();
    }

    private String errorPath(final JsonMappingException e) {
        return e.getPath().stream()
                .map(path -> {
                    final var fieldName = path.getFieldName();
                    if (fieldName == null && path.getIndex() != -1) {
                        return "[" + path.getIndex() + "]";
                    }
                    return fieldName;
                })
                .collect(Collectors.joining("."));
    }

    private String errorWithJsonPath(final JsonMappingException e, String path, int lineNr, int columnNr) {
        final var location = lineNr >= 0 && columnNr >= 0 ? "[" + lineNr + ", " + columnNr + "]" : "";
        final var quotedPath = "\"" + path + "\"";
        final var messagePrefix = "Error at " + quotedPath + (location.isEmpty() ? "" : " " + location);


        if (e instanceof PropertyBindingException propertyBindingException) {
            final Collection<Object> knownPropertyIds = firstNonNull(propertyBindingException.getKnownPropertyIds(), Collections.emptyList());
            final StringBuilder message = new StringBuilder("Unable to map property ")
                    .append(propertyBindingException.getPropertyName())
                    .append(".\nKnown properties include: ");
            Joiner.on(", ").appendTo(message, knownPropertyIds);
            return message.toString();
        }
        if (e instanceof MismatchedInputException mismatchedInputException) {
            final var targetType = mismatchedInputException.getTargetType();
            if (targetType != null) {
                return messagePrefix + ": Must be of type " + mismatchedInputException.getTargetType().getSimpleName();
            } else {
                return messagePrefix + ": " + mismatchedInputException.getMessage();
            }
        } else {
            final var cause = e.getCause();
            final String problemMessage = firstNonNull(cause, e).getMessage();
            final String message = firstNonNull(problemMessage, "Couldn't process JSON input");

            return messagePrefix + ": " + message;
        }
    }
}

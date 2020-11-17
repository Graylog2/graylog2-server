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
package org.graylog2.rest;

import org.graylog2.indexer.QueryParsingException;
import org.graylog2.rest.resources.search.responses.QueryParseError;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class QueryParsingExceptionMapper implements ExceptionMapper<QueryParsingException> {
    @Override
    public Response toResponse(QueryParsingException exception) {
        final QueryParseError errorMessage = QueryParseError.create(
                exception.getMessage(),
                exception.getErrorDetails(),
                exception.getLine().orElse(null),
                exception.getColumn().orElse(null));

        return Response.status(Response.Status.BAD_REQUEST)
                .entity(errorMessage)
                .build();
    }
}

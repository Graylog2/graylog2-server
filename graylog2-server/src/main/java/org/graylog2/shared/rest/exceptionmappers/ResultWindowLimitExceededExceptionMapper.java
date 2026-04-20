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

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import org.graylog2.indexer.exceptions.ResultWindowLimitExceededException;
import org.graylog2.rest.resources.search.responses.SearchError;

public class ResultWindowLimitExceededExceptionMapper implements ExceptionMapper<ResultWindowLimitExceededException> {

    @Override
    public Response toResponse(final ResultWindowLimitExceededException exception) {
        final SearchError searchError = SearchError.create(exception.getMessage(), exception.getErrorDetails());

        return Response.status(Response.Status.BAD_REQUEST)
                .entity(searchError)
                .build();
    }
}

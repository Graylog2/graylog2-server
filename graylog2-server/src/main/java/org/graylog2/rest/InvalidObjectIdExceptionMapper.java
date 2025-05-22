package org.graylog2.rest;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.graylog2.plugin.rest.ApiError;
import org.graylog2.rest.helpers.DatabaseIdParser;

@Provider
public class InvalidObjectIdExceptionMapper implements ExceptionMapper<DatabaseIdParser.InvalidObjectIdException> {
    @Override
    public Response toResponse(DatabaseIdParser.InvalidObjectIdException exception) {
        final ApiError apiError = ApiError.create(exception.getMessage());
        return Response.status(Response.Status.NOT_FOUND).entity(apiError).build();
    }
}

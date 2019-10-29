package org.graylog.plugins.views.search.rest.exceptionmappers;

import org.graylog.plugins.views.search.errors.EntityNotFoundException;
import org.graylog2.plugin.rest.ApiError;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

public class EntityNotFoundExceptionMapper implements ExceptionMapper<EntityNotFoundException> {
    @Override
    public Response toResponse(EntityNotFoundException exception) {
        final ApiError apiError = ApiError.create(exception.getMessage());
        return Response.status(Response.Status.NOT_FOUND).entity(apiError).build();
    }
}

package org.graylog.plugins.views.search.rest.exceptionmappers;

import org.graylog.plugins.views.search.EntityNotFoundException;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

public class EntityNotFoundExceptionMapper implements ExceptionMapper<EntityNotFoundException> {
    @SuppressWarnings("unused")
    @Override
    public Response toResponse(EntityNotFoundException exception) {
        return Response.status(Response.Status.NOT_FOUND).entity(exception.getMessage()).type("text/plain").build();
    }
}

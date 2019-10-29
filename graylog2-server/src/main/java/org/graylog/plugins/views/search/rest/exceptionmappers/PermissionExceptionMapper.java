package org.graylog.plugins.views.search.rest.exceptionmappers;

import org.graylog.plugins.views.search.errors.PermissionException;
import org.graylog2.plugin.rest.ApiError;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

public class PermissionExceptionMapper implements ExceptionMapper<PermissionException> {
    @Override
    public Response toResponse(PermissionException exception) {
        final ApiError apiError = ApiError.create(exception.getMessage());
        return Response.status(Response.Status.FORBIDDEN).entity(apiError).build();
    }
}

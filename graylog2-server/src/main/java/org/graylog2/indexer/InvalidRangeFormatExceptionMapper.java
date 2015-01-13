package org.graylog2.indexer;

import org.graylog2.plugin.rest.ApiError;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class InvalidRangeFormatExceptionMapper implements ExceptionMapper<InvalidRangeFormatException> {
    @Override
    public Response toResponse(InvalidRangeFormatException exception) {
        return Response
                .status(Response.Status.BAD_REQUEST)
                .type(MediaType.APPLICATION_JSON_TYPE)
                .entity(new ApiError(exception.getMessage()))
                .build();
    }
}

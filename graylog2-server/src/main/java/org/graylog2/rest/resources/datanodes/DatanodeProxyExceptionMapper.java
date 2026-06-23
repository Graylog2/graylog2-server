package org.graylog2.rest.resources.datanodes;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;

public class DatanodeProxyExceptionMapper implements ExceptionMapper<DatanodeNotFoundException> {

    @Override
    public Response toResponse(DatanodeNotFoundException exception) {
        return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                .entity(exception.getMessage())
                .type(MediaType.TEXT_PLAIN_TYPE)
                .build();
    }
}

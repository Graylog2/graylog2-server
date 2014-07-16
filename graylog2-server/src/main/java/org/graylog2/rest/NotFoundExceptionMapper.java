package org.graylog2.rest;

import org.graylog2.database.NotFoundException;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public class NotFoundExceptionMapper implements ExceptionMapper<NotFoundException> {
    @Override
    public Response toResponse(NotFoundException exception) {
        return Response.status(Response.Status.NOT_FOUND).entity(exception.getMessage()).build();
    }
}

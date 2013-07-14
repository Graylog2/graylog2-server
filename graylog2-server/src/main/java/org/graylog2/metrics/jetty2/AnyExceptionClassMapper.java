package org.graylog2.metrics.jetty2;

import org.glassfish.jersey.spi.ExtendedExceptionMapper;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

@Provider
public class AnyExceptionClassMapper implements ExtendedExceptionMapper<Exception> {

    @Override
    public boolean isMappable(Exception exception) {
        return true;
    }

    @Override
    public Response toResponse(Exception exception) {
        final Response.ResponseBuilder builder = Response.serverError();
        builder.entity(exception);
        return builder.build();
    }

}

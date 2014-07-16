package org.graylog2.plugin.rest;

import com.google.common.base.Throwables;
import org.glassfish.jersey.spi.ExtendedExceptionMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

@Provider
public class AnyExceptionClassMapper implements ExtendedExceptionMapper<Exception> {
    private static final Logger log = LoggerFactory.getLogger(AnyExceptionClassMapper.class);

    @Override
    public boolean isMappable(Exception exception) {
        // we map anything except WebApplicationException to a response, WAEs are handled by the framework.
        return !(exception instanceof WebApplicationException);
    }

    @Override
    public Response toResponse(Exception exception) {

        log.error("Unhandled exception in REST resource", exception);

        final StringBuilder sb = new StringBuilder();
        if (exception.getMessage() != null) {
            sb.append(exception.getMessage()).append("\n");
        }
        sb.append(Throwables.getStackTraceAsString(exception));
        return Response.serverError()
                .type(MediaType.TEXT_PLAIN_TYPE)
                .entity(sb.toString())
                .build();
    }

}

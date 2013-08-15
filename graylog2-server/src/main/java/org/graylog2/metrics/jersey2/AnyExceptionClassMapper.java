package org.graylog2.metrics.jersey2;

import com.google.common.base.Throwables;
import org.glassfish.jersey.spi.ExtendedExceptionMapper;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

@Provider
public class AnyExceptionClassMapper implements ExtendedExceptionMapper<Exception> {

    @Override
    public boolean isMappable(Exception exception) {
        // we map anything except WebApplicationException to a response, WAEs are handled by the framework.
        return !(exception instanceof WebApplicationException);
    }

    @Override
    public Response toResponse(Exception exception) {
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

/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.shared.rest.exceptionmappers;

import org.glassfish.jersey.spi.ExtendedExceptionMapper;
import org.graylog2.plugin.rest.ApiError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

import static com.google.common.base.Strings.nullToEmpty;

@Provider
public class AnyExceptionClassMapper implements ExtendedExceptionMapper<Exception> {
    private static final Logger LOG = LoggerFactory.getLogger(AnyExceptionClassMapper.class);

    @Override
    public boolean isMappable(Exception exception) {
        // we map anything except WebApplicationException to a response, WAEs are handled by the framework.
        return !(exception instanceof WebApplicationException);
    }

    @Override
    public Response toResponse(Exception exception) {
        LOG.error("Unhandled exception in REST resource", exception);
        final String message = nullToEmpty(exception.getMessage());
        final ApiError apiError = ApiError.create(message);

        return Response.serverError()
                .type(MediaType.APPLICATION_JSON_TYPE)
                .entity(apiError)
                .build();
    }
}

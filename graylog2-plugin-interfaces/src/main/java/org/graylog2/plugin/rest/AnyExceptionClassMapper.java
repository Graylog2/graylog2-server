/*
 * Copyright 2012-2014 TORCH GmbH
 *
 * This file is part of Graylog2.
 *
 * Graylog2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog2.  If not, see <http://www.gnu.org/licenses/>.
 */

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

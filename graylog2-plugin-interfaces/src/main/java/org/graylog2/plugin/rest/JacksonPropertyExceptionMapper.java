/*
 * Copyright 2013 TORCH GmbH
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

import com.fasterxml.jackson.databind.exc.PropertyBindingException;
import com.google.common.base.Joiner;
import org.glassfish.jersey.spi.ExtendedExceptionMapper;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;
import java.util.Collection;

import static javax.ws.rs.core.Response.status;

@Provider
public class JacksonPropertyExceptionMapper implements ExtendedExceptionMapper<PropertyBindingException> {

    @Override
    public boolean isMappable(PropertyBindingException exception) {
        return exception != null;
    }

    @Override
    public Response toResponse(PropertyBindingException exception) {
        final Collection<Object> knownPropertyIds = exception.getKnownPropertyIds();
        StringBuilder b = new StringBuilder();
        b.append("Unable to map property ").append(exception.getPropertyName()).append(". \n");
        b.append("Known properties include: ").append(Joiner.on(", ").join(knownPropertyIds));
        final String message = b.toString();
        return status(Response.Status.BAD_REQUEST).entity(message).build();
    }
}

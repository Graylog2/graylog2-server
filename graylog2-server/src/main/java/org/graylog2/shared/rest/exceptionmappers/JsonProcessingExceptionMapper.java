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

import com.fasterxml.jackson.core.JsonProcessingException;
import org.graylog2.plugin.rest.ApiError;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import static com.google.common.base.MoreObjects.firstNonNull;
import static javax.ws.rs.core.Response.status;

@Provider
public class JsonProcessingExceptionMapper implements ExceptionMapper<JsonProcessingException> {
    @Override
    public Response toResponse(JsonProcessingException e) {
        final String message = firstNonNull(e.getMessage(), "Couldn't process JSON input");
        final ApiError apiError = ApiError.create(message);
        return status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON_TYPE).entity(apiError).build();
    }
}

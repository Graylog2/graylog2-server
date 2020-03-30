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

import org.graylog2.plugin.rest.MissingStreamPermissionError;
import org.graylog2.shared.rest.exceptions.MissingStreamPermissionException;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

public class MissingStreamPermissionExceptionMapper implements ExceptionMapper<MissingStreamPermissionException> {
    @Override
    public Response toResponse(MissingStreamPermissionException e) {
        final MissingStreamPermissionError missingStreamPermissionError = MissingStreamPermissionError.builder()
                .errorMessage(e.getMessage())
                .streams(e.streamsWithMissingPermissions())
                .build();
        return Response.status(Response.Status.NOT_ACCEPTABLE)
                .type(MediaType.APPLICATION_JSON_TYPE)
                .entity(missingStreamPermissionError)
                .build();
    }
}


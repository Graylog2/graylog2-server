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
package org.graylog.plugins.views.search.errors;

import com.google.common.collect.ImmutableMap;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import java.util.Map;

public class MissingCapabilitiesExceptionMapper implements ExceptionMapper<MissingCapabilitiesException> {
    @Override
    public Response toResponse(MissingCapabilitiesException exception) {
        final Map<String, Object> error = ImmutableMap.of(
                "error", "Unable to execute this search, the following capabilities are missing:",
                "missing", exception.getMissingRequirements()
        );
        return Response.status(Response.Status.CONFLICT).entity(error).build();
    }
}

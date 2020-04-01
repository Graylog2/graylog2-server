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
package org.graylog.plugins.views.search.rest;

import org.apache.shiro.authz.annotation.RequiresAuthentication;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

@Path("/views/dashboards")
@RequiresAuthentication
public class DashboardsRedirectResource {
    @GET
    @Deprecated
    public Response redirect(@Context UriInfo uriInfo) {
        final UriBuilder uriBuilder = uriInfo.getRequestUriBuilder()
            .replacePath(
                uriInfo.getPath().replace("/views/dashboards", "/dashboards")
            );
        return Response.status(Response.Status.MOVED_PERMANENTLY)
                .location(uriBuilder.build())
                .build();
    }
}

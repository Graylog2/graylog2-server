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

package org.graylog.plugins.views.search.rest;

import org.apache.shiro.authz.annotation.RequiresAuthentication;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

@Path("/views/savedSearches")
@RequiresAuthentication
public class SavedSearchesRedirectResource extends RedirectResource {
    private final String sourcePath;
    private final String targetPath;

    public SavedSearchesRedirectResource() {
        this.sourcePath = pathForClass(this.getClass());
        this.targetPath = pathForClass(SavedSearchesResource.class);
    }

    @GET
    @Deprecated
    public Response redirect(@Context UriInfo uriInfo) {
        final UriBuilder uriBuilder = uriInfo.getRequestUriBuilder()
                .replacePath(
                        uriInfo.getPath().replace(sourcePath, targetPath)
                );
        return Response.status(Response.Status.MOVED_PERMANENTLY)
                .location(uriBuilder.build())
                .build();
    }
}

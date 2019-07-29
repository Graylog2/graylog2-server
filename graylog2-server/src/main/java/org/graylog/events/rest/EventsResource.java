package org.graylog.events.rest;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog.events.search.EventsSearchParameters;
import org.graylog.events.search.EventsSearchResult;
import org.graylog.events.search.EventsSearchService;
import org.graylog2.plugin.rest.PluginRestResource;
import org.graylog2.shared.rest.resources.RestResource;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Api(value = "Events", description = "Events overview and search")
@Path("/events")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RequiresAuthentication
public class EventsResource extends RestResource implements PluginRestResource {
    private final EventsSearchService searchService;

    @Inject
    public EventsResource(EventsSearchService searchService) {
        this.searchService = searchService;
    }

    @POST
    @Path("/search")
    @ApiOperation("Search events")
    public EventsSearchResult search(EventsSearchParameters request) {
        final EventsSearchResult result = searchService.search(request);

        return result;
    }
}

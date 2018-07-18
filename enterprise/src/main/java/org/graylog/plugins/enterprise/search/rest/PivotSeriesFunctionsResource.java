package org.graylog.plugins.enterprise.search.rest;

import io.swagger.annotations.Api;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog2.plugin.rest.PluginRestResource;
import org.graylog2.shared.rest.resources.RestResource;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.Map;

@Api(value = "Enterprise/Search/Functions", description = "Definitions for aggregation functions")
@Path("/functions")
@Produces(MediaType.APPLICATION_JSON)
@RequiresAuthentication
public class PivotSeriesFunctionsResource extends RestResource implements PluginRestResource {
    private final Map<String, SeriesDescription> availableFunctions;

    @Inject
    public PivotSeriesFunctionsResource(Map<String, SeriesDescription> availableFunctions) {
        this.availableFunctions = availableFunctions;
    }

    @GET
    public Map<String, SeriesDescription> functions() {
        return this.availableFunctions;
    }
}

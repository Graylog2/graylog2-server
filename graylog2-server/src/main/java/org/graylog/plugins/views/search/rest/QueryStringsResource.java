package org.graylog.plugins.views.search.rest;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog.plugins.views.search.permissions.SearchUser;
import org.graylog.plugins.views.search.querystrings.LastUsedQueryStringsService;
import org.graylog2.audit.jersey.NoAuditEvent;
import org.graylog2.plugin.rest.PluginRestResource;
import org.graylog2.shared.rest.resources.RestResource;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static org.graylog2.shared.rest.documentation.generator.Generator.CLOUD_VISIBLE;

@RequiresAuthentication
@Api(value = "Search/Query Strings", tags = {CLOUD_VISIBLE})
@Path("/search/query_strings")
public class QueryStringsResource extends RestResource implements PluginRestResource {
    private final LastUsedQueryStringsService lastUsedQueryStringsService;

    @Inject
    public QueryStringsResource(LastUsedQueryStringsService lastUsedQueryStringsService) {
        this.lastUsedQueryStringsService = lastUsedQueryStringsService;
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation("Register a query string used")
    @NoAuditEvent("No audit event needed for this operation")
    public Response queryStringUsed(@ApiParam(name = "queryStringRequest") @Valid @NotNull final QueryStringUsedDTO queryStringUsed,
                                    @Context final SearchUser searchUser) {
        this.lastUsedQueryStringsService.save(searchUser.getUser(), queryStringUsed.queryString());
        return Response.noContent().build();
    }

}

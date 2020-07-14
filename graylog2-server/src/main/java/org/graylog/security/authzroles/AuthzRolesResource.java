package org.graylog.security.authzroles;

import com.codahale.metrics.annotation.Timed;
import com.google.common.collect.ImmutableMap;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog2.database.PaginatedList;
import org.graylog2.rest.models.PaginatedResponse;
import org.graylog2.search.SearchQuery;
import org.graylog2.search.SearchQueryField;
import org.graylog2.search.SearchQueryParser;
import org.graylog2.shared.rest.resources.RestResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.validation.constraints.NotEmpty;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

@RequiresAuthentication
@Api(value = "AuthzRoles", description = "Read Roles")
@Path("/authzRoles")
public class AuthzRolesResource extends RestResource {
    private static final Logger LOG = LoggerFactory.getLogger(AuthzRolesResource.class);


    protected static final ImmutableMap<String, SearchQueryField> SEARCH_FIELD_MAPPING = ImmutableMap.<String, SearchQueryField>builder()
            .put(AuthzRoleDTO.FIELD_NAME, SearchQueryField.create(AuthzRoleDTO.FIELD_NAME))
            .put(AuthzRoleDTO.FIELD_DESCRIPTION, SearchQueryField.create(AuthzRoleDTO.FIELD_DESCRIPTION))
            .build();

    private final PaginatedAuthzRolesService authzRolesService;
    private final SearchQueryParser searchQueryParser;

    @Inject
    public AuthzRolesResource(PaginatedAuthzRolesService authzRolesService) {
        this.authzRolesService = authzRolesService;

        this.searchQueryParser = new SearchQueryParser(AuthzRoleDTO.FIELD_NAME, SEARCH_FIELD_MAPPING);
    }

    @GET
    @Timed
    @ApiOperation(value = "Get a paginated list of all roles")
    @Produces(MediaType.APPLICATION_JSON)
    public PaginatedResponse<AuthzRoleDTO> getList(@ApiParam(name = "page") @QueryParam("page") @DefaultValue("1") int page,
        @ApiParam(name = "per_page") @QueryParam("per_page") @DefaultValue("50") int perPage,
        @ApiParam(name = "query") @QueryParam("query") @DefaultValue("") String query,
        @ApiParam(name = "sort",
                value = "The field to sort the result on",
                required = true,
                allowableValues = "name,description")
        @DefaultValue(AuthzRoleDTO.FIELD_NAME) @QueryParam("sort") String sort,
        @ApiParam(name = "order", value = "The sort direction", allowableValues = "asc, desc")
        @DefaultValue("asc") @QueryParam("order") String order) {

        SearchQuery searchQuery;
        try {
            searchQuery = searchQueryParser.parse(query);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid argument in search query: " + e.getMessage());
        }

        final PaginatedList<AuthzRoleDTO> result = authzRolesService.findPaginated(searchQuery, page, perPage,sort, order);
        return PaginatedResponse.create("roles", result, query);
    }

    @GET
    @ApiOperation(value = "Get a paginated list roles for a user")
    @Path("/{username}")
    @Produces(MediaType.APPLICATION_JSON)
    public PaginatedResponse<AuthzRoleDTO> getListForUser(
        @ApiParam(name = "username") @PathParam("username") @NotEmpty String username,
        @ApiParam(name = "page") @QueryParam("page") @DefaultValue("1") int page,
        @ApiParam(name = "per_page") @QueryParam("per_page") @DefaultValue("50") int perPage,
        @ApiParam(name = "query") @QueryParam("query") @DefaultValue("") String query,
        @ApiParam(name = "sort",
                value = "The field to sort the result on",
                required = true,
                allowableValues = "name,description")
        @DefaultValue(AuthzRoleDTO.FIELD_NAME) @QueryParam("sort") String sort,
        @ApiParam(name = "order", value = "The sort direction", allowableValues = "asc, desc")
        @DefaultValue("asc") @QueryParam("order") String order) {

        SearchQuery searchQuery;
        try {
            searchQuery = searchQueryParser.parse(query);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid argument in search query: " + e.getMessage());
        }

        final PaginatedList<AuthzRoleDTO> result = authzRolesService.findPaginatedForUser(searchQuery, page, perPage,sort, order, username);
        return PaginatedResponse.create("roles", result, query);
    }
}

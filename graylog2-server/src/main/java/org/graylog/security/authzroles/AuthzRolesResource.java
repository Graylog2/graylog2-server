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
package org.graylog.security.authzroles;

import com.codahale.metrics.annotation.Timed;
import com.google.common.collect.ImmutableMap;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.graylog2.audit.AuditEventTypes;
import org.graylog2.audit.jersey.AuditEvent;
import org.graylog2.database.PaginatedList;
import org.graylog2.plugin.database.ValidationException;
import org.graylog2.plugin.database.users.User;
import org.graylog2.rest.models.PaginatedResponse;
import org.graylog2.search.SearchQuery;
import org.graylog2.search.SearchQueryField;
import org.graylog2.search.SearchQueryParser;
import org.graylog2.shared.rest.resources.RestResource;
import org.graylog2.shared.security.RestPermissions;
import org.graylog2.shared.users.UserService;
import org.graylog2.users.PaginatedUserService;
import org.graylog2.users.UserOverviewDTO;

import javax.inject.Inject;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import java.util.Set;

import static org.graylog2.shared.security.RestPermissions.USERS_EDIT;

@RequiresAuthentication
@Api(value = "AuthzRoles", description = "Read Roles")
@Path("/authzRoles")
@Produces(MediaType.APPLICATION_JSON)
public class AuthzRolesResource extends RestResource {
    protected static final ImmutableMap<String, SearchQueryField> SEARCH_FIELD_MAPPING = ImmutableMap.<String, SearchQueryField>builder()
            .put(AuthzRoleDTO.FIELD_NAME, SearchQueryField.create(AuthzRoleDTO.FIELD_NAME))
            .put(AuthzRoleDTO.FIELD_DESCRIPTION, SearchQueryField.create(AuthzRoleDTO.FIELD_DESCRIPTION))
            .build();

    protected static final ImmutableMap<String, SearchQueryField> USER_SEARCH_FIELD_MAPPING = ImmutableMap.<String, SearchQueryField>builder()
            .put(UserOverviewDTO.FIELD_USERNAME, SearchQueryField.create(UserOverviewDTO.FIELD_USERNAME))
            .put(UserOverviewDTO.FIELD_FULL_NAME, SearchQueryField.create(UserOverviewDTO.FIELD_FULL_NAME))
            .put(UserOverviewDTO.FIELD_EMAIL, SearchQueryField.create(UserOverviewDTO.FIELD_EMAIL))
            .build();

    private final PaginatedAuthzRolesService authzRolesService;
    private final PaginatedUserService paginatedUserService;
    private final UserService userService;
    private final SearchQueryParser searchQueryParser;
    private final SearchQueryParser userSearchQueryParser;

    @Inject
    public AuthzRolesResource(PaginatedAuthzRolesService authzRolesService, PaginatedUserService paginatedUserService, UserService userService) {
        this.authzRolesService = authzRolesService;
        this.paginatedUserService = paginatedUserService;
        this.userService = userService;

        this.searchQueryParser = new SearchQueryParser(AuthzRoleDTO.FIELD_NAME, SEARCH_FIELD_MAPPING);
        this.userSearchQueryParser = new SearchQueryParser(UserOverviewDTO.FIELD_USERNAME, USER_SEARCH_FIELD_MAPPING);
    }

    @GET
    @Timed
    @ApiOperation(value = "Get a paginated list of all roles")
    @RequiresPermissions(RestPermissions.ROLES_READ)
    public PaginatedResponse<AuthzRoleDTO> getList(
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

        final PaginatedList<AuthzRoleDTO> result = authzRolesService.findPaginated(
                searchQuery, page, perPage,sort, order);
        return PaginatedResponse.create("roles", result, query);
    }

    @GET
    @ApiOperation(value = "Get a paginated list of users for a role")
    @Path("/{roleId}/assignees")
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresPermissions(RestPermissions.USERS_LIST)
    public PaginatedResponse<UserOverviewDTO> getUsersForRole(
            @ApiParam(name = "roleId") @PathParam("roleId") @NotEmpty String roleId,
            @ApiParam(name = "page") @QueryParam("page") @DefaultValue("1") int page,
            @ApiParam(name = "per_page") @QueryParam("per_page") @DefaultValue("50") int perPage,
            @ApiParam(name = "query") @QueryParam("query") @DefaultValue("") String query,
            @ApiParam(name = "sort",
                    value = "The field to sort the result on",
                    required = true,
                    allowableValues = "username,full_name,email")
            @DefaultValue(AuthzRoleDTO.FIELD_NAME) @QueryParam("sort") String sort,
            @ApiParam(name = "order", value = "The sort direction", allowableValues = "asc, desc")
            @DefaultValue("asc") @QueryParam("order") String order) {

        SearchQuery searchQuery;
        try {
            searchQuery = userSearchQueryParser.parse(query);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid argument in search query: " + e.getMessage());
        }

        final PaginatedList<UserOverviewDTO> result = paginatedUserService.findPaginatedByRole(
                searchQuery, page, perPage,sort, order, roleId);
        return PaginatedResponse.create("users", result, query);
    }

    @GET
    @ApiOperation(value = "Get a single role")
    @Path("{roleId}")
    @Produces(MediaType.APPLICATION_JSON)
    public AuthzRoleDTO get(@ApiParam(name = "roleId") @PathParam("roleId") @NotBlank String roleId) {
        checkPermission(RestPermissions.ROLES_READ, roleId);
        return authzRolesService.get(roleId).orElseThrow(
                () -> new NotFoundException("Could not find role with id: " + roleId));
    }

    @GET
    @ApiOperation(value = "Get a paginated list roles for a user")
    @Path("/rolesForUser/{username}")
    @RequiresPermissions(RestPermissions.ROLES_READ)
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

        final PaginatedList<AuthzRoleDTO> result = authzRolesService.findPaginatedForUser(
                searchQuery, page, perPage,sort, order, username);
        return PaginatedResponse.create("roles", result, query);
    }

    @PUT
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation("Add a user to a role")
    @AuditEvent(type = AuditEventTypes.USER_UPDATE)
    @Path("{roleId}/assignee/{username}")
    public void addUser(
            @ApiParam(name = "roleId") @PathParam("roleId") @NotBlank String roleId,
            @ApiParam(name = "username") @PathParam("username") @NotBlank String username) throws ValidationException {
        updateUserRole(roleId, username, Set::add);
    }

    @DELETE
    @ApiOperation("Remove a member to a team")
    @Path("{roleId}/assignee/{username}")
    @AuditEvent(type = AuditEventTypes.USER_UPDATE)
    public void removeUser(
            @ApiParam(name = "roleId") @PathParam("roleId") @NotBlank String roleId,
            @ApiParam(name = "username") @PathParam("username") @NotBlank String username) throws ValidationException {
        updateUserRole(roleId, username, Set::remove);
    }

    interface UpdateRoles {
        boolean update(Set<String> roles, String roleId);
    }

    private void updateUserRole(String roleId, String username, UpdateRoles rolesUpdater) throws ValidationException {
        checkPermission(USERS_EDIT, username);

        final User user = userService.load(username);
        if (user == null) {
            throw new NotFoundException("Cannot find user with name: " + username);
        }
        authzRolesService.get(roleId).orElseThrow(() -> new NotFoundException("Cannot find role with id: " + roleId));
        Set<String> roles = user.getRoleIds();
        rolesUpdater.update(roles, roleId);
        user.setRoleIds(roles);
        userService.save(user);
    }
}

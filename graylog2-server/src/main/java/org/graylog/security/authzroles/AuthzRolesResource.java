/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
package org.graylog.security.authzroles;

import com.codahale.metrics.annotation.Timed;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.NotAllowedException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.graylog2.shared.security.RestPermissions.USERS_ROLESEDIT;

@RequiresAuthentication
@Api(value = "Authorization/Roles", description = "Manage roles")
@Path("/authz/roles")
@Produces(MediaType.APPLICATION_JSON)
public class AuthzRolesResource extends RestResource {
    private static final Logger LOG = LoggerFactory.getLogger(RestResource.class);

    protected static final ImmutableMap<String, SearchQueryField> SEARCH_FIELD_MAPPING = ImmutableMap.<String, SearchQueryField>builder()
            .put(AuthzRoleDTO.FIELD_ID, SearchQueryField.create("_id", SearchQueryField.Type.ID))
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
        final Map<String, Set<Map<String, String>>> userRoleMap = userRoleContext(result);

        return PaginatedResponse.create("roles", result, query, ImmutableMap.of("users", userRoleMap));
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
                searchQuery, page, perPage,sort, order, ImmutableSet.of(roleId));
        final Set<String> roleIds = result.stream().flatMap(u -> u.roles().stream()).collect(Collectors.toSet());
        final Map<String, String> rolesMap = authzRolesService.findPaginatedByIds(
                new SearchQuery(""), 0, 0, AuthzRoleDTO.FIELD_NAME, "asc", roleIds)
                .stream().collect(Collectors.toMap(AuthzRoleDTO::id, AuthzRoleDTO::name));
        final List<UserOverviewDTO> users = result.stream().map(u -> {
            final Set<String> roleNames = u.roles().stream().map(rolesMap::get).collect(Collectors.toSet());
            return u.toBuilder().roles(roleNames).build();
        }).collect(Collectors.toList());

        final PaginatedList<UserOverviewDTO> enrichedResult = new PaginatedList<>(users, result.pagination().total(),
                result.pagination().page(), result.pagination().perPage());
        return PaginatedResponse.create("users", enrichedResult, query);
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
    @Path("/user/{username}")
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

        final User user = Optional.ofNullable(userService.load(username))
                .orElseThrow(() -> new NotFoundException("Couldn't find user: " + username));

        final PaginatedList<AuthzRoleDTO> result = authzRolesService.findPaginatedByIds(
                searchQuery, page, perPage,sort, order, user.getRoleIds());
        return PaginatedResponse.create("roles", result, query);
    }

    @PUT
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation("Add user to role")
    @AuditEvent(type = AuditEventTypes.ROLE_MEMBERSHIP_UPDATE)
    @Path("{roleId}/assignees")
    public void addUser(
            @ApiParam(name = "roleId") @PathParam("roleId") @NotBlank String roleId,
            @ApiParam(name = "usernames") Set<String> usernames) throws ValidationException {
        updateUserRole(roleId, usernames, Set::add);
    }

    @DELETE
    @ApiOperation("Remove user from role")
    @Path("{roleId}/assignee/{username}")
    @AuditEvent(type = AuditEventTypes.ROLE_MEMBERSHIP_DELETE)
    public void removeUser(
            @ApiParam(name = "roleId") @PathParam("roleId") @NotBlank String roleId,
            @ApiParam(name = "username") @PathParam("username") @NotBlank String username) throws ValidationException {
        updateUserRole(roleId, ImmutableSet.of(username), Set::remove);
    }

    interface UpdateRoles {
        boolean update(Set<String> roles, String roleId);
    }

    private void updateUserRole(String roleId, Set<String> usernames, UpdateRoles rolesUpdater) throws ValidationException {
        usernames.forEach(username -> {
            checkPermission(USERS_ROLESEDIT, username);

            final User user = userService.load(username);
            if (user == null) {
                throw new NotFoundException("Cannot find user with name: " + username);
            }
            authzRolesService.get(roleId).orElseThrow(() -> new NotFoundException("Cannot find role with id: " + roleId));
            Set<String> roles = user.getRoleIds();
            rolesUpdater.update(roles, roleId);
            user.setRoleIds(roles);
            try {
                userService.save(user);
            } catch (ValidationException e) {
                LOG.warn("Could not update user: {}", username);
            }
        });
    }

    @DELETE
    @Path("{roleId}")
    @AuditEvent(type = AuditEventTypes.ROLE_DELETE)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation("Delete a role")
    public void delete(@ApiParam(name = "roleId") @PathParam("roleId") @NotBlank String roleId) {
        checkPermission(RestPermissions.ROLES_EDIT);
        final AuthzRoleDTO roleDTO = authzRolesService.get(roleId).orElseThrow(
                () -> new NotFoundException("Could not delete role with id: " + roleId));
        if (roleDTO.readOnly()) {
            throw new NotAllowedException("Cannot delete read only role with id: " + roleId);
        }
        authzRolesService.delete(roleId);
    }


    private Map<String, Set<Map<String, String>>> userRoleContext(PaginatedList<AuthzRoleDTO> roles) {
        final PaginatedList<UserOverviewDTO> users = paginatedUserService.findPaginatedByRole(new SearchQuery(""),
                1,0, UserOverviewDTO.FIELD_USERNAME, "asc",
                roles.stream().map(AuthzRoleDTO::id).collect(Collectors.toSet()));
        final Map<String, Set<Map<String, String>>> userRoleMap = new HashMap<>(roles.size());
        roles.forEach(authzRoleDTO -> {
            final Set<Map<String, String>> userMap = users.stream().filter(u -> u.roles().contains(authzRoleDTO.id()))
                    .map(u -> ImmutableMap.of(UserOverviewDTO.FIELD_ID, Objects.requireNonNull(u.id()),
                            UserOverviewDTO.FIELD_USERNAME, u.username())).collect(Collectors.toSet());
            userRoleMap.put(authzRoleDTO.id(), userMap);
        });
        return userRoleMap;
    }
}

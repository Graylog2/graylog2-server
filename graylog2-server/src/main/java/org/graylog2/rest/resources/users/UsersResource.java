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
package org.graylog2.rest.resources.users;

import com.codahale.metrics.annotation.Timed;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.apache.shiro.authz.permission.WildcardPermission;
import org.graylog.security.UserContext;
import org.graylog.security.permissions.GRNPermission;
import org.graylog2.audit.AuditEventTypes;
import org.graylog2.audit.jersey.AuditEvent;
import org.graylog2.database.PaginatedList;
import org.graylog2.plugin.database.ValidationException;
import org.graylog2.plugin.database.users.User;
import org.graylog2.rest.models.PaginatedResponse;
import org.graylog2.rest.models.users.requests.ChangePasswordRequest;
import org.graylog2.rest.models.users.requests.ChangeUserRequest;
import org.graylog2.rest.models.users.requests.CreateUserRequest;
import org.graylog2.rest.models.users.requests.PermissionEditRequest;
import org.graylog2.rest.models.users.requests.Startpage;
import org.graylog2.rest.models.users.requests.UpdateUserPreferences;
import org.graylog2.rest.models.users.responses.Token;
import org.graylog2.rest.models.users.responses.TokenList;
import org.graylog2.rest.models.users.responses.TokenSummary;
import org.graylog2.rest.models.users.responses.UserList;
import org.graylog2.rest.models.users.responses.UserSummary;
import org.graylog2.search.SearchQuery;
import org.graylog2.search.SearchQueryField;
import org.graylog2.search.SearchQueryParser;
import org.graylog2.security.AccessToken;
import org.graylog2.security.AccessTokenService;
import org.graylog2.security.MongoDBSessionService;
import org.graylog2.security.MongoDbSession;
import org.graylog2.shared.rest.resources.RestResource;
import org.graylog2.shared.security.RestPermissions;
import org.graylog2.shared.users.Role;
import org.graylog2.shared.users.Roles;
import org.graylog2.shared.users.UserManagementService;
import org.graylog2.users.PaginatedUserService;
import org.graylog2.users.RoleService;
import org.graylog2.users.UserOverviewDTO;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.GET;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.maxBy;
import static org.graylog2.shared.security.RestPermissions.USERS_EDIT;
import static org.graylog2.shared.security.RestPermissions.USERS_PERMISSIONSEDIT;
import static org.graylog2.shared.security.RestPermissions.USERS_ROLESEDIT;
import static org.graylog2.shared.security.RestPermissions.USERS_TOKENCREATE;
import static org.graylog2.shared.security.RestPermissions.USERS_TOKENLIST;
import static org.graylog2.shared.security.RestPermissions.USERS_TOKENREMOVE;

@RequiresAuthentication
@Path("/users")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Api(value = "Users", description = "User accounts")
public class UsersResource extends RestResource {
    private static final Logger LOG = LoggerFactory.getLogger(RestResource.class);

    private final UserManagementService userManagementService;
    private final PaginatedUserService paginatedUserService;
    private final AccessTokenService accessTokenService;
    private final RoleService roleService;
    private final MongoDBSessionService sessionService;
    private final SearchQueryParser searchQueryParser;

    protected static final ImmutableMap<String, SearchQueryField> SEARCH_FIELD_MAPPING = ImmutableMap.<String, SearchQueryField>builder()
            .put(UserOverviewDTO.FIELD_USERNAME, SearchQueryField.create(UserOverviewDTO.FIELD_USERNAME))
            .put(UserOverviewDTO.FIELD_FULL_NAME, SearchQueryField.create(UserOverviewDTO.FIELD_FULL_NAME))
            .put(UserOverviewDTO.FIELD_EMAIL, SearchQueryField.create(UserOverviewDTO.FIELD_EMAIL))
            .build();

    @Inject
    public UsersResource(UserManagementService userManagementService,
                         PaginatedUserService paginatedUserService,
                         AccessTokenService accessTokenService,
                         RoleService roleService,
                         MongoDBSessionService sessionService) {
        this.userManagementService = userManagementService;
        this.accessTokenService = accessTokenService;
        this.roleService = roleService;
        this.sessionService = sessionService;
        this.paginatedUserService = paginatedUserService;
        this.searchQueryParser = new SearchQueryParser(UserOverviewDTO.FIELD_FULL_NAME, SEARCH_FIELD_MAPPING);
    }

    @GET
    @Deprecated
    @Path("{username}")
    @ApiOperation(value = "Get user details", notes = "The user's permissions are only included if a user asks for his " +
            "own account or for users with the necessary permissions to edit permissions.")
    @ApiResponses({
                          @ApiResponse(code = 404, message = "The user could not be found.")
                  })
    public UserSummary get(@ApiParam(name = "username", value = "The username to return information for.", required = true)
                           @PathParam("username") String username,
                           @Context UserContext userContext) {
        // If a user has permissions to edit another user's profile, it should be able to see it.
        // Reader users always have permissions to edit their own profile.
        if (!isPermitted(USERS_EDIT, username)) {
            throw new ForbiddenException("Not allowed to view user " + username);
        }

        final User user = userManagementService.load(username);
        if (user == null) {
            throw new NotFoundException("Couldn't find user " + username);
        }
        return returnSummary(userContext, user);
    }

    @GET
    @Path("id/{userId}")
    @ApiOperation(value = "Get user details by userId", notes = "The user's permissions are only included if a user asks for his " +
            "own account or for users with the necessary permissions to edit permissions.")
    @ApiResponses({
                          @ApiResponse(code = 404, message = "The user could not be found.")
                  })
    public UserSummary getbyId(@ApiParam(name = "userId", value = "The userId to return information for.", required = true)
                               @PathParam("userId") String userId,
                               @Context UserContext userContext) {

        final User user = loadUserById(userId);
        final String username = user.getName();
        // If a user has permissions to edit another user's profile, it should be able to see it.
        // Reader users always have permissions to edit their own profile.
        if (!isPermitted(USERS_EDIT, username)) {
            throw new ForbiddenException("Not allowed to view userId " + userId);
        }
        return returnSummary(userContext, user);
    }

    private UserSummary returnSummary(UserContext userContext, User user) {
        final String requestingUser = userContext.getUser().getId();
        final boolean isSelf = requestingUser.equals(user.getId());
        final boolean canEditUserPermissions = isPermitted(USERS_PERMISSIONSEDIT, user.getName());

        return toUserResponse(user, isSelf || canEditUserPermissions, AllUserSessions.create(sessionService));
    }

    @GET
    @Deprecated
    @RequiresPermissions(RestPermissions.USERS_LIST)
    @ApiOperation(value = "List all users", notes = "The permissions assigned to the users are always included.")
    public UserList listUsers() {
        final List<User> users = userManagementService.loadAll();
        final AllUserSessions sessions = AllUserSessions.create(sessionService);

        final List<UserSummary> resultUsers = Lists.newArrayListWithCapacity(users.size() + 1);
        userManagementService.getRootUser().ifPresent(adminUser ->
                resultUsers.add(toUserResponse(adminUser, sessions))
        );

        for (User user : users) {
            resultUsers.add(toUserResponse(user, sessions));
        }

        return UserList.create(resultUsers);
    }

    @GET
    @Timed
    @Path("/paginated")
    @ApiOperation(value = "Get paginated list of users")
    @RequiresPermissions(RestPermissions.USERS_LIST)
    @Produces(MediaType.APPLICATION_JSON)
    public PaginatedResponse<UserOverviewDTO> getPage(@ApiParam(name = "page") @QueryParam("page") @DefaultValue("1") int page,
                                                      @ApiParam(name = "per_page") @QueryParam("per_page") @DefaultValue("50") int perPage,
                                                      @ApiParam(name = "query") @QueryParam("query") @DefaultValue("") String query,
                                                      @ApiParam(name = "sort",
                                                                value = "The field to sort the result on",
                                                                required = true,
                                                                allowableValues = "title,description")
                                                      @DefaultValue(UserOverviewDTO.FIELD_FULL_NAME) @QueryParam("sort") String sort,
                                                      @ApiParam(name = "order", value = "The sort direction", allowableValues = "asc, desc")
                                                      @DefaultValue("asc") @QueryParam("order") String order) {

        SearchQuery searchQuery;
        final AllUserSessions sessions = AllUserSessions.create(sessionService);
        try {
            searchQuery = searchQueryParser.parse(query);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid argument in search query: " + e.getMessage());
        }

        final PaginatedList<UserOverviewDTO> result = paginatedUserService
                .findPaginated(searchQuery, page, perPage, sort, order);
        final Set<String> allRoleIds = result.stream().flatMap(userDTO -> {
            if (userDTO.roles() != null) {
                return userDTO.roles().stream();
            }
            return Stream.empty();
        }).collect(Collectors.toSet());

        Map<String, String> roleNameMap;
        try {
            roleNameMap = getRoleNameMap(allRoleIds);
        } catch (org.graylog2.database.NotFoundException e) {
            throw new NotFoundException("Couldn't find roles: " + e.getMessage());
        }

        final UserOverviewDTO adminUser = getAdminUserDTO(sessions);

        List<UserOverviewDTO> users = result.stream().map(userDTO -> {
            UserOverviewDTO.Builder builder = userDTO.toBuilder()
                    .fillSession(sessions.forUser(userDTO));
            if (userDTO.roles() != null) {
                builder.roles(userDTO.roles().stream().map(roleNameMap::get).collect(Collectors.toSet()));
            }
            return builder.build();
        }).collect(Collectors.toList());

        final PaginatedList<UserOverviewDTO> userOverviewDTOS = new PaginatedList<>(users, result.pagination().total(),
                result.pagination().page(), result.pagination().perPage());
        return PaginatedResponse.create("users", userOverviewDTOS, query, Collections.singletonMap("admin_user", adminUser));
    }

    @POST
    @RequiresPermissions(RestPermissions.USERS_CREATE)
    @ApiOperation("Create a new user account.")
    @ApiResponses({
                          @ApiResponse(code = 400, message = "Missing or invalid user details.")
                  })
    @AuditEvent(type = AuditEventTypes.USER_CREATE)
    public Response create(@ApiParam(name = "JSON body", value = "Must contain username, full_name, email, password and a list of permissions.", required = true)
                           @Valid @NotNull CreateUserRequest cr) throws ValidationException {
        if (userManagementService.load(cr.username()) != null) {
            final String msg = "Cannot create user " + cr.username() + ". Username is already taken.";
            LOG.error(msg);
            throw new BadRequestException(msg);
        }

        // Create user.
        User user = userManagementService.create();
        user.setName(cr.username());
        user.setPassword(cr.password());
        user.setFirstLastFullNames(cr.firstName(), cr.lastName());
        user.setEmail(cr.email());
        user.setPermissions(cr.permissions());
        setUserRoles(cr.roles(), user);

        if (cr.timezone() != null) {
            user.setTimeZone(cr.timezone());
        }

        final Long sessionTimeoutMs = cr.sessionTimeoutMs();
        if (sessionTimeoutMs != null) {
            user.setSessionTimeoutMs(sessionTimeoutMs);
        }

        final Startpage startpage = cr.startpage();
        if (startpage != null) {
            user.setStartpage(startpage.type(), startpage.id());
        }

        final String id = userManagementService.create(user);
        LOG.debug("Saved user {} with id {}", user.getName(), id);

        final URI userUri = getUriBuilderToSelf().path(UsersResource.class)
                .path("{username}")
                .build(user.getName());

        return Response.created(userUri).build();
    }

    private void setUserRoles(@Nullable List<String> roles, User user) {
        if (roles != null) {
            try {
                final Map<String, Role> nameMap = roleService.loadAllLowercaseNameMap();
                final Iterable<String> roleIds = Iterables.transform(roles, Roles.roleNameToIdFunction(nameMap));
                user.setRoleIds(Sets.newHashSet(roleIds));
            } catch (org.graylog2.database.NotFoundException e) {
                throw new InternalServerErrorException(e);
            }
        }
    }

    @PUT
    @Path("{userId}")
    @ApiOperation("Modify user details.")
    @ApiResponses({
                          @ApiResponse(code = 400, message = "Attempted to modify a read only user account (e.g. built-in or LDAP users)."),
                          @ApiResponse(code = 400, message = "Missing or invalid user details.")
                  })
    @AuditEvent(type = AuditEventTypes.USER_UPDATE)
    public void changeUser(@ApiParam(name = "userId", value = "The ID of the user to modify.", required = true)
                           @PathParam("userId") String userId,
                           @ApiParam(name = "JSON body", value = "Updated user information.", required = true)
                           @Valid @NotNull ChangeUserRequest cr) throws ValidationException {

        final User user = loadUserById(userId);
        final String username = user.getName();
        checkPermission(USERS_EDIT, username);

        if (user.isReadOnly()) {
            throw new BadRequestException("Cannot modify readonly user " + username);
        }
        // We only allow setting a subset of the fields in ChangeUserRequest
        if (!user.isExternalUser()) {
            if (cr.email() != null) {
                user.setEmail(cr.email());
            }
            if (cr.firstName() != null && cr.lastName() != null) {
                user.setFirstLastFullNames(cr.firstName(), cr.lastName());
            }
        }
        final boolean permitted = isPermitted(USERS_PERMISSIONSEDIT, user.getName());
        if (permitted && cr.permissions() != null) {
            user.setPermissions(getEffectiveUserPermissions(user, cr.permissions()));
        }

        if (isPermitted(USERS_ROLESEDIT, user.getName())) {
            setUserRoles(cr.roles(), user);
        }

        final String timezone = cr.timezone();
        if (timezone == null) {
            user.setTimeZone((String) null);
        } else {
            try {
                if (timezone.isEmpty()) {
                    user.setTimeZone((String) null);
                } else {
                    final DateTimeZone tz = DateTimeZone.forID(timezone);
                    user.setTimeZone(tz);
                }
            } catch (IllegalArgumentException e) {
                LOG.error("Invalid timezone '{}', ignoring it for user {}.", timezone, username);
            }
        }

        final Startpage startpage = cr.startpage();
        if (startpage != null) {
            user.setStartpage(startpage.type(), startpage.id());
        }

        if (isPermitted("*")) {
            final Long sessionTimeoutMs = cr.sessionTimeoutMs();
            if (sessionTimeoutMs != null && sessionTimeoutMs != 0) {
                user.setSessionTimeoutMs(sessionTimeoutMs);
            }
        }
        userManagementService.update(user);
    }

    @DELETE
    @Path("{username}")
    @RequiresPermissions(USERS_EDIT)
    @ApiOperation("Removes a user account.")
    @ApiResponses({@ApiResponse(code = 400, message = "When attempting to remove a read only user (e.g. built-in or LDAP user).")})
    @AuditEvent(type = AuditEventTypes.USER_DELETE)
    public void deleteUser(@ApiParam(name = "username", value = "The name of the user to delete.", required = true)
                           @PathParam("username") String username) {
        if (userManagementService.delete(username) == 0) {
            throw new NotFoundException("Couldn't find user " + username);
        }
    }

    @DELETE
    @Path("id/{userId}")
    @RequiresPermissions(USERS_EDIT)
    @ApiOperation("Removes a user account.")
    @ApiResponses({@ApiResponse(code = 400, message = "When attempting to remove a read only user (e.g. built-in or LDAP user).")})
    @AuditEvent(type = AuditEventTypes.USER_DELETE)
    public void deleteUserById(@ApiParam(name = "userId", value = "The id of the user to delete.", required = true)
                               @PathParam("userId") String userId) {
        if (userManagementService.deleteById(userId) == 0) {
            throw new NotFoundException("Couldn't find user " + userId);
        }
    }

    @PUT
    @Path("{username}/permissions")
    @RequiresPermissions(RestPermissions.USERS_PERMISSIONSEDIT)
    @ApiOperation("Update a user's permission set.")
    @ApiResponses({
                          @ApiResponse(code = 400, message = "Missing or invalid permission data.")
                  })
    @AuditEvent(type = AuditEventTypes.USER_PERMISSIONS_UPDATE)
    public void editPermissions(@ApiParam(name = "username", value = "The name of the user to modify.", required = true)
                                @PathParam("username") String username,
                                @ApiParam(name = "JSON body", value = "The list of permissions to assign to the user.", required = true)
                                @Valid @NotNull PermissionEditRequest permissionRequest) throws ValidationException {
        final User user = userManagementService.load(username);
        if (user == null) {
            throw new NotFoundException("Couldn't find user " + username);
        }

        user.setPermissions(getEffectiveUserPermissions(user, permissionRequest.permissions()));
        userManagementService.save(user);
    }

    @PUT
    @Path("{username}/preferences")
    @ApiOperation("Update a user's preferences set.")
    @ApiResponses({
                          @ApiResponse(code = 400, message = "Missing or invalid permission data.")
                  })
    @AuditEvent(type = AuditEventTypes.USER_PREFERENCES_UPDATE)
    public void savePreferences(@ApiParam(name = "username", value = "The name of the user to modify.", required = true)
                                @PathParam("username") String username,
                                @ApiParam(name = "JSON body", value = "The map of preferences to assign to the user.", required = true)
                                        UpdateUserPreferences preferencesRequest) throws ValidationException {
        final User user = userManagementService.load(username);
        checkPermission(RestPermissions.USERS_EDIT, username);

        if (user == null) {
            throw new NotFoundException("Couldn't find user " + username);
        }

        user.setPreferences(preferencesRequest.preferences());
        userManagementService.save(user);
    }

    @DELETE
    @Path("{username}/permissions")
    @RequiresPermissions(RestPermissions.USERS_PERMISSIONSEDIT)
    @ApiOperation("Revoke all permissions for a user without deleting the account.")
    @ApiResponses({
                          @ApiResponse(code = 500, message = "When saving the user failed.")
                  })
    @AuditEvent(type = AuditEventTypes.USER_PERMISSIONS_DELETE)
    public void deletePermissions(@ApiParam(name = "username", value = "The name of the user to modify.", required = true)
                                  @PathParam("username") String username) throws ValidationException {
        final User user = userManagementService.load(username);
        if (user == null) {
            throw new NotFoundException("Couldn't find user " + username);
        }
        user.setPermissions(Collections.emptyList());
        userManagementService.save(user);
    }

    @PUT
    @Path("{userId}/password")
    @ApiOperation("Update the password for a user.")
    @ApiResponses({
                          @ApiResponse(code = 204, message = "The password was successfully updated. Subsequent requests must be made with the new password."),
                          @ApiResponse(code = 400, message = "The new password is missing, or the old password is missing or incorrect."),
                          @ApiResponse(code = 403, message = "The requesting user has insufficient privileges to update the password for the given user."),
                          @ApiResponse(code = 404, message = "User does not exist.")
                  })
    @AuditEvent(type = AuditEventTypes.USER_PASSWORD_UPDATE)
    public void changePassword(
            @ApiParam(name = "userId", value = "The id of the user whose password to change.", required = true)
            @PathParam("userId") String userId,
            @ApiParam(name = "JSON body", value = "The old and new passwords.", required = true)
            @Valid ChangePasswordRequest cr) throws ValidationException {

        final User user = loadUserById(userId);
        final String username = user.getName();

        if (!getSubject().isPermitted(RestPermissions.USERS_PASSWORDCHANGE + ":" + username)) {
            throw new ForbiddenException("Not allowed to change password for user " + username);
        }
        if (user.isExternalUser()) {
            final String msg = "Cannot change password for external user.";
            LOG.error(msg);
            throw new ForbiddenException(msg);
        }

        boolean checkOldPassword = true;
        // users with the wildcard permission for password change do not have to supply the old password, unless they try to change their own password.
        // the rationale is to prevent accidental or malicious change of admin passwords (e.g. to prevent locking out legitimate admins)
        if (getSubject().isPermitted(RestPermissions.USERS_PASSWORDCHANGE + ":*")) {
            if (username.equals(getSubject().getPrincipal())) {
                LOG.debug("User {} is allowed to change the password of any user, but attempts to change own password. Must supply the old password.", getSubject().getPrincipal());
                checkOldPassword = true;
            } else {
                LOG.debug("User {} is allowed to change the password for any user, including {}, ignoring old password", getSubject().getPrincipal(), username);
                checkOldPassword = false;
            }
        }

        boolean changeAllowed = false;
        if (checkOldPassword) {
            if (userManagementService.isUserPassword(user, cr.oldPassword())) {
                changeAllowed = true;
            }
        } else {
            changeAllowed = true;
        }

        if (changeAllowed) {
            if (checkOldPassword) {
                userManagementService.changePassword(user, cr.oldPassword(), cr.password());
            } else {
                userManagementService.changePassword(user, cr.password());
            }
        } else {
            throw new BadRequestException("Old password is missing or incorrect.");
        }
    }

    @PUT
    @Path("{userId}/status/{newStatus}")
    @Consumes(MediaType.WILDCARD)
    @ApiOperation("Update the account status for a user")
    @AuditEvent(type = AuditEventTypes.USER_UPDATE)
    public Response updateAccountStatus(
            @ApiParam(name = "userId", value = "The id of the user whose status to change.", required = true) @PathParam("userId") String userId,
            @ApiParam(name = "newStatus", value = "The account status to be set", required = true,
                    defaultValue = "enabled", allowableValues = "enabled,disabled,deleted")
            @PathParam("newStatus") @NotBlank String newStatusString) throws ValidationException {

        final User.AccountStatus newStatus = User.AccountStatus.valueOf(newStatusString.toUpperCase(Locale.US));
        final User user = loadUserById(userId);
        checkPermission(RestPermissions.USERS_EDIT, user.getName());
        final User.AccountStatus oldStatus = user.getAccountStatus();

        if (oldStatus.equals(newStatus)) {
            return Response.notModified().build();
        }

        userManagementService.setUserStatus(user, newStatus);
        return Response.ok().build();
    }

    @GET
    @Path("{userId}/tokens")
    @ApiOperation("Retrieves the list of access tokens for a user")
    public TokenList listTokens(@ApiParam(name = "userId", required = true)
                                @PathParam("userId") String userId) {
        final User user = loadUserById(userId);
        final String username = user.getName();

        if (!isPermitted(USERS_TOKENLIST, username)) {
            throw new ForbiddenException("Not allowed to list tokens for user " + username);
        }

        final ImmutableList.Builder<TokenSummary> tokenList = ImmutableList.builder();
        for (AccessToken token : accessTokenService.loadAll(user.getName())) {
            tokenList.add(TokenSummary.create(token.getId(), token.getName(), token.getLastAccess()));
        }

        return TokenList.create(tokenList.build());
    }

    @POST
    @Path("{userId}/tokens/{name}")
    @ApiOperation("Generates a new access token for a user")
    @AuditEvent(type = AuditEventTypes.USER_ACCESS_TOKEN_CREATE)
    public Token generateNewToken(
            @ApiParam(name = "userId", required = true) @PathParam("userId") String userId,
            @ApiParam(name = "name", value = "Descriptive name for this token (e.g. 'cronjob') ", required = true) @PathParam("name") String name,
            @ApiParam(name = "JSON Body", value = "Placeholder because POST requests should have a body. Set to '{}', the content will be ignored.", defaultValue = "{}") String body) {
        final User user = loadUserById(userId);
        final String username = user.getName();
        if (!isPermitted(USERS_TOKENCREATE, username)) {
            throw new ForbiddenException("Not allowed to create tokens for user " + username);
        }
        final AccessToken accessToken = accessTokenService.create(user.getName(), name);

        return Token.create(accessToken.getId(), accessToken.getName(), accessToken.getToken(), accessToken.getLastAccess());
    }

    @DELETE
    @Path("{userId}/tokens/{idOrToken}")
    @ApiOperation("Removes a token for a user")
    @AuditEvent(type = AuditEventTypes.USER_ACCESS_TOKEN_DELETE)
    public void revokeToken(
            @ApiParam(name = "userId", required = true) @PathParam("userId") String userId,
            @ApiParam(name = "idOrToken", required = true) @PathParam("idOrToken") String idOrToken) {
        final User user = loadUserById(userId);
        final String username = user.getName();
        if (!isPermitted(USERS_TOKENREMOVE, username)) {
            throw new ForbiddenException("Not allowed to remove tokens for user " + username);
        }

        // The endpoint supports both, deletion by token ID and deletion by using the token value itself.
        // The latter should not be used anymore because the plain text token will be part of the URL and URLs
        // will most probably be logged. We keep the old behavior for backwards compatibility.
        // TODO: Remove support for old behavior in 4.0
        final AccessToken accessToken = Optional.ofNullable(accessTokenService.loadById(idOrToken))
                .orElse(accessTokenService.load(idOrToken));

        if (accessToken != null) {
            accessTokenService.destroy(accessToken);
        } else {
            throw new NotFoundException("Couldn't find access token for user " + username);
        }
    }

    private User loadUserById(String userId) {
        final User user = userManagementService.loadById(userId);
        if (user == null) {
            throw new NotFoundException("Couldn't find user with ID <" + userId + ">");
        }
        return user;
    }

    private UserSummary toUserResponse(User user, AllUserSessions sessions) {
        return toUserResponse(user, true, sessions);
    }

    private UserSummary toUserResponse(User user, boolean includePermissions, AllUserSessions sessions) {
        final Set<String> roleIds = user.getRoleIds();
        Set<String> roleNames = Collections.emptySet();

        if (!roleIds.isEmpty()) {
            roleNames = userManagementService.getRoleNames(user);

            if (roleNames.isEmpty()) {
                LOG.error("Unable to load role names for role IDs {} for user {}", roleIds, user);
            }
        }

        boolean sessionActive = false;
        Date lastActivity = null;
        String clientAddress = null;
        final Optional<MongoDbSession> mongoDbSession = sessions.forUser(user);
        if (mongoDbSession.isPresent()) {
            final MongoDbSession session = mongoDbSession.get();
            sessionActive = true;
            lastActivity = session.getLastAccessTime();
            clientAddress = session.getHost();
        }
        List<WildcardPermission> wildcardPermissions;
        List<GRNPermission> grnPermissions;
        if (includePermissions) {
            wildcardPermissions = userManagementService.getWildcardPermissionsForUser(user);
            grnPermissions = userManagementService.getGRNPermissionsForUser(user);
        } else {
            wildcardPermissions = ImmutableList.of();
            grnPermissions = ImmutableList.of();
        }

        return UserSummary.create(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getFirstName().orElse(null),
                user.getLastName().orElse(null),
                user.getFullName(),
                wildcardPermissions,
                grnPermissions,
                user.getPreferences(),
                user.getTimeZone() == null ? null : user.getTimeZone().getID(),
                user.getSessionTimeoutMs(),
                user.isReadOnly(),
                user.isExternalUser(),
                user.getStartpage(),
                roleNames,
                sessionActive,
                lastActivity,
                clientAddress,
                user.getAccountStatus()
        );
    }

    // Filter the permissions granted by roles from the permissions list
    private List<String> getEffectiveUserPermissions(final User user, final List<String> permissions) {
        final List<String> effectivePermissions = Lists.newArrayList(permissions);
        effectivePermissions.removeAll(userManagementService.getUserPermissionsFromRoles(user));
        return effectivePermissions;
    }

    private Map<String, String> getRoleNameMap(Set<String> roleIds) throws org.graylog2.database.NotFoundException {
        final Map<String, Role> roleMap = roleService.findIdMap(roleIds);
        final Map<String, String> result = new HashMap<>(roleMap.size());
        roleMap.forEach((key, value) -> result.put(key, value.getName()));
        return result;
    }

    private UserOverviewDTO getAdminUserDTO(AllUserSessions sessions) {
        final Optional<User> optionalAdmin = userManagementService.getRootUser();
        if (!optionalAdmin.isPresent()) {
            return null;
        }
        final User admin = optionalAdmin.get();
        final Set<String> adminRoles = userManagementService.getRoleNames(admin);
        final Optional<MongoDbSession> lastSession = sessions.forUser(admin);
        return UserOverviewDTO.builder()
                .username(admin.getName())
                .fullName(admin.getFullName())
                .email(admin.getEmail())
                .externalUser(admin.isExternalUser())
                .readOnly(admin.isReadOnly())
                .id(admin.getId())
                .fillSession(lastSession)
                .roles(adminRoles)
                .build();
    }

    private static class AllUserSessions {
        private final Map<String, Optional<MongoDbSession>> sessions;

        public static AllUserSessions create(MongoDBSessionService sessionService) {
            return new AllUserSessions(sessionService.loadAll());
        }

        private AllUserSessions(Collection<MongoDbSession> sessions) {
            this.sessions = getLastSessionForUser(sessions);
        }

        public Optional<MongoDbSession> forUser(User user) {
            return sessions.getOrDefault(user.getId(), Optional.empty());
        }

        public Optional<MongoDbSession> forUser(UserOverviewDTO user) {
            return sessions.getOrDefault(user.id(), Optional.empty());
        }

        // Among all active sessions, find the last recently used for each user
        private Map<String, Optional<MongoDbSession>> getLastSessionForUser(Collection<MongoDbSession> sessions) {
            //noinspection OptionalGetWithoutIsPresent
            return sessions.stream()
                    .filter(s -> s.getUserIdAttribute().isPresent())
                    .collect(groupingBy(s -> s.getUserIdAttribute().get(),
                            maxBy(Comparator.comparing(MongoDbSession::getLastAccessTime))));
        }
    }
}

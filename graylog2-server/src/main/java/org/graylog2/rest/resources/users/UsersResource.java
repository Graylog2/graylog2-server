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
package org.graylog2.rest.resources.users;

import com.google.common.collect.ImmutableList;
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
import org.graylog2.Configuration;
import org.graylog2.plugin.database.ValidationException;
import org.graylog2.plugin.database.users.User;
import org.graylog2.rest.models.users.requests.ChangePasswordRequest;
import org.graylog2.rest.models.users.requests.ChangeUserRequest;
import org.graylog2.rest.models.users.requests.CreateUserRequest;
import org.graylog2.rest.models.users.requests.PermissionEditRequest;
import org.graylog2.rest.models.users.requests.Startpage;
import org.graylog2.rest.models.users.requests.UpdateUserPreferences;
import org.graylog2.rest.models.users.responses.Token;
import org.graylog2.rest.models.users.responses.TokenList;
import org.graylog2.rest.models.users.responses.UserList;
import org.graylog2.rest.models.users.responses.UserSummary;
import org.graylog2.security.AccessToken;
import org.graylog2.security.AccessTokenService;
import org.graylog2.shared.rest.resources.RestResource;
import org.graylog2.shared.security.RestPermissions;
import org.graylog2.shared.users.Role;
import org.graylog2.shared.users.Roles;
import org.graylog2.shared.users.UserService;
import org.graylog2.users.RoleService;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.GET;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.MoreObjects.firstNonNull;
import static org.graylog2.shared.security.RestPermissions.USERS_EDIT;
import static org.graylog2.shared.security.RestPermissions.USERS_PERMISSIONSEDIT;
import static org.graylog2.shared.security.RestPermissions.USERS_ROLESEDIT;

@RequiresAuthentication
@Path("/users")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Api(value = "Users", description = "User accounts")
public class UsersResource extends RestResource {
    private static final Logger LOG = LoggerFactory.getLogger(RestResource.class);

    private final UserService userService;
    private final AccessTokenService accessTokenService;
    private final RoleService roleService;
    private final Configuration configuration;

    @Inject
    public UsersResource(UserService userService,
                         AccessTokenService accessTokenService,
                         RoleService roleService,
                         Configuration configuration) {
        this.userService = userService;
        this.accessTokenService = accessTokenService;
        this.roleService = roleService;
        this.configuration = configuration;
    }

    @GET
    @Path("{username}")
    @ApiOperation(value = "Get user details", notes = "The user's permissions are only included if a user asks for his " +
            "own account or for users with the necessary permissions to edit permissions.")
    @ApiResponses({
            @ApiResponse(code = 404, message = "The user could not be found.")
    })
    public UserSummary get(@ApiParam(name = "username", value = "The username to return information for.", required = true)
                           @PathParam("username") String username) {
        final org.graylog2.plugin.database.users.User user = userService.load(username);
        if (user == null) {
            throw new NotFoundException();
        }
        // if the requested username does not match the authenticated user, then we don't return permission information
        final boolean allowedToSeePermissions = isPermitted(RestPermissions.USERS_PERMISSIONSEDIT, username);
        final boolean permissionsAllowed = getSubject().getPrincipal().toString().equals(username) || allowedToSeePermissions;

        return toUserResponse(user, permissionsAllowed);
    }

    @GET
    @RequiresPermissions(RestPermissions.USERS_LIST)
    @ApiOperation(value = "List all users", notes = "The permissions assigned to the users are always included.")
    public UserList listUsers() {
        final List<org.graylog2.plugin.database.users.User> users = userService.loadAll();
        final List<UserSummary> resultUsers = Lists.newArrayListWithCapacity(users.size() + 1);
        resultUsers.add(toUserResponse(userService.getAdminUser()));

        for (org.graylog2.plugin.database.users.User user : users) {
            resultUsers.add(toUserResponse(user));
        }

        return UserList.create(resultUsers);
    }

    @POST
    @RequiresPermissions(RestPermissions.USERS_CREATE)
    @ApiOperation("Create a new user account.")
    @ApiResponses({
            @ApiResponse(code = 400, message = "Missing or invalid user details.")
    })
    public Response create(@ApiParam(name = "JSON body", value = "Must contain username, full_name, email, password and a list of permissions.", required = true)
                           @Valid @NotNull CreateUserRequest cr) throws ValidationException {
        if (userService.load(cr.username()) != null) {
            final String msg = "Cannot create user " + cr.username() + ". Username is already taken.";
            LOG.error(msg);
            throw new BadRequestException(msg);
        }

        // Create user.
        org.graylog2.plugin.database.users.User user = userService.create();
        user.setName(cr.username());
        user.setPassword(cr.password());
        user.setFullName(cr.fullName());
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

        final String id = userService.save(user);
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
    @Path("{username}")
    @ApiOperation("Modify user details.")
    @ApiResponses({
            @ApiResponse(code = 400, message = "Attempted to modify a read only user account (e.g. built-in or LDAP users)."),
            @ApiResponse(code = 400, message = "Missing or invalid user details.")
    })
    public void changeUser(@ApiParam(name = "username", value = "The name of the user to modify.", required = true)
                           @PathParam("username") String username,
                           @ApiParam(name = "JSON body", value = "Updated user information.", required = true)
                           @Valid @NotNull ChangeUserRequest cr) throws ValidationException {
        checkPermission(USERS_EDIT, username);

        final org.graylog2.plugin.database.users.User user = userService.load(username);
        if (user == null) {
            throw new NotFoundException();
        }

        if (user.isReadOnly()) {
            throw new BadRequestException("Cannot modify readonly user " + username);
        }
        // we only allow setting a subset of the fields in CreateStreamRuleRequest
        if (cr.email() != null) {
            user.setEmail(cr.email());
        }
        if (cr.fullName() != null) {
            user.setFullName(cr.fullName());
        }
        final boolean permitted = isPermitted(USERS_PERMISSIONSEDIT, user.getName());
        if (permitted && cr.permissions() != null) {
            user.setPermissions(cr.permissions());
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
        userService.save(user);
    }

    @DELETE
    @Path("{username}")
    @RequiresPermissions(USERS_EDIT)
    @ApiOperation("Removes a user account.")
    @ApiResponses({@ApiResponse(code = 400, message = "When attempting to remove a read only user (e.g. built-in or LDAP user).")})
    public void deleteUser(@ApiParam(name = "username", value = "The name of the user to delete.", required = true)
                           @PathParam("username") String username) {
        if (userService.delete(username) == 0) {
            throw new NotFoundException();
        }
    }

    @PUT
    @Path("{username}/permissions")
    @RequiresPermissions(RestPermissions.USERS_PERMISSIONSEDIT)
    @ApiOperation("Update a user's permission set.")
    @ApiResponses({
            @ApiResponse(code = 400, message = "Missing or invalid permission data.")
    })
    public void editPermissions(@ApiParam(name = "username", value = "The name of the user to modify.", required = true)
                                @PathParam("username") String username,
                                @ApiParam(name = "JSON body", value = "The list of permissions to assign to the user.", required = true)
                                @Valid @NotNull PermissionEditRequest permissionRequest) throws ValidationException {
        final org.graylog2.plugin.database.users.User user = userService.load(username);
        if (user == null) {
            throw new NotFoundException();
        }

        user.setPermissions(permissionRequest.permissions());
        userService.save(user);
    }

    @PUT
    @Path("{username}/preferences")
    @ApiOperation("Update a user's preferences set.")
    @ApiResponses({
            @ApiResponse(code = 400, message = "Missing or invalid permission data.")
    })
    public void savePreferences(@ApiParam(name = "username", value = "The name of the user to modify.", required = true)
                                @PathParam("username") String username,
                                @ApiParam(name = "JSON body", value = "The map of preferences to assign to the user.", required = true)
                                UpdateUserPreferences preferencesRequest) throws ValidationException {
        final org.graylog2.plugin.database.users.User user = userService.load(username);
        checkPermission(RestPermissions.USERS_EDIT, username);

        if (user == null) {
            throw new NotFoundException();
        }

        user.setPreferences(preferencesRequest.preferences());
        userService.save(user);
    }

    @DELETE
    @Path("{username}/permissions")
    @RequiresPermissions(RestPermissions.USERS_PERMISSIONSEDIT)
    @ApiOperation("Revoke all permissions for a user without deleting the account.")
    @ApiResponses({
            @ApiResponse(code = 500, message = "When saving the user failed.")
    })
    public void deletePermissions(@ApiParam(name = "username", value = "The name of the user to modify.", required = true)
                                  @PathParam("username") String username) throws ValidationException {
        final org.graylog2.plugin.database.users.User user = userService.load(username);
        if (user == null) {
            throw new NotFoundException();
        }
        user.setPermissions(Collections.<String>emptyList());
        userService.save(user);
    }

    @PUT
    @Path("{username}/password")
    @ApiOperation("Update the password for a user.")
    @ApiResponses({
            @ApiResponse(code = 204, message = "The password was successfully updated. Subsequent requests must be made with the new password."),
            @ApiResponse(code = 400, message = "If the old or new password is missing."),
            @ApiResponse(code = 403, message = "If the requesting user has insufficient privileges to update the password for the given user or the old password was wrong."),
            @ApiResponse(code = 404, message = "If the user does not exist.")
    })
    public void changePassword(
            @ApiParam(name = "username", value = "The name of the user whose password to change.", required = true)
            @PathParam("username") String username,
            @ApiParam(name = "JSON body", value = "The old and new passwords.", required = true)
            @Valid ChangePasswordRequest cr) throws ValidationException {

        final org.graylog2.plugin.database.users.User user = userService.load(username);
        if (user == null) {
            throw new NotFoundException();
        }

        if (!getSubject().isPermitted(RestPermissions.USERS_PASSWORDCHANGE + ":" + user.getName())) {
            throw new ForbiddenException();
        }
        if (user.isExternalUser()) {
            final String msg = "Cannot change password for LDAP user.";
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
            if (user.isUserPassword(cr.oldPassword())) {
                changeAllowed = true;
            }
        } else {
            changeAllowed = true;
        }

        if (changeAllowed) {
            user.setPassword(cr.password());
            userService.save(user);
        } else {
            throw new ForbiddenException();
        }
    }

    @GET
    @Path("{username}/tokens")
    @RequiresPermissions(RestPermissions.USERS_TOKENLIST)
    @ApiOperation("Retrieves the list of access tokens for a user")
    public TokenList listTokens(@ApiParam(name = "username", required = true)
                                @PathParam("username") String username) {
        final org.graylog2.plugin.database.users.User user = _tokensCheckAndLoadUser(username);

        final ImmutableList.Builder<Token> tokenList = ImmutableList.builder();
        for (AccessToken token : accessTokenService.loadAll(user.getName())) {
            tokenList.add(Token.create(token.getName(), token.getToken(), token.getLastAccess()));
        }

        return TokenList.create(tokenList.build());
    }

    @POST
    @Path("{username}/tokens/{name}")
    @RequiresPermissions(RestPermissions.USERS_TOKENCREATE)
    @ApiOperation("Generates a new access token for a user")
    public Token generateNewToken(
            @ApiParam(name = "username", required = true) @PathParam("username") String username,
            @ApiParam(name = "name", value = "Descriptive name for this token (e.g. 'cronjob') ", required = true) @PathParam("name") String name) {
        final org.graylog2.plugin.database.users.User user = _tokensCheckAndLoadUser(username);
        final AccessToken accessToken = accessTokenService.create(user.getName(), name);

        return Token.create(accessToken.getName(), accessToken.getToken(), accessToken.getLastAccess());
    }

    @DELETE
    @RequiresPermissions(RestPermissions.USERS_TOKENREMOVE)
    @Path("{username}/tokens/{token}")
    @ApiOperation("Removes a token for a user")
    public void revokeToken(
            @ApiParam(name = "username", required = true) @PathParam("username") String username,
            @ApiParam(name = "access token", required = true) @PathParam("token") String token) {
        final org.graylog2.plugin.database.users.User user = _tokensCheckAndLoadUser(username);
        final AccessToken accessToken = accessTokenService.load(token);

        if (accessToken != null) {
            accessTokenService.destroy(accessToken);
        } else {
            throw new NotFoundException();
        }
    }

    private org.graylog2.plugin.database.users.User _tokensCheckAndLoadUser(String username) {
        final org.graylog2.plugin.database.users.User user = userService.load(username);
        if (user == null) {
            throw new NotFoundException("Unknown user " + username);
        }
        if (!getSubject().getPrincipal().equals(username)) {
            throw new ForbiddenException("Cannot access other people's tokens.");
        }
        return user;
    }

    private UserSummary toUserResponse(org.graylog2.plugin.database.users.User user) {
        return toUserResponse(user, true);
    }

    private UserSummary toUserResponse(org.graylog2.plugin.database.users.User user, boolean includePermissions) {
        final Set<String> roleIds = user.getRoleIds();
        Set<String> roleNames = Collections.emptySet();

        if (!roleIds.isEmpty()) {
            roleNames = userService.getRoleNames(user);

            if (roleNames.isEmpty()) {
                LOG.error("Unable to load role names for role IDs {} for user {}", roleIds, user);
            }
        }

        return UserSummary.create(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getFullName(),
                includePermissions ? userService.getPermissionsForUser(user) : Collections.<String>emptyList(),
                user.getPreferences(),
                firstNonNull(user.getTimeZone(), DateTimeZone.UTC).getID(),
                user.getSessionTimeoutMs(),
                user.isReadOnly(),
                user.isExternalUser(),
                user.getStartpage(),
                roleNames
        );
    }
}

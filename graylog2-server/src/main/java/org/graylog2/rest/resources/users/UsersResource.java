/**
 * This file is part of Graylog2.
 *
 * Graylog2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog2.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.rest.resources.users;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.graylog2.Configuration;
import org.graylog2.database.ValidationException;
import org.graylog2.rest.resources.RestResource;
import org.graylog2.rest.resources.users.requests.ChangePasswordRequest;
import org.graylog2.rest.resources.users.requests.CreateRequest;
import org.graylog2.rest.resources.users.requests.PermissionEditRequest;
import org.graylog2.security.AccessToken;
import org.graylog2.security.AccessTokenService;
import org.graylog2.security.RestPermissions;
import org.graylog2.users.User;
import org.graylog2.users.UserService;
import org.hibernate.validator.constraints.NotEmpty;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.google.common.base.MoreObjects.firstNonNull;
import static org.graylog2.security.RestPermissions.USERS_EDIT;
import static org.graylog2.security.RestPermissions.USERS_PERMISSIONSEDIT;

@RequiresAuthentication
@Path("/users")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Api(value = "Users", description = "User accounts")
public class UsersResource extends RestResource {

    private static final Logger LOG = LoggerFactory.getLogger(RestResource.class);

    private final UserService userService;
    private final AccessTokenService accessTokenService;
    private final Configuration configuration;

    @Inject
    public UsersResource(UserService userService,
                         AccessTokenService accessTokenService,
                         Configuration configuration) {
        this.userService = userService;
        this.accessTokenService = accessTokenService;
        this.configuration = configuration;
    }

    @GET
    @Path("{username}")
    @ApiOperation(value = "Get user details", notes = "The user's permissions are only included if a user asks for his " +
            "own account or for users with the necessary permissions to edit permissions.")
    @ApiResponses({
            @ApiResponse(code = 404, message = "The user could not be found.")
    })
    public Map<String, Object> get(@ApiParam(name = "username", value = "The username to return information for.", required = true)
                                   @PathParam("username") String username) {
        final User user = userService.load(username);
        if (user == null) {
            throw new NotFoundException();
        }
        // if the requested username does not match the authenticated user, then we don't return permission information
        final boolean allowedToSeePermissions = isPermitted(RestPermissions.USERS_PERMISSIONSEDIT, username);
        final boolean permissionsAllowed = getSubject().getPrincipal().toString().equals(username) || allowedToSeePermissions;

        return toMap(user, permissionsAllowed);
    }

    @GET
    @RequiresPermissions(RestPermissions.USERS_LIST)
    @ApiOperation(value = "List all users", notes = "The permissions assigned to the users are always included.")
    public Map<String, List<Map<String, Object>>> listUsers() {
        final List<User> users = userService.loadAll();
        final List<Map<String, Object>> resultUsers = Lists.newArrayList();
        resultUsers.add(toMap(userService.getAdminUser()));
        for (User user : users) {
            resultUsers.add(toMap(user));
        }

        return ImmutableMap.of("users", resultUsers);
    }

    @POST
    @RequiresPermissions(RestPermissions.USERS_CREATE)
    @ApiOperation("Create a new user account.")
    @ApiResponses({
            @ApiResponse(code = 400, message = "Missing or invalid user details.")
    })
    public Response create(@ApiParam(name = "JSON body", value = "Must contain username, full_name, email, password and a list of permissions.", required = true)
                           @Valid @NotNull CreateRequest cr) throws ValidationException {
        if (userService.load(cr.username) != null) {
            LOG.error("Cannot create user {}: username is already taken.", cr.username);
            throw new BadRequestException();
        }

        // Create user.
        User user = userService.create();
        user.setName(cr.username);
        user.setPassword(cr.password, configuration.getPasswordSecret());
        user.setFullName(cr.fullname);
        user.setEmail(cr.email);
        user.setPermissions(cr.permissions);

        if (cr.timezone != null) {
            user.setTimeZone(cr.timezone);
        }
        if (cr.sessionTimeoutMs != null) {
            user.setSessionTimeoutMs(cr.sessionTimeoutMs);
        }

        final String id = userService.save(user);
        LOG.debug("Saved user {} with id {}", user.getName(), id);

        final URI userUri = UriBuilder.fromResource(UsersResource.class)
                .path("{username}")
                .build(user.getName());

        return Response.created(userUri).build();
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
                           @Valid @NotNull CreateRequest cr) throws ValidationException {
        checkPermission(USERS_EDIT, username);

        final User user = userService.load(username);
        if (user == null) {
            throw new NotFoundException();
        }

        if (user.isReadOnly()) {
            throw new BadRequestException("Cannot modify readonly user " + username);
        }
        // we only allow setting a subset of the fields in CreateStreamRuleRequest
        if (cr.email != null) {
            user.setEmail(cr.email);
        }
        if (cr.fullname != null) {
            user.setFullName(cr.fullname);
        }
        final boolean permitted = isPermitted(USERS_PERMISSIONSEDIT, user.getName());
        if (permitted && cr.permissions != null) {
            user.setPermissions(cr.permissions);
        }
        if (cr.timezone == null) {
            user.setTimeZone((String) null);
        } else {
            try {
                if (cr.timezone.isEmpty()) {
                    user.setTimeZone((String) null);
                } else {
                    final DateTimeZone tz = DateTimeZone.forID(cr.timezone);
                    user.setTimeZone(tz);
                }
            } catch (IllegalArgumentException e) {
                LOG.error("Invalid timezone '{}', ignoring it for user {}.", cr.timezone, username);
            }
        }

        if (cr.startpage != null) {
            user.setStartpage(cr.startpage.type, cr.startpage.id);
        }
        if (isPermitted("*")) {
            if (cr.sessionTimeoutMs != null && cr.sessionTimeoutMs != 0) {
                user.setSessionTimeoutMs(cr.sessionTimeoutMs);
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
        final User user = userService.load(username);
        if (user == null) {
            throw new NotFoundException();
        }

        if (user.isReadOnly()) {
            throw new BadRequestException("Cannot delete readonly user " + username);
        }

        userService.destroy(user);
    }

    @PUT
    @Path("{username}/permissions")
    @RequiresPermissions(RestPermissions.USERS_PERMISSIONSEDIT)
    @ApiOperation("Update a user's permission set.")
    @ApiResponses({
            @ApiResponse(code = 400, message = "Missing or invalid permission data.")
    })
    public void editPermissions(
            @ApiParam(name = "username", value = "The name of the user to modify.", required = true) @PathParam("username") String username,
            @ApiParam(name = "JSON body", value = "The list of permissions to assign to the user.", required = true)
            @Valid @NotNull PermissionEditRequest permissionRequest) throws ValidationException {
        final User user = userService.load(username);
        if (user == null) {
            throw new NotFoundException();
        }

        user.setPermissions(permissionRequest.permissions);
        userService.save(user);
    }

    @PUT
    @Path("{username}/preferences")
    @RequiresPermissions(USERS_EDIT)
    @ApiOperation("Update a user's preferences set.")
    @ApiResponses({
            @ApiResponse(code = 400, message = "Missing or invalid permission data.")
    })
    public void savePreferences(
            @ApiParam(name = "username", value = "The name of the user to modify.", required = true) @PathParam("username") String username,
            @ApiParam(name = "JSON body", value = "The map of preferences to assign to the user.", required = true)
            @NotEmpty Map<String, Object> preferencesRequest) throws ValidationException {
        final User user = userService.load(username);

        if (user == null) {
            throw new NotFoundException();
        }

        user.setPreferences(preferencesRequest);
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
        final User user = userService.load(username);
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
            @ApiResponse(code = 201, message = "The password was successfully updated. Subsequent requests must be made with the new password."),
            @ApiResponse(code = 400, message = "If the old or new password is missing."),
            @ApiResponse(code = 403, message = "If the requesting user has insufficient privileges to update the password for the given user or the old password was wrong."),
            @ApiResponse(code = 404, message = "If the user does not exist.")
    })
    public void changePassword(
            @ApiParam(name = "username", value = "The name of the user whose password to change.", required = true)
            @PathParam("username") String username,
            @ApiParam(name = "JSON body", value = "The old and new passwords.", required = true)
            @Valid ChangePasswordRequest cr) {

        final User user = userService.load(username);
        if (user == null) {
            throw new NotFoundException();
        }

        if (!getSubject().isPermitted(RestPermissions.USERS_PASSWORDCHANGE + ":" + user.getName())) {
            throw new ForbiddenException();
        }
        if (user.isExternalUser()) {
            LOG.error("Cannot change password for LDAP user.");
            throw new ForbiddenException();
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
        final String secret = configuration.getPasswordSecret();
        if (checkOldPassword) {
            if (cr.old_password == null) {
                LOG.info("Changing password for user {} must supply the old password.", username);
                throw new BadRequestException();
            }
            if (user.isUserPassword(cr.old_password, secret)) {
                changeAllowed = true;
            }
        } else {
            changeAllowed = true;
        }

        if (changeAllowed) {
            user.setPassword(cr.password, secret);
            try {
                userService.save(user);
            } catch (ValidationException e) {
                throw new BadRequestException("Validation error for " + username, e);
            }
        } else {
            throw new ForbiddenException();
        }
    }

    @GET
    @Path("{username}/tokens")
    @RequiresPermissions(RestPermissions.USERS_TOKENLIST)
    @ApiOperation("Retrieves the list of access tokens for a user")
    public TokenList listTokens(@ApiParam(name = "username", required = true) @PathParam("username") String username) {
        final User user = _tokensCheckAndLoadUser(username);
        final TokenList tokenList = new TokenList();
        List<AccessToken> tokens = accessTokenService.loadAll(user.getName());
        for (AccessToken token : tokens) {
            tokenList.addToken(new Token(token));
        }
        return tokenList;
    }

    @POST
    @Path("{username}/tokens/{name}")
    @RequiresPermissions(RestPermissions.USERS_TOKENCREATE)
    @ApiOperation("Generates a new access token for a user")
    public Token generateNewToken(
            @ApiParam(name = "username", required = true) @PathParam("username") String username,
            @ApiParam(name = "name", value = "Descriptive name for this token (e.g. 'cronjob') ", required = true) @PathParam("name") String name) {
        final User user = _tokensCheckAndLoadUser(username);
        final AccessToken accessToken = accessTokenService.create(user.getName(), name);
        return new Token(accessToken);
    }

    @DELETE
    @RequiresPermissions(RestPermissions.USERS_TOKENREMOVE)
    @Path("{username}/tokens/{token}")
    @ApiOperation("Removes a token for a user")
    public void revokeToken(
            @ApiParam(name = "username", required = true) @PathParam("username") String username,
            @ApiParam(name = "access token", required = true) @PathParam("token") String token) {
        final User user = _tokensCheckAndLoadUser(username);
        final AccessToken accessToken = accessTokenService.load(token);

        if (accessToken != null) {
            accessTokenService.destroy(accessToken);
        } else {
            throw new NotFoundException();
        }
    }

    private User _tokensCheckAndLoadUser(String username) {
        final User user = userService.load(username);
        if (user == null) {
            throw new NotFoundException("Unknown user " + username);
        }
        if (!getSubject().getPrincipal().equals(username)) {
            throw new ForbiddenException("Cannot access other people's tokens.");
        }
        return user;
    }

    private Map<String, Object> toMap(User user) {
        return toMap(user, true);
    }

    private HashMap<String, Object> toMap(User user, boolean includePermissions) {
        final HashMap<String, Object> map = Maps.newHashMap();
        map.put("id", firstNonNull(user.getId(), ""));
        map.put("username", user.getName());
        map.put("email", user.getEmail());
        map.put("full_name", user.getFullName());
        if (includePermissions) {
            map.put("permissions", user.getPermissions());
        }
        final Map<String, Object> preferences = user.getPreferences();
        if (preferences != null && !preferences.isEmpty()) {
            map.put("preferences", preferences);
        }
        if (user.getTimeZone() != null) {
            map.put("timezone", user.getTimeZone().getID());
        }
        map.put("session_timeout_ms", user.getSessionTimeoutMs());
        map.put("read_only", user.isReadOnly());
        map.put("external", user.isExternalUser());
        map.put("startpage", user.getStartpage());

        return map;
    }

    @JsonAutoDetect
    private static class TokenList {
        @JsonProperty
        private final List<Token> tokens = Lists.newArrayList();

        public void addToken(Token token) {
            tokens.add(token);
        }
    }

    @JsonAutoDetect
    private static class Token {

        private final AccessToken token;

        public Token(AccessToken token) {
            this.token = token;
        }

        @JsonProperty
        public String getName() {
            return token.getName();
        }

        @JsonProperty
        public String getToken() {
            return token.getToken();
        }

        @JsonProperty
        public Date getLastAccess() {
            return token.getLastAccess().toDate();
        }
    }
}

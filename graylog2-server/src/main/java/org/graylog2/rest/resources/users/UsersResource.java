/**
 * Copyright 2013 Lennart Koopmann <lennart@torch.sh>
 *
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
 *
 */
package org.graylog2.rest.resources.users;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.apache.shiro.crypto.hash.SimpleHash;
import org.bson.types.ObjectId;
import org.graylog2.database.ValidationException;
import org.graylog2.rest.documentation.annotations.*;
import org.graylog2.rest.resources.RestResource;
import org.graylog2.rest.resources.users.requests.ChangePasswordRequest;
import org.graylog2.rest.resources.users.requests.CreateRequest;
import org.graylog2.rest.resources.users.requests.PermissionEditRequest;
import org.graylog2.security.AccessToken;
import org.graylog2.security.RestPermissions;
import org.graylog2.users.User;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static javax.ws.rs.core.Response.Status.*;
import static javax.ws.rs.core.Response.*;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
@RequiresAuthentication
@Path("/users")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Api(value = "Users", description = "User accounts")
public class UsersResource extends RestResource {

    private static final Logger LOG = LoggerFactory.getLogger(RestResource.class);

    @GET
    @Path("{username}")
    @ApiOperation(value = "Get user details", notes = "The user's permissions are only included if a user asks for his " +
            "own account or for users with the necessary permissions to edit permissions.")
    @ApiResponses({
            @ApiResponse(code = 404, message = "The user could not be found.")
    })
    public Response get(@ApiParam(title = "username", description = "The username to return information for.", required = true) @PathParam("username") String username) {
        final User user = User.load(username, core);
        if (user == null) {
            return status(NOT_FOUND).build();
        }
        // if the requested username does not match the authenticated user, then we don't return permission information
        final boolean allowedToSeePermissions = getSubject().isPermitted(RestPermissions.USERS_PERMISSIONSEDIT);
        final boolean permissionsAllowed = getSubject().getPrincipal().toString().equals(username) || allowedToSeePermissions;

        return ok().entity(json(toMap(user, permissionsAllowed))).build();
    }

    @GET
    @RequiresPermissions(RestPermissions.USERS_LIST)
    @ApiOperation(value = "List all users", notes = "The permissions assigned to the users are always included.")
    public Response listUsers() {
        final List<User> users = User.loadAll(core);
        final List<Map<String, Object>> resultUsers = Lists.newArrayList();
        for (User user : users) {
            resultUsers.add(toMap(user));
        }
        resultUsers.add(toMap(new User.LocalAdminUser(core)));
        final HashMap<Object, Object> map = Maps.newHashMap();
        map.put("users", resultUsers);
        return ok(json(map)).build();
    }

    @POST
    @RequiresPermissions(RestPermissions.USERS_CREATE)
    @ApiOperation("Create a new user account.")
    @ApiResponses({
            @ApiResponse(code = 400, message = "Missing or invalid user details.")
    })
    public Response create(@ApiParam(title = "JSON body", required = true) String body) {
        if (body == null || body.isEmpty()) {
            LOG.error("Missing parameters. Returning HTTP 400.");
            throw new WebApplicationException(400);
        }

        CreateRequest cr = getCreateRequest(body);

        // Create user.
        Map<String, Object> userData = Maps.newHashMap();
        userData.put(User.USERNAME, cr.username);
        final String hashedPassword = new SimpleHash("SHA-1", cr.password, core.getConfiguration().getPasswordSecret()).toString();
        userData.put(User.PASSWORD, hashedPassword);
        userData.put(User.FULL_NAME, cr.fullname);
        userData.put(User.EMAIL, cr.email);
        userData.put(User.PERMISSIONS, cr.permissions);
        if (cr.timezone != null) {
            userData.put(User.TIMEZONE, cr.timezone);
        }
        User user = new User(userData, core);
        ObjectId id;
        try {
            // TODO JPA this is wrong, the primary key is the username
            id = user.save();
        } catch (ValidationException e) {
            LOG.error("Validation error.", e);
            throw new WebApplicationException(e, BAD_REQUEST);
        }
        // TODO don't expose mongo object id here, we never accept it. set location header instead
        Map<String, Object> result = Maps.newHashMap();
        result.put("id", id.toStringMongod());

        return status(CREATED).entity(json(result)).build();
    }

    @PUT
    @Path("{username}")
    @RequiresPermissions(RestPermissions.USERS_EDIT)
    @ApiOperation("Modify user details.")
    @ApiResponses({
            @ApiResponse(code = 400, message = "Attempted to modify a read only user account (e.g. built-in or LDAP users)."),
            @ApiResponse(code = 400, message = "Missing or invalid user details.")
    })
    public Response changeUser(@ApiParam(title = "username", description = "The name of the user to modify.", required = true) @PathParam("username") String username, String body) {
        if (body == null || body.isEmpty()) {
            throw new BadRequestException("Missing request body.");
        }

        CreateRequest cr = getCreateRequest(body);

        final User user = User.load(username, core);
        if (user == null) {
            return status(NOT_FOUND).build();
        }
        if (user.isReadOnly()) {
            throw new BadRequestException("Cannot modify readonly user " + username);
        }
        // we only allow setting a subset of the fields in CreateRequest
        if (cr.email != null) {
            user.setEmail(cr.email);
        }
        if (cr.fullname != null) {
            user.setFullName(cr.fullname);
        }
        if (cr.permissions != null) {
            user.setPermissions(cr.permissions);
        }
        if (cr.timezone != null) {
            try {
                final DateTimeZone tz = DateTimeZone.forID(cr.timezone);
                user.setTimeZone(tz);
            } catch (IllegalArgumentException e) {
                LOG.error("Invalid timezone {}, discarding it for user {}.", cr.timezone, username);
            }
        }
        try {
            // TODO JPA this is wrong, the primary key is the username
            user.save();
        } catch (ValidationException e) {
            LOG.error("Validation error.", e);
            throw new BadRequestException("Validation error for " + username, e);
        }

        return Response.noContent().build();
    }

    @DELETE
    @Path("{username}")
    @RequiresPermissions(RestPermissions.USERS_EDIT)
    @ApiOperation("Removes a user account.")
    @ApiResponses({@ApiResponse(code = 400, message = "When attempting to remove a read only user (e.g. built-in or LDAP user).")})
    public Response deleteUser(@ApiParam(title = "username", description = "The name of the user to delete.", required = true) @PathParam("username") String username) {
        final User user = User.load(username, core);
        if (user == null) {
            return status(NOT_FOUND).build();
        }
        if (user.isReadOnly()) {
            throw new BadRequestException("Cannot delete readonly user " + username);
        }

        user.destroy();
        return Response.noContent().build();
    }

    @PUT
    @Path("{username}/permissions")
    @RequiresPermissions(RestPermissions.USERS_PERMISSIONSEDIT)
    @ApiOperation("Update a user's permission set.")
    @ApiResponses({
            @ApiResponse(code = 400, message = "Missing or invalid permission data.")
    })
    public Response editPermissions(
            @ApiParam(title = "username", description = "The name of the user to modify.", required = true) @PathParam("username") String username,
            @ApiParam(title = "JSON body", description = "The list of permissions to assign to the user.", required = true) String body) {
        PermissionEditRequest permissionRequest;
        try {
            permissionRequest = objectMapper.readValue(body, PermissionEditRequest.class);
        } catch (IOException e) {
            throw new BadRequestException(e);
        }

        final User user = User.load(username, core);
        if (user == null) {
            return status(NOT_FOUND).build();
        }
        user.setPermissions(permissionRequest.permissions);
        try {
            user.save();
        } catch (ValidationException e) {
            LOG.error("Validation error.", e);
            throw new BadRequestException("Validation error for " + username, e);
        }
        return Response.noContent().build();
    }

    @DELETE
    @Path("{username}/permissions")
    @RequiresPermissions(RestPermissions.USERS_PERMISSIONSEDIT)
    @ApiOperation("Revoke all permissions for a user without deleting the account.")
    @ApiResponses({
            @ApiResponse(code = 500, message = "When saving the user failed.")
    })
    public Response deletePermissions(@ApiParam(title = "username", description = "The name of the user to modify.", required = true) @PathParam("username") String username) {
        final User user = User.load(username, core);
        if (user == null) {
            return status(NOT_FOUND).build();
        }
        user.setPermissions(Lists.<String>newArrayList());
        try {
            user.save();
        } catch (ValidationException e) {
            throw new InternalServerErrorException(e);
        }
        return status(NO_CONTENT).build();
    }

    @PUT
    @Path("{username}/password")
    @ApiOperation("Update the password for a user.")
    @ApiResponses({
            @ApiResponse(code = 201, message = "The password was successfully updated. Subsequent requests must be made with the new password."),
            @ApiResponse(code = 400, message = "If the given old password does not match the password of the given user."),
            @ApiResponse(code = 403, message = "If the requesting user has insufficient privileges to update the password for the given user or the old password was wrong."),
            @ApiResponse(code = 404, message = "If the user does not exist.")
    })
    public Response changePassword(
            @ApiParam(title = "username", description = "The name of the user whose password to change.", required = true) @PathParam("username") String username,
            @ApiParam(title = "JSON body", description = "The hashed old and new passwords.", required = true) String body) {

        if (body == null || body.isEmpty()) {
            throw new BadRequestException("Missing request body.");
        }

        final ChangePasswordRequest cr;
        try {
            cr = objectMapper.readValue(body, ChangePasswordRequest.class);
        } catch (IOException e) {
            LOG.error("Error while parsing JSON", e);
            throw new WebApplicationException(e, BAD_REQUEST);
        }

        final User user = User.load(username, core);
        if (user == null) {
            return status(NOT_FOUND).build();
        }

        if (!getSubject().isPermitted(RestPermissions.USERS_PASSWORDCHANGE + ":" + user.getName())) {
            return status(FORBIDDEN).build();
        }
        if (user.isExternalUser()) {
            LOG.error("Cannot change password for LDAP user.");
            return status(FORBIDDEN).build();
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

        final String currentPasswordHash = user.getHashedPassword();
        boolean changeAllowed = false;
        final String secret = core.getConfiguration().getPasswordSecret();
        if (checkOldPassword) {
            if (cr.old_password == null) {
                LOG.info("Changing password for user {} must supply the old password.", username);
                return status(BAD_REQUEST).build();
            }
            final String oldPasswordHash = new SimpleHash("SHA-1", cr.old_password, secret).toString();
            if (currentPasswordHash.equals(oldPasswordHash)) {
                changeAllowed = true;
            }
        } else {
            changeAllowed = true;
        }
        if (changeAllowed) {
            final String newHashedPassword = new SimpleHash("SHA-1", cr.password, secret).toString();
            user.setHashedPassword(newHashedPassword);
            try {
                user.save();
            } catch (ValidationException e) {
                throw new BadRequestException("Validation error for " + username, e);
            }
            return noContent().build();
        }
        return status(FORBIDDEN).build();
    }

    @GET
    @Path("{username}/tokens")
    @RequiresPermissions(RestPermissions.USERS_TOKENLIST)
    @ApiOperation("Retrieves the list of access tokens for a user")
    public TokenList listTokens(@ApiParam(title = "username", required = true) @PathParam("username") String username) {
        final User user = _tokensCheckAndLoadUser(username);
        final TokenList tokenList = new TokenList();
        List<AccessToken>  tokens = AccessToken.loadAll(user.getName(), core);
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
            @ApiParam(title = "username", required = true) @PathParam("username") String username,
            @ApiParam(title = "name", description = "Descriptive name for this token (e.g. 'cronjob') ", required = true) @PathParam("name") String name) {
        final User user = _tokensCheckAndLoadUser(username);
        final AccessToken accessToken = AccessToken.create(core, user.getName(), name);
        return new Token(accessToken);
    }

    @DELETE
    @RequiresPermissions(RestPermissions.USERS_TOKENREMOVE)
    @Path("{username}/tokens/{token}")
    @ApiOperation("Removes a token for a user")
    public Response revokeToken(
            @ApiParam(title = "username", required = true) @PathParam("username") String username,
            @ApiParam(title = "access token", required = true) @PathParam("token") String token) {
        final User user = _tokensCheckAndLoadUser(username);
        final AccessToken accessToken = AccessToken.load(token, core);
        if (accessToken != null) {
            accessToken.destroy();
            return noContent().build();
        }
        return Response.status(NOT_FOUND).build();
    }

    private User _tokensCheckAndLoadUser(String username) {
        final User user = User.load(username, core);
        if (user == null) {
            throw new NotFoundException("Unknown user " + username);
        }
        if (!getSubject().getPrincipal().equals(username)) {
            throw new ForbiddenException("Cannot access other people's tokens.");
        }
        return user;
    }

    private HashMap<String, Object> toMap(User user) {
        return toMap(user, true);
    }

    private HashMap<String, Object> toMap(User user, boolean includePermissions) {
        final HashMap<String,Object> map = Maps.newHashMap();
        map.put("id", Objects.firstNonNull(user.getId(), "").toString());
        map.put("username", user.getName());
        map.put("email", user.getEmail());
        map.put("full_name", user.getFullName());
        if (includePermissions) {
            map.put("permissions", user.getPermissions());
        }
        if (user.getTimeZone() != null) {
            map.put("timezone", user.getTimeZone().getID());
        }
        map.put("read_only", user.isReadOnly());
        map.put("external", user.isExternalUser());
        return map;
    }

    private CreateRequest getCreateRequest(String body) {
        CreateRequest cr;
        try {
            cr = objectMapper.readValue(body, CreateRequest.class);
        } catch(IOException e) {
            LOG.error("Error while parsing JSON", e);
            throw new WebApplicationException(e, BAD_REQUEST);
        }
        return cr;
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

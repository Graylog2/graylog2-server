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

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.bson.types.ObjectId;
import org.graylog2.database.ValidationException;
import org.graylog2.rest.resources.RestResource;
import org.graylog2.rest.resources.users.requests.CreateRequest;
import org.graylog2.rest.resources.users.requests.PermissionEditRequest;
import org.graylog2.security.RestPermissions;
import org.graylog2.users.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.io.IOException;
import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
@RequiresAuthentication
@Path("/users")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class UsersResource extends RestResource {

    private static final Logger LOG = LoggerFactory.getLogger(RestResource.class);

    @GET
    @Path("{username}")
    public Response authenticate(@Context SecurityContext securityContext, @PathParam("username") String username) {
        final Principal principal = securityContext.getUserPrincipal();
        final User user = User.load(principal.getName(), core);

        if (user == null) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        // if the requested username does not match the authenticated user, then we don't return permission information
        final boolean isSameUser = user.getName().equals(username);
        return Response.ok().entity(json(toMap(user, isSameUser))).build();
    }

    @GET
    @RequiresPermissions(RestPermissions.USERS_LIST)
    public Response listUsers() {
        final List<User> users = User.loadAll(core);
        final List<Map<String, Object>> resultUsers = Lists.newArrayList();
        for (User user : users) {
            resultUsers.add(toMap(user));
        }
        final HashMap<Object, Object> map = Maps.newHashMap();
        map.put("users", resultUsers);
        return Response.ok(json(map)).build();
    }

    @POST
    @RequiresPermissions(RestPermissions.USERS_CREATE)
    public Response create(String body) {
        if (body == null || body.isEmpty()) {
            LOG.error("Missing parameters. Returning HTTP 400.");
            throw new WebApplicationException(400);
        }

        CreateRequest cr;
        try {
            cr = objectMapper.readValue(body, CreateRequest.class);
        } catch(IOException e) {
            LOG.error("Error while parsing JSON", e);
            throw new WebApplicationException(e, Response.Status.BAD_REQUEST);
        }

        // Create user.
        Map<String, Object> userData = Maps.newHashMap();
        userData.put("username", cr.username);
        userData.put("password", cr.password); // core.getConfiguration().getPasswordSecret()));
        userData.put("full_name", cr.fullName);
        userData.put("permissions", cr.permissions);

        User user = new User(userData, core);
        ObjectId id;
        try {
            // TODO JPA this is wrong, the primary key is the username
            id = user.save();
        } catch (ValidationException e) {
            LOG.error("Validation error.", e);
            throw new WebApplicationException(e, Response.Status.BAD_REQUEST);
        }

        Map<String, Object> result = Maps.newHashMap();
        result.put("id", id.toStringMongod());

        return Response.status(Response.Status.CREATED).entity(json(result)).build();
    }

    @PUT
    @Path("{username}/permissions")
    @RequiresPermissions(RestPermissions.USERPERMISSIONS_EDIT)
    public Response editPermissions(@PathParam("username") String username, String body) {
        PermissionEditRequest permissionRequest;
        try {
            permissionRequest = objectMapper.readValue(body, PermissionEditRequest.class);
        } catch (IOException e) {
            throw new BadRequestException(e);
        }

        final User user = User.load(username, core);
        user.setPermissions(permissionRequest.permissions);
        try {
            user.save();
        } catch (ValidationException e) {
            throw new InternalServerErrorException(e);
        }
        return Response.status(Response.Status.CREATED).build();
    }

    @DELETE
    @Path("{username}/permissions")
    @RequiresPermissions(RestPermissions.USERPERMISSIONS_EDIT)
    public Response deletePermissions(@PathParam("username") String username) {
        final User user = User.load(username, core);
        user.setPermissions(Lists.<String>newArrayList());
        try {
            user.save();
        } catch (ValidationException e) {
            throw new InternalServerErrorException(e);
        }
        return Response.status(Response.Status.NO_CONTENT).build();
    }

    private HashMap<String, Object> toMap(User user) {
        return toMap(user, true);
    }

    private HashMap<String, Object> toMap(User user, boolean includePermissions) {
        final HashMap<String,Object> map = Maps.newHashMap();
        map.put("id", user.getId().toString());
        map.put("username", user.getName());
        map.put("full_name", user.getFullName());
        if (includePermissions) {
            map.put("permissions", user.getPermissions());
        }
        map.put("read_only", user.isReadOnly());
        return map;
    }


}

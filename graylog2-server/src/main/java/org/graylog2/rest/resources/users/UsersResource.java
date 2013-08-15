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

import com.google.common.collect.Maps;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.bson.types.ObjectId;
import org.graylog2.database.ValidationException;
import org.graylog2.rest.resources.RestResource;
import org.graylog2.rest.resources.users.requests.CreateRequest;
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
import java.util.Map;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
@Path("/users")
public class UsersResource extends RestResource {

    private static final Logger LOG = LoggerFactory.getLogger(RestResource.class);

    @GET
    @RequiresAuthentication
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response authenticate(@Context SecurityContext securityContext) {
        final Principal principal = securityContext.getUserPrincipal();
        final User user = User.load(principal.getName(), core);

        if (user == null) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        final HashMap<String,String> map = Maps.newHashMap();
        map.put("username", user.getName());
        map.put("isAuthorized", "true");
        map.put("full_name", user.getFullName());
        return Response.ok().entity(json(map)).build();

    }

    @POST
    @RequiresAuthentication
    @RequiresPermissions("users:create")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
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
        result.put("user_id", id.toStringMongod());

        return Response.status(Response.Status.CREATED).entity(json(result)).build();
    }

}

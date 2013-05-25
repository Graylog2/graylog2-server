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
package org.graylog2.rest.resources.sessions;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.jersey.api.core.ResourceConfig;
import org.graylog2.Core;
import org.graylog2.rest.RestResource;
import org.graylog2.rest.resources.sessions.requests.CreateRequest;
import org.graylog2.users.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
@Path("/sessions")
public class SessionsResource extends RestResource {

    private static final Logger LOG = LoggerFactory.getLogger(RestResource.class);

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Context
    ResourceConfig rc;

    @POST
    @Path("/")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response authenticate(String body, @QueryParam("pretty") boolean prettyPrint) {
        Core core = (Core) rc.getProperty("core");

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

        if (cr.username == null || cr.username.isEmpty() || cr.password == null || cr.password.isEmpty()) {
            LOG.error("Incomplete JSON.");
            throw new WebApplicationException(400);
        }

        // Does the user exist at all?
        if (User.exists(cr.username, cr.password, core)) {
            return Response.ok().build();
        } else {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
    }

}

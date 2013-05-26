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
package selenium.serverstub.rest.resources.authentication;

import com.google.common.collect.Maps;
import org.codehaus.jackson.map.ObjectMapper;
import selenium.serverstub.ServerStub;
import selenium.serverstub.rest.resources.RestResource;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.Map;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
@Path("/session")
public class SessionResource extends RestResource {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Context
    com.sun.jersey.api.core.ResourceConfig rc;

    @POST
    @Path("/")
    public Response create(String body, @QueryParam("pretty") boolean prettyPrint) {
        ServerStub core = (ServerStub) rc.getProperty("core");

        LoginRequest lr;
        try {
            lr = objectMapper.readValue(body, LoginRequest.class);
        } catch(IOException e) { throw new RuntimeException(e); }

        if (core.users.containsKey(lr.username) && core.users.get(lr.username).equals(lr.password)) {
            Map<String, Object> result = Maps.newHashMap();
            result.put("username", lr.username);
            result.put("full_name", "Stub User");

            return Response.ok(json(result, prettyPrint)).build();
        } else {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

    }

    private class LoginRequest {
        public String username;
        public String password;
    }
}
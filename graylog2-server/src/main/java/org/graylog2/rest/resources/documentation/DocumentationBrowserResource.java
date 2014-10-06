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
package org.graylog2.rest.resources.documentation;

import com.google.common.io.Resources;
import org.graylog2.rest.resources.RestResource;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.net.URL;

@Path("/api-browser")
public class DocumentationBrowserResource extends RestResource {

    private ClassLoader classLoader = ClassLoader.getSystemClassLoader();

    @GET
    public Response root() {
        return asset("index.html");
    }

    @GET
    @Path("/{route: .*}")
    public Response asset(@PathParam("route") String route) {
        // Directory traversal should not be possible but just to make sure..
        if (route.contains("..")) {
            throw new BadRequestException();
        }

        if (route.trim().equals("")) {
            route = "index.html";
        }

        final URL resource = classLoader.getResource("swagger/" + route);
        if (null != resource) {
            try {
                final byte[] resourceBytes = Resources.toByteArray(resource);

                return Response.ok(resourceBytes, guessContentType(route))
                        .header("Content-Length", resourceBytes.length)
                        .build();
            } catch (IOException e) {
                throw new NotFoundException(e);
            }
        } else {
            throw new NotFoundException();
        }
    }
}

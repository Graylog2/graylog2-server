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
package org.graylog2.rest.resources.system;

import com.wordnik.swagger.annotations.Api;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog2.rest.models.system.DisplayGettingStarted;
import org.graylog2.shared.rest.resources.RestResource;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

@RequiresAuthentication
@Api(value = "System/Getting Started Guide", description = "Getting Started guide")
@Path("/system/gettingstarted")
public class GettingStartedResource extends RestResource {

    @GET
    public DisplayGettingStarted displayGettingStarted() {
        return DisplayGettingStarted.create(true);
    }

    @POST
    @Path("dismiss")
    public Response dismissGettingStarted() {
        return Response.noContent().build();
    }

}

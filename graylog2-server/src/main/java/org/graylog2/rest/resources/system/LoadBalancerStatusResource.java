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
package org.graylog2.rest.resources.system;

import com.codahale.metrics.annotation.Timed;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.graylog2.plugin.lifecycles.LoadBalancerStatus;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import org.graylog2.rest.resources.RestResource;
import org.graylog2.security.RestPermissions;

import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Api(value = "System/LoadBalancers", description = "Status propagation for load balancers")
@Path("/system/lbstatus")
public class LoadBalancerStatusResource extends RestResource{

    /*
     *  IMPORTANT: this resource is unauthenticated to allow easy
     *             acccess for load balancers. think about this
     *             when adding more stuff.
     */

    @GET @Timed
    @Produces(MediaType.TEXT_PLAIN)
    @ApiOperation(value = "Get status of this graylog2-server node for load balancers. " +
            "Returns either ALIVE with HTTP 200 or DEAD with HTTP 503.")
    public Response status() {
        final LoadBalancerStatus lbStatus = serverStatus.getLifecycle().getLoadbalancerStatus();

        Response.Status status = lbStatus == LoadBalancerStatus.ALIVE
                ? Response.Status.OK : Response.Status.SERVICE_UNAVAILABLE;

        return Response.status(status)
                .entity(lbStatus.toString().toUpperCase())
                .build();
    }

    @PUT @Timed
    @RequiresAuthentication
    @RequiresPermissions(RestPermissions.LBSTATUS_CHANGE)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Override load balancer status of this graylog2-server node. Next lifecycle " +
            "change will override it again to its default. Set to ALIVE or DEAD.")
    @Path("/override/{status}")
    public Response override(@ApiParam(name = "status") @PathParam("status") String status) {
        final LoadBalancerStatus lbStatus;
        try {
            lbStatus = LoadBalancerStatus.valueOf(status.toUpperCase());
        } catch(IllegalArgumentException e) {
            throw new WebApplicationException(e, Response.Status.BAD_REQUEST);
        }

        switch (lbStatus) {
            case DEAD:
                serverStatus.overrideLoadBalancerDead();
                break;
            case ALIVE:
                serverStatus.overrideLoadBalancerAlive();
                break;
        }

        return Response.ok().build();
    }
}

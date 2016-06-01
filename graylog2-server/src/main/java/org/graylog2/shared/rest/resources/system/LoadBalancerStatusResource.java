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
package org.graylog2.shared.rest.resources.system;

import com.codahale.metrics.annotation.Timed;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.graylog2.plugin.ServerStatus;
import org.graylog2.plugin.lifecycles.LoadBalancerStatus;
import org.graylog2.shared.rest.resources.RestResource;
import org.graylog2.shared.security.RestPermissions;
import org.graylog2.rest.TooManyRequestsStatus;

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Locale;

@Api(value = "System/LoadBalancers", description = "Status propagation for load balancers")
@Path("/system/lbstatus")
public class LoadBalancerStatusResource extends RestResource {

    /*
     *  IMPORTANT: this resource is unauthenticated to allow easy
     *             acccess for load balancers. think about this
     *             when adding more stuff.
     */

    private final ServerStatus serverStatus;

    @Inject
    public LoadBalancerStatusResource(ServerStatus serverStatus) {
        this.serverStatus = serverStatus;
    }

    @GET
    @Timed
    @Produces(MediaType.TEXT_PLAIN)
    @ApiOperation(value = "Get status of this Graylog server node for load balancers. " +
            "Returns ALIVE with HTTP 200, DEAD with HTTP 503, or THROTTLED with HTTP 429.")
    public Response status() {
        final LoadBalancerStatus lbStatus = serverStatus.getLifecycle().getLoadbalancerStatus();

        Response.StatusType status;
        switch (lbStatus) {
            case ALIVE:
                status = Response.Status.OK;
                break;
            case THROTTLED:
                status = new TooManyRequestsStatus();
                break;
            default:
                status = Response.Status.SERVICE_UNAVAILABLE;
        }

        return Response.status(status)
                .entity(lbStatus.toString().toUpperCase(Locale.ENGLISH))
                .build();
    }

    @PUT
    @Timed
    @RequiresAuthentication
    @RequiresPermissions(RestPermissions.LBSTATUS_CHANGE)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Override load balancer status of this Graylog server node. Next lifecycle " +
            "change will override it again to its default. Set to ALIVE, DEAD, or THROTTLED.")
    @Path("/override/{status}")
    public void override(@ApiParam(name = "status") @PathParam("status") String status) {
        final LoadBalancerStatus lbStatus;
        try {
            lbStatus = LoadBalancerStatus.valueOf(status.toUpperCase(Locale.ENGLISH));
        } catch (IllegalArgumentException e) {
            throw new BadRequestException(e);
        }

        switch (lbStatus) {
            case DEAD:
                serverStatus.overrideLoadBalancerDead();
                break;
            case ALIVE:
                serverStatus.overrideLoadBalancerAlive();
                break;
            case THROTTLED:
                serverStatus.overrideLoadBalancerThrottled();
        }
    }
}

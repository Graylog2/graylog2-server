/**
 * Copyright 2014 Lennart Koopmann <lennart@torch.sh>
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
package org.graylog2.rest.resources.system;

import com.codahale.metrics.annotation.Timed;
import org.graylog2.lifecycles.LoadBalancerStatus;
import org.graylog2.rest.documentation.annotations.Api;
import org.graylog2.rest.documentation.annotations.ApiOperation;
import org.graylog2.rest.resources.RestResource;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
@Api(value = "System/LoadBalancers", description = "Status propagation for load balancers")
@Path("/system/lbstatus")
public class LoadBalancerStatusResource extends RestResource{

    /*
     *  IMPORTANT: this resource is unauthenticated to allow easy
     *             acccess for load balancers. think about this
     *             when adding more stuff.
     */

    @GET @Timed
    @ApiOperation(value = "Get status of this graylog2-server node for load balancers. " +
            "Returns either ALIVE with HTTP 200 or DEAD with HTTP 503.")
    public Response status() {
        LoadBalancerStatus lbStatus = core.getLifecycle().getLoadbalancerStatus();

        Response.Status status = lbStatus.equals(LoadBalancerStatus.ALIVE)
                ? Response.Status.OK : Response.Status.SERVICE_UNAVAILABLE;

        return Response.status(status)
                .entity(lbStatus.toString().toUpperCase())
                .type(MediaType.TEXT_PLAIN)
                .build();
    }


}

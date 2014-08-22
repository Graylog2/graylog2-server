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
package org.graylog2.radio.rest.resources.system;

import com.codahale.metrics.annotation.Timed;
import org.graylog2.plugin.ServerStatus;
import org.graylog2.plugin.lifecycles.LoadBalancerStatus;
import org.graylog2.radio.rest.resources.RestResource;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/system/lbstatus")
public class LoadBalancerStatusResource extends RestResource {
    @Inject
    private ServerStatus serverStatus;

    @GET @Timed
    @Produces(MediaType.TEXT_PLAIN)
    public Response status() {
        /*
         * IMPORTANT!! When implementing permissions for radio: This must be
         *             accessible without authorization. LBs don't do that.
         */
        final LoadBalancerStatus lbStatus = serverStatus.getLifecycle().getLoadbalancerStatus();

        final Response.Status status = lbStatus == LoadBalancerStatus.ALIVE
                ? Response.Status.OK : Response.Status.SERVICE_UNAVAILABLE;

        return Response.status(status)
                .entity(lbStatus.toString().toUpperCase())
                .build();
    }

    @PUT @Timed
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/override/{status}")
    public Response override(@PathParam("status") String status) {
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

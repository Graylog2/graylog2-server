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
package org.graylog2.radio.rest.resources.system;

import com.codahale.metrics.annotation.Timed;
import org.graylog2.plugin.lifecycles.Lifecycle;
import org.graylog2.plugin.lifecycles.LoadBalancerStatus;
import org.graylog2.radio.rest.resources.RestResource;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
@Path("/system/lbstatus")
public class LoadBalancerStatusResource extends RestResource {

    @GET @Timed
    public javax.ws.rs.core.Response status() {
        /*
         * IMPORTANT!! When implementing permissions for radio: This must be
         *             accessible without authorization. LBs don't do that.
         */
        LoadBalancerStatus lbStatus = radio.getLifecycle().getLoadbalancerStatus();

        Response.Status status = lbStatus.equals(LoadBalancerStatus.ALIVE)
                ? Response.Status.OK : Response.Status.SERVICE_UNAVAILABLE;

        return Response.status(status)
                .entity(lbStatus.toString().toUpperCase())
                .type(MediaType.TEXT_PLAIN)
                .build();
    }

    @PUT @Timed
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/override/{status}")
    public Response override(@PathParam("status") String status) {
        LoadBalancerStatus lbStatus;
        try {
            lbStatus = LoadBalancerStatus.valueOf(status.toUpperCase());
        } catch(IllegalArgumentException e) {
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        }

        switch (lbStatus) {
            case DEAD:
                radio.setLifecycle(Lifecycle.OVERRIDE_LB_DEAD);
                break;
            case ALIVE:
                radio.setLifecycle(Lifecycle.OVERRIDE_LB_ALIVE);
                break;
        }

        return Response.status(Response.Status.OK).build();
    }

}

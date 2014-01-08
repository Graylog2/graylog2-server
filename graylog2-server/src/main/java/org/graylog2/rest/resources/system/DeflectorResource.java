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
package org.graylog2.rest.resources.system;

import com.codahale.metrics.annotation.Timed;
import com.google.common.collect.Maps;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.graylog2.rest.documentation.annotations.Api;
import org.graylog2.rest.documentation.annotations.ApiOperation;
import org.graylog2.rest.resources.RestResource;
import org.graylog2.security.RestPermissions;
import org.graylog2.system.activities.Activity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Map;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
@RequiresAuthentication
@Api(value = "System/Deflector", description = "Index deflector management")
@Path("/system/deflector")
public class DeflectorResource extends RestResource {

    private static final Logger LOG = LoggerFactory.getLogger(DeflectorResource.class);

    @GET @Timed
    @ApiOperation(value = "Get current deflector status")
    @RequiresPermissions(RestPermissions.DEFLECTOR_READ)
    @Produces(MediaType.APPLICATION_JSON)
    public Response deflector() {
        Map<String, Object> result = Maps.newHashMap();

        result.put("is_up", core.getDeflector().isUp());
        result.put("current_target", core.getDeflector().getCurrentActualTargetIndex());

        return Response.ok().entity(json(result)).build();
    }

    @GET @Timed
    @ApiOperation(value = "Get deflector configuration. Only available on master nodes.")
    @RequiresPermissions(RestPermissions.DEFLECTOR_READ)
    @Path("/config")
    @Produces(MediaType.APPLICATION_JSON)
    public Response config() {
        restrictToMaster();

        Map<String, Object> result = Maps.newHashMap();

        result.put("max_docs_per_index", core.getConfiguration().getElasticSearchMaxDocsPerIndex());
        result.put("max_number_of_indices", core.getConfiguration().getMaxNumberOfIndices());

        return Response.ok().entity(json(result)).build();
    }

    @POST @Timed
    @ApiOperation(value = "Cycle deflector to new/next index")
    @RequiresPermissions(RestPermissions.DEFLECTOR_CYCLE)
    @Path("/cycle")
    public Response cycle() {
        restrictToMaster();

        String msg = "Cycling deflector. Reason: REST request.";
        LOG.info(msg);
        core.getActivityWriter().write(new Activity(msg, DeflectorResource.class));

        core.getDeflector().cycle();
        return Response.ok().build();
    }
}

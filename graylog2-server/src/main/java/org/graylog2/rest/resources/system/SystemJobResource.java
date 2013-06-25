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

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.sun.jersey.api.core.ResourceConfig;
import org.graylog2.Core;
import org.graylog2.rest.resources.RestResource;
import org.graylog2.systemjobs.SystemJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.util.List;
import java.util.Map;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
@Path("/system/jobs")
public class SystemJobResource extends RestResource {

    private static final Logger LOG = LoggerFactory.getLogger(SystemJobResource.class);

    @Context
    ResourceConfig rc;

    @GET
    @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    public String list(@QueryParam("pretty") boolean prettyPrint) {
        Core core = (Core) rc.getProperty("core");

        List<Map<String, Object>> jobs = Lists.newArrayList();

        for (Map.Entry<String, SystemJob> x : core.getSystemJobManager().getRunningJobs().entrySet()) {
            jobs.add(x.getValue().toMap());
        }

        Map<String, Object> result = Maps.newHashMap();
        result.put("jobs", jobs);

        return json(result, prettyPrint);
    }

    @GET
    @Path("/{jobId}")
    @Produces(MediaType.APPLICATION_JSON)
    public String get(@PathParam("jobId") String jobId, @QueryParam("pretty") boolean prettyPrint) {
        Core core = (Core) rc.getProperty("core");

        if (jobId == null || jobId.isEmpty()) {
            LOG.error("Missing jobId. Returning HTTP 400.");
            throw new WebApplicationException(400);
        }

        SystemJob job = core.getSystemJobManager().getRunningJobs().get(jobId);

        if (job == null) {
            LOG.error("No system job with ID <{}> found.", jobId);
            throw new WebApplicationException(404);
        }

        return json(job.toMap(), prettyPrint);
    }

    // DELETE try to stop/cancel job

}

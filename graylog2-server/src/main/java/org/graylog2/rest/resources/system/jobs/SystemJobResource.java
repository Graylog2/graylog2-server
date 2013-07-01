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
package org.graylog2.rest.resources.system.jobs;

import com.codahale.metrics.annotation.Timed;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.sun.jersey.api.core.ResourceConfig;
import org.graylog2.Core;
import org.graylog2.rest.resources.RestResource;
import org.graylog2.rest.resources.system.jobs.requests.TriggerRequest;
import org.graylog2.systemjobs.NoSuchJobException;
import org.graylog2.systemjobs.SystemJob;
import org.graylog2.systemjobs.SystemJobConcurrencyException;
import org.graylog2.systemjobs.SystemJobFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
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

    @GET @Timed
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

    @GET @Timed
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

    @POST @Timed
    @Path("/")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response trigger(String body, @QueryParam("pretty") boolean prettyPrint) {
        Core core = (Core) rc.getProperty("core");

        if (body == null || body.isEmpty()) {
            LOG.error("Missing parameters. Returning HTTP 400.");
            throw new WebApplicationException(400);
        }

        TriggerRequest tr;
        try {
            tr = objectMapper.readValue(body, TriggerRequest.class);
        } catch(IOException e) {
            LOG.error("Error while parsing JSON", e);
            throw new WebApplicationException(e, Response.Status.BAD_REQUEST);
        }

        SystemJob job;
        try {
            job = SystemJobFactory.build(tr.jobName, core);
        } catch(NoSuchJobException e) {
            LOG.error("Such a system job type does not exist. Returning HTTP 400.");
            throw new WebApplicationException(400);
        }

        try {
            core.getSystemJobManager().submit(job);
        } catch (SystemJobConcurrencyException e) {
            LOG.error("Concurrency level of this job reached: " + e.getMessage());
            throw new WebApplicationException(400);
        }

        return Response.status(Response.Status.ACCEPTED).build();
    }

    // TODO: DELETE: attempt to stop/cancel job

}

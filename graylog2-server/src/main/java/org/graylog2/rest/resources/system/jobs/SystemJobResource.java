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

import com.beust.jcommander.internal.Lists;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import com.sun.jersey.api.core.ResourceConfig;
import org.graylog2.Core;
import org.graylog2.plugin.Tools;
import org.graylog2.rest.RestResource;
import org.graylog2.systemjobs.SystemJob;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
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

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Context
    ResourceConfig rc;

    @GET
    @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    public String listJobs(@QueryParam("pretty") boolean prettyPrint) {
        Core core = (Core) rc.getProperty("core");

        List<Map<String, Object>> jobs = Lists.newArrayList();

        for (Map.Entry<String, SystemJob> x : core.getSystemJobManager().getRunningJobs().entrySet()) {
            String jobId = x.getKey();
            SystemJob jobInfo = x.getValue();

            Map<String, Object> job = Maps.newHashMap();
            job.put("id", jobId);
            job.put("description", jobInfo.getDescription());
            job.put("started_at", Tools.getISO8601String(jobInfo.getStartedAt()));
            job.put("started_by", "SYSTEM/FIXME");
            job.put("percent_complete", jobInfo.getProgress());
            job.put("provides_progress", jobInfo.providesProgress());
            job.put("is_stoppable", jobInfo.isCancelable());
            job.put("node_id", core.getServerId());

            jobs.add(job);
        }

        Map<String, Object> result = Maps.newHashMap();
        result.put("jobs", jobs);

        return json(result, prettyPrint);
    }

    // GET single job
    // DELETE try to stop/cancel job

}

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
import org.graylog2.indexer.ranges.RebuildIndexRangesJob;
import org.graylog2.rest.documentation.annotations.Api;
import org.graylog2.rest.documentation.annotations.ApiOperation;
import org.graylog2.rest.documentation.annotations.ApiResponse;
import org.graylog2.rest.documentation.annotations.ApiResponses;
import org.graylog2.rest.resources.RestResource;
import org.graylog2.system.jobs.SystemJob;
import org.graylog2.system.jobs.SystemJobConcurrencyException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
@Api(value = "System/IndexRanges", description = "Index timeranges")
@Path("/system/indices/ranges")
public class IndexRangesResource extends RestResource {

    private static final Logger LOG = LoggerFactory.getLogger(IndexRangesResource.class);

    @POST @Timed
    @Path("/rebuild")
    @ApiOperation(value = "Rebuild/sync index range information.",
                  notes = "This triggers a systemjob that scans every index and stores meta information " +
                          "about what indices contain messages in what timeranges. It atomically overwrites " +
                          "already existing meta information.")
    @ApiResponses(value = {
            @ApiResponse(code = 202, message = "Rebuild/sync systemjob triggered.")
    })
    @Produces(MediaType.APPLICATION_JSON)
    public Response rebuild() {
        SystemJob rebuildJob = new RebuildIndexRangesJob(core);
        try {
            core.getSystemJobManager().submit(rebuildJob);
        } catch (SystemJobConcurrencyException e) {
            LOG.error("Concurrency level of this job reached: " + e.getMessage());
            throw new WebApplicationException(403);
        }

        return Response.status(Response.Status.ACCEPTED).build();
    }

}

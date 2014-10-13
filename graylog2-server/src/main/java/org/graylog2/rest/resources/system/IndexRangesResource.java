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
import com.google.common.collect.Lists;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.graylog2.indexer.Deflector;
import org.graylog2.indexer.ranges.IndexRange;
import org.graylog2.indexer.ranges.IndexRangeService;
import org.graylog2.indexer.ranges.RebuildIndexRangesJob;
import org.graylog2.rest.resources.RestResource;
import org.graylog2.rest.resources.system.responses.IndexRangesResponse;
import org.graylog2.security.RestPermissions;
import org.graylog2.system.jobs.SystemJob;
import org.graylog2.system.jobs.SystemJobConcurrencyException;
import org.graylog2.system.jobs.SystemJobManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
@RequiresAuthentication
@Api(value = "System/IndexRanges", description = "Index timeranges")
@Path("/system/indices/ranges")
public class IndexRangesResource extends RestResource {

    private static final Logger LOG = LoggerFactory.getLogger(IndexRangesResource.class);

    private final IndexRangeService indexRangeService;
    private final RebuildIndexRangesJob.Factory rebuildIndexRangesJobFactory;
    private final Deflector deflector;
    private final SystemJobManager systemJobManager;

    @Inject
    public IndexRangesResource(IndexRangeService indexRangeService,
                               RebuildIndexRangesJob.Factory rebuildIndexRangesJobFactory,
                               Deflector deflector,
                               SystemJobManager systemJobManager) {
        this.indexRangeService = indexRangeService;
        this.rebuildIndexRangesJobFactory = rebuildIndexRangesJobFactory;
        this.deflector = deflector;
        this.systemJobManager = systemJobManager;
    }

    @GET @Timed
    @ApiOperation(value = "Get a list of all index ranges")
    @Produces(MediaType.APPLICATION_JSON)
    public IndexRangesResponse list() {
        IndexRangesResponse irp = new IndexRangesResponse();
        List<IndexRange> ranges = Lists.newArrayList();

        for (IndexRange range : indexRangeService.getFrom(0)) {
            if (!isPermitted(RestPermissions.INDEXRANGES_READ, range.getIndexName())) {
                continue;
            }
            ranges.add(range);
        }

        irp.ranges = ranges;
        irp.total = ranges.size();

        return irp;
    }

    @POST @Timed
    @Path("/rebuild")
    @RequiresPermissions(RestPermissions.INDEXRANGES_REBUILD)
    @ApiOperation(value = "Rebuild/sync index range information.",
                  notes = "This triggers a systemjob that scans every index and stores meta information " +
                          "about what indices contain messages in what timeranges. It atomically overwrites " +
                          "already existing meta information.")
    @ApiResponses(value = {
            @ApiResponse(code = 202, message = "Rebuild/sync systemjob triggered.")
    })
    @Produces(MediaType.APPLICATION_JSON)
    public Response rebuild() {
        SystemJob rebuildJob = rebuildIndexRangesJobFactory.create(this.deflector);
        try {
            this.systemJobManager.submit(rebuildJob);
        } catch (SystemJobConcurrencyException e) {
            LOG.error("Concurrency level of this job reached: " + e.getMessage());
            throw new WebApplicationException(403);
        }

        return Response.status(Response.Status.ACCEPTED).build();
    }

}

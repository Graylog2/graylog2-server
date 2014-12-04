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
package org.graylog2.rest.resources.system.indexer;

import com.codahale.metrics.annotation.Timed;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.elasticsearch.action.admin.indices.stats.CommonStats;
import org.graylog2.indexer.Deflector;
import org.graylog2.indexer.cluster.Cluster;
import org.graylog2.indexer.indices.IndexStatistics;
import org.graylog2.indexer.indices.Indices;
import org.graylog2.indexer.ranges.RebuildIndexRangesJob;
import org.graylog2.rest.resources.RestResource;
import org.graylog2.rest.resources.system.indexer.responses.ClosedIndices;
import org.graylog2.rest.resources.system.indexer.responses.IndexInfo;
import org.graylog2.rest.resources.system.indexer.responses.IndexStats;
import org.graylog2.rest.resources.system.indexer.responses.ShardRouting;
import org.graylog2.security.RestPermissions;
import org.graylog2.system.jobs.SystemJob;
import org.graylog2.system.jobs.SystemJobConcurrencyException;
import org.graylog2.system.jobs.SystemJobManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.DELETE;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.GET;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.Set;

@RequiresAuthentication
@Api(value = "Indexer/Indices", description = "Index information")
@Path("/system/indexer/indices")
public class IndicesResource extends RestResource {
    private static final Logger LOG = LoggerFactory.getLogger(IndicesResource.class);

    @Inject
    private RebuildIndexRangesJob.Factory rebuildIndexRangesJobFactory;
    @Inject
    private Indices indices;
    @Inject
    private Cluster cluster;
    @Inject
    private Deflector deflector;
    @Inject
    private SystemJobManager systemJobManager;

    @GET
    @Timed
    @Path("/{index}")
    @ApiOperation(value = "Get information of an index and its shards.")
    @Produces(MediaType.APPLICATION_JSON)
    public IndexInfo single(@ApiParam(name = "index") @PathParam("index") String index) {
        checkPermission(RestPermissions.INDICES_READ, index);

        if (!deflector.isGraylog2Index(index)) {
            final String msg = "Index [" + index + "] doesn't look like an index managed by Graylog2.";
            LOG.info(msg);
            throw new NotFoundException(msg);
        }

        final IndexStatistics stats = indices.getIndexStats(index);
        if (stats == null) {
            final String msg = "Index [" + index + "] not found.";
            LOG.error(msg);
            throw new NotFoundException(msg);
        }

        final ImmutableList.Builder<ShardRouting> routing = ImmutableList.builder();
        for (org.elasticsearch.cluster.routing.ShardRouting shardRouting : stats.getShardRoutings()) {
            routing.add(shardRouting(shardRouting));
        }

        return IndexInfo.create(indexStats(stats.getPrimaries()), indexStats(stats.getTotal()),
                routing.build(), indices.isReopened(index));
    }

    @GET
    @Timed
    @Path("/closed")
    @ApiOperation(value = "Get a list of closed indices that can be reopened.")
    @Produces(MediaType.APPLICATION_JSON)
    public ClosedIndices closed() {
        Set<String> closedIndices;
        try {
            closedIndices = Sets.filter(indices.getClosedIndices(), new Predicate<String>() {
                @Override
                public boolean apply(String indexName) {
                    return isPermitted(RestPermissions.INDICES_READ, indexName);
                }
            });
        } catch (Exception e) {
            LOG.error("Could not get closed indices.", e);
            throw new InternalServerErrorException(e);
        }

        return ClosedIndices.create(closedIndices, closedIndices.size());
    }

    @POST
    @Timed
    @Path("/{index}/reopen")
    @ApiOperation(value = "Reopen a closed index. This will also trigger an index ranges rebuild job.")
    @Produces(MediaType.APPLICATION_JSON)
    public void reopen(@ApiParam(name = "index") @PathParam("index") String index) {
        checkPermission(RestPermissions.INDICES_CHANGESTATE, index);

        if (!deflector.isGraylog2Index(index)) {
            LOG.info("Index [{}] doesn't look like an index managed by Graylog2.", index);
            throw new NotFoundException();
        }

        indices.reopenIndex(index);

        // Trigger index ranges rebuild job.
        final SystemJob rebuildJob = rebuildIndexRangesJobFactory.create(deflector);
        try {
            systemJobManager.submit(rebuildJob);
        } catch (SystemJobConcurrencyException e) {
            final String msg = "Concurrency level of this job reached: " + e.getMessage();
            LOG.error(msg);
            throw new ForbiddenException(msg);
        }
    }

    @POST
    @Timed
    @Path("/{index}/close")
    @ApiOperation(value = "Close an index. This will also trigger an index ranges rebuild job.")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 403, message = "You cannot close the current deflector target index.")
    })
    public void close(@ApiParam(name = "index") @PathParam("index") String index) {
        checkPermission(RestPermissions.INDICES_CHANGESTATE, index);

        if (!deflector.isGraylog2Index(index)) {
            LOG.info("Index [{}] doesn't look like an index managed by Graylog2.", index);
            throw new NotFoundException();
        }

        if (deflector.getCurrentActualTargetIndex().equals(index)) {
            throw new ForbiddenException();
        }

        // Close index.
        indices.close(index);

        // Trigger index ranges rebuild job.
        final SystemJob rebuildJob = rebuildIndexRangesJobFactory.create(deflector);
        try {
            systemJobManager.submit(rebuildJob);
        } catch (SystemJobConcurrencyException e) {
            final String msg = "Concurrency level of this job reached: " + e.getMessage();
            LOG.error(msg);
            throw new ForbiddenException(msg);
        }
    }

    @DELETE
    @Timed
    @Path("/{index}")
    @ApiOperation(value = "Delete an index. This will also trigger an index ranges rebuild job.")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 403, message = "You cannot delete the current deflector target index.")
    })
    public void delete(@ApiParam(name = "index") @PathParam("index") String index) {
        checkPermission(RestPermissions.INDICES_DELETE, index);

        if (!deflector.isGraylog2Index(index)) {
            final String msg = "Index [" + index + "] doesn't look like an index managed by Graylog2.";
            LOG.info(msg);
            throw new NotFoundException(msg);
        }

        if (deflector.getCurrentActualTargetIndex().equals(index)) {
            throw new ForbiddenException();
        }

        // Delete index.
        indices.delete(index);

        // Trigger index ranges rebuild job.
        final SystemJob rebuildJob = rebuildIndexRangesJobFactory.create(deflector);
        try {
            systemJobManager.submit(rebuildJob);
        } catch (SystemJobConcurrencyException e) {
            final String msg = "Concurrency level of this job reached: " + e.getMessage();
            LOG.error(msg);
            throw new ForbiddenException(msg);
        }
    }

    private ShardRouting shardRouting(org.elasticsearch.cluster.routing.ShardRouting route) {
        return ShardRouting.create(route.shardId().getId(),
                route.state().name().toLowerCase(),
                route.active(),
                route.primary(),
                route.currentNodeId(),
                cluster.nodeIdToName(route.currentNodeId()),
                cluster.nodeIdToHostName(route.currentNodeId()),
                route.relocatingNodeId());
    }

    private IndexStats indexStats(final CommonStats stats) {
        return IndexStats.create(
                IndexStats.TimeAndTotalStats.create(stats.getFlush().getTotal(), stats.getFlush().getTotalTime().getSeconds()),
                IndexStats.TimeAndTotalStats.create(stats.getGet().getCount(), stats.getGet().getTime().getSeconds()),
                IndexStats.TimeAndTotalStats.create(stats.getIndexing().getTotal().getIndexCount(), stats.getIndexing().getTotal().getIndexTime().getSeconds()),
                IndexStats.TimeAndTotalStats.create(stats.getMerge().getTotal(), stats.getMerge().getTotalTime().getSeconds()),
                IndexStats.TimeAndTotalStats.create(stats.getRefresh().getTotal(), stats.getRefresh().getTotalTime().getSeconds()),
                IndexStats.TimeAndTotalStats.create(stats.getSearch().getTotal().getQueryCount(), stats.getSearch().getTotal().getQueryTime().getSeconds()),
                IndexStats.TimeAndTotalStats.create(stats.getSearch().getTotal().getFetchCount(), stats.getSearch().getTotal().getFetchTime().getSeconds()),
                stats.getSearch().getOpenContexts(),
                stats.getStore().getSize().getBytes(),
                stats.getSegments().getCount(),
                IndexStats.DocsStats.create(stats.getDocs().getCount(), stats.getDocs().getDeleted())
        );
    }
}
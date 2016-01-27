/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.rest.resources.system.indexer;

import com.codahale.metrics.annotation.Timed;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.elasticsearch.action.admin.indices.stats.CommonStats;
import org.graylog2.indexer.Deflector;
import org.graylog2.indexer.cluster.Cluster;
import org.graylog2.indexer.indices.IndexStatistics;
import org.graylog2.indexer.indices.Indices;
import org.graylog2.rest.models.system.indexer.responses.AllIndices;
import org.graylog2.rest.models.system.indexer.responses.ClosedIndices;
import org.graylog2.rest.models.system.indexer.responses.IndexInfo;
import org.graylog2.rest.models.system.indexer.responses.IndexStats;
import org.graylog2.rest.models.system.indexer.responses.OpenIndicesInfo;
import org.graylog2.rest.models.system.indexer.responses.ShardRouting;
import org.graylog2.shared.rest.resources.RestResource;
import org.graylog2.shared.security.RestPermissions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;
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
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@RequiresAuthentication
@Api(value = "Indexer/Indices", description = "Index information")
@Path("/system/indexer/indices")
public class IndicesResource extends RestResource {
    private static final Logger LOG = LoggerFactory.getLogger(IndicesResource.class);

    @Inject
    private Indices indices;
    @Inject
    private Cluster cluster;
    @Inject
    private Deflector deflector;

    @GET
    @Timed
    @Path("/{index}")
    @ApiOperation(value = "Get information of an index and its shards.")
    @Produces(MediaType.APPLICATION_JSON)
    public IndexInfo single(@ApiParam(name = "index") @PathParam("index") String index) {
        checkPermission(RestPermissions.INDICES_READ, index);

        if (!deflector.isGraylogIndex(index)) {
            final String msg = "Index [" + index + "] doesn't look like an index managed by Graylog.";
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
        for (org.elasticsearch.cluster.routing.ShardRouting shardRouting : stats.shardRoutings()) {
            routing.add(shardRouting(shardRouting));
        }

        return IndexInfo.create(indexStats(stats.primaries()), indexStats(stats.total()),
                routing.build(), indices.isReopened(index));
    }

    @GET
    @Path("/open")
    @Timed
    @ApiOperation(value = "Get information of all open indices managed by Graylog and their shards.")
    @RequiresPermissions(RestPermissions.INDICES_READ)
    @Produces(MediaType.APPLICATION_JSON)
    public OpenIndicesInfo open() {
        final Set<IndexStatistics> indicesStats = indices.getIndicesStats();

        final Map<String, IndexInfo> indexInfos = new HashMap<>();
        for (IndexStatistics indexStatistics : indicesStats) {
            final ImmutableList.Builder<ShardRouting> routing = ImmutableList.builder();
            for (org.elasticsearch.cluster.routing.ShardRouting shardRouting : indexStatistics.shardRoutings()) {
                routing.add(shardRouting(shardRouting));
            }

            final IndexInfo indexInfo = IndexInfo.create(
                    indexStats(indexStatistics.primaries()),
                    indexStats(indexStatistics.total()),
                    routing.build(),
                    indices.isReopened(indexStatistics.indexName()));

            indexInfos.put(indexStatistics.indexName(), indexInfo);
        }

        return OpenIndicesInfo.create(indexInfos);
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

    @GET
    @Timed
    @Path("/reopened")
    @ApiOperation(value = "Get a list of reopened indices, which will not be cleaned by retention cleaning")
    @Produces(MediaType.APPLICATION_JSON)
    public ClosedIndices reopened() {
        final Set<String> reopenedIndices;
        try {
            reopenedIndices = Sets.filter(indices.getReopenedIndices(), new Predicate<String>() {
                @Override
                public boolean apply(String indexName) {
                    return isPermitted(RestPermissions.INDICES_READ, indexName);
                }
            });
        } catch (Exception e) {
            LOG.error("Could not get reopened indices.", e);
            throw new InternalServerErrorException(e);
        }

        return ClosedIndices.create(reopenedIndices, reopenedIndices.size());
    }

    @GET
    @Timed
    @ApiOperation(value = "List all open, closed and reopened indices.")
    @Produces(MediaType.APPLICATION_JSON)
    public AllIndices all() {
        return AllIndices.create(this.closed(), this.reopened(), this.open());
    }


    @POST
    @Timed
    @Path("/{index}/reopen")
    @ApiOperation(value = "Reopen a closed index. This will also trigger an index ranges rebuild job.")
    @Produces(MediaType.APPLICATION_JSON)
    public void reopen(@ApiParam(name = "index") @PathParam("index") String index) {
        checkPermission(RestPermissions.INDICES_CHANGESTATE, index);

        if (!deflector.isGraylogIndex(index)) {
            LOG.info("Index [{}] doesn't look like an index managed by Graylog.", index);
            throw new NotFoundException();
        }

        indices.reopenIndex(index);
    }

    @POST
    @Timed
    @Path("/{index}/close")
    @ApiOperation(value = "Close an index. This will also trigger an index ranges rebuild job.")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 403, message = "You cannot close the current deflector target index.")
    })
    public void close(@ApiParam(name = "index") @PathParam("index") @NotNull String index) {
        checkPermission(RestPermissions.INDICES_CHANGESTATE, index);

        if (!deflector.isGraylogIndex(index)) {
            LOG.info("Index [{}] doesn't look like an index managed by Graylog.", index);
            throw new NotFoundException();
        }

        if (index.equals(deflector.getCurrentActualTargetIndex())) {
            throw new ForbiddenException("The current deflector target index (" + index + ") cannot be closed");
        }

        // Close index.
        indices.close(index);
    }

    @DELETE
    @Timed
    @Path("/{index}")
    @ApiOperation(value = "Delete an index. This will also trigger an index ranges rebuild job.")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 403, message = "You cannot delete the current deflector target index.")
    })
    public void delete(@ApiParam(name = "index") @PathParam("index") @NotNull String index) {
        checkPermission(RestPermissions.INDICES_DELETE, index);

        if (!deflector.isGraylogIndex(index)) {
            final String msg = "Index [" + index + "] doesn't look like an index managed by Graylog.";
            LOG.info(msg);
            throw new NotFoundException(msg);
        }

        if (index.equals(deflector.getCurrentActualTargetIndex())) {
            throw new ForbiddenException("The current deflector target index (" + index + ") cannot be deleted");
        }

        // Delete index.
        indices.delete(index);
    }

    private ShardRouting shardRouting(org.elasticsearch.cluster.routing.ShardRouting route) {
        return ShardRouting.create(route.shardId().getId(),
                route.state().name().toLowerCase(Locale.ENGLISH),
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

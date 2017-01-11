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
import com.google.common.collect.ImmutableList;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.elasticsearch.action.admin.indices.stats.CommonStats;
import org.graylog2.audit.AuditEventTypes;
import org.graylog2.audit.jersey.AuditEvent;
import org.graylog2.audit.jersey.NoAuditEvent;
import org.graylog2.indexer.IndexSet;
import org.graylog2.indexer.IndexSetRegistry;
import org.graylog2.indexer.cluster.Cluster;
import org.graylog2.indexer.indices.IndexStatistics;
import org.graylog2.indexer.indices.Indices;
import org.graylog2.indexer.indices.TooManyAliasesException;
import org.graylog2.rest.models.system.indexer.requests.IndicesReadRequest;
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
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.DELETE;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.GET;
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
import java.util.function.Function;
import java.util.stream.Collectors;

@RequiresAuthentication
@Api(value = "Indexer/Indices", description = "Index information")
@Path("/system/indexer/indices")
public class IndicesResource extends RestResource {
    private static final Logger LOG = LoggerFactory.getLogger(IndicesResource.class);

    private final Indices indices;
    private final Cluster cluster;
    private final IndexSetRegistry indexSetRegistry;

    @Inject
    public IndicesResource(Indices indices, Cluster cluster, IndexSetRegistry indexSetRegistry) {
        this.indices = indices;
        this.cluster = cluster;
        this.indexSetRegistry = indexSetRegistry;
    }

    @GET
    @Timed
    @Path("/{index}")
    @ApiOperation(value = "Get information of an index and its shards.")
    @Produces(MediaType.APPLICATION_JSON)
    public IndexInfo single(@ApiParam(name = "index") @PathParam("index") String index) {
        checkPermission(RestPermissions.INDICES_READ, index);

        if (!indexSetRegistry.isManagedIndex(index)) {
            final String msg = "Index [" + index + "] doesn't look like an index managed by Graylog.";
            LOG.info(msg);
            throw new NotFoundException(msg);
        }

        final IndexStatistics stats = indices.getIndexStats(index);
        if (stats == null) {
            final String msg = "Index [" + index + "] not found.";
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

    @POST
    @Timed
    @Path("/multiple")
    @ApiOperation(value = "Get information of all specified indices and their shards.")
    @Produces(MediaType.APPLICATION_JSON)
    @NoAuditEvent("only used to request index information")
    public Map<String, IndexInfo> multiple(@ApiParam(name = "Requested indices", required = true)
                                           @Valid @NotNull IndicesReadRequest request) {
        if (request.indices() != null) {
            return request.indices().stream()
                    .filter(indexSetRegistry::isManagedIndex)
                    .collect(Collectors.toMap(Function.identity(), this::single));
        }

        throw new BadRequestException("Missing or invalid list of indices passed in request.");
    }

    @GET
    @Path("/open")
    @Timed
    @ApiOperation(value = "Get information of all open indices managed by Graylog and their shards.")
    @RequiresPermissions(RestPermissions.INDICES_READ)
    @Produces(MediaType.APPLICATION_JSON)
    public OpenIndicesInfo open() {
        final Set<IndexStatistics> indicesStats = indexSetRegistry.getAll().stream()
                .flatMap(indexSet -> indices.getIndicesStats(indexSet).stream())
                .collect(Collectors.toSet());

        return getOpenIndicesInfo(indicesStats);
    }

    @GET
    @Timed
    @Path("/closed")
    @ApiOperation(value = "Get a list of closed indices that can be reopened.")
    @Produces(MediaType.APPLICATION_JSON)
    public ClosedIndices closed() {
        final Set<String> closedIndices = indexSetRegistry.getAll().stream()
                .flatMap(indexSet -> indices.getClosedIndices(indexSet).stream())
                .filter((indexName) -> isPermitted(RestPermissions.INDICES_READ, indexName) && indexSetRegistry.isManagedIndex(indexName))
                .collect(Collectors.toSet());

        return ClosedIndices.create(closedIndices, closedIndices.size());
    }

    @GET
    @Timed
    @Path("/reopened")
    @ApiOperation(value = "Get a list of reopened indices, which will not be cleaned by retention cleaning")
    @Produces(MediaType.APPLICATION_JSON)
    public ClosedIndices reopened() {
        final Set<String> reopenedIndices = indexSetRegistry.getAll().stream()
                .flatMap(indexSet -> indices.getReopenedIndices(indexSet).stream())
                .filter((indexName) -> isPermitted(RestPermissions.INDICES_READ, indexName) && indexSetRegistry.isManagedIndex(indexName))
                .collect(Collectors.toSet());

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
    @AuditEvent(type = AuditEventTypes.ES_INDEX_OPEN)
    public void reopen(@ApiParam(name = "index") @PathParam("index") String index) {
        checkPermission(RestPermissions.INDICES_CHANGESTATE, index);

        if (!indexSetRegistry.isManagedIndex(index)) {
            final String msg = "Index [" + index + "] doesn't look like an index managed by Graylog.";
            LOG.info(msg);
            throw new NotFoundException(msg);
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
    @AuditEvent(type = AuditEventTypes.ES_INDEX_CLOSE)
    public void close(@ApiParam(name = "index") @PathParam("index") @NotNull String index) throws TooManyAliasesException {
        checkPermission(RestPermissions.INDICES_CHANGESTATE, index);

        if (!indexSetRegistry.isManagedIndex(index)) {
            final String msg = "Index [" + index + "] doesn't look like an index managed by Graylog.";
            LOG.info(msg);
            throw new NotFoundException(msg);
        }

        if (indexSetRegistry.isCurrentWriteIndex(index)) {
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
    @AuditEvent(type = AuditEventTypes.ES_INDEX_DELETE)
    public void delete(@ApiParam(name = "index") @PathParam("index") @NotNull String index) throws TooManyAliasesException {
        checkPermission(RestPermissions.INDICES_DELETE, index);

        if (!indexSetRegistry.isManagedIndex(index)) {
            final String msg = "Index [" + index + "] doesn't look like an index managed by Graylog.";
            LOG.info(msg);
            throw new NotFoundException(msg);
        }

        if (indexSetRegistry.isCurrentWriteIndex(index)) {
            throw new ForbiddenException("The current deflector target index (" + index + ") cannot be deleted");
        }

        // Delete index.
        indices.delete(index);
    }

    // Index set

    @GET
    @Timed
    @Path("/{indexSetId}/list")
    @ApiOperation(value = "List all open, closed and reopened indices.")
    @Produces(MediaType.APPLICATION_JSON)
    public AllIndices indexSetList(@ApiParam(name = "indexSetId") @PathParam("indexSetId") String indexSetId) {
        return AllIndices.create(this.indexSetClosed(indexSetId), this.indexSetReopened(indexSetId), this.indexSetOpen(indexSetId));
    }

    @GET
    @Path("/{indexSetId}/open")
    @Timed
    @ApiOperation(value = "Get information of all open indices managed by Graylog and their shards.")
    @RequiresPermissions(RestPermissions.INDICES_READ)
    @Produces(MediaType.APPLICATION_JSON)
    public OpenIndicesInfo indexSetOpen(@ApiParam(name = "indexSetId") @PathParam("indexSetId") String indexSetId) {
        final IndexSet indexSet = getIndexSet(indexSetRegistry, indexSetId);
        final Set<IndexStatistics> indicesStats = indices.getIndicesStats(indexSet).stream()
                .filter(indexStats -> indexSetRegistry.isManagedIndex(indexStats.indexName()))
                .collect(Collectors.toSet());

        return getOpenIndicesInfo(indicesStats);
    }

    @GET
    @Timed
    @Path("/{indexSetId}/closed")
    @ApiOperation(value = "Get a list of closed indices that can be reopened.")
    @Produces(MediaType.APPLICATION_JSON)
    public ClosedIndices indexSetClosed(@ApiParam(name = "indexSetId") @PathParam("indexSetId") String indexSetId) {
        final IndexSet indexSet = getIndexSet(indexSetRegistry, indexSetId);

        final Set<String> closedIndices = indices.getClosedIndices(indexSet)
                .stream()
                .filter((indexName) -> isPermitted(RestPermissions.INDICES_READ, indexName) && indexSetRegistry.isManagedIndex(indexName))
                .collect(Collectors.toSet());

        return ClosedIndices.create(closedIndices, closedIndices.size());
    }

    @GET
    @Timed
    @Path("/{indexSetId}/reopened")
    @ApiOperation(value = "Get a list of reopened indices, which will not be cleaned by retention cleaning")
    @Produces(MediaType.APPLICATION_JSON)
    public ClosedIndices indexSetReopened(@ApiParam(name = "indexSetId") @PathParam("indexSetId") String indexSetId) {
        final IndexSet indexSet = getIndexSet(indexSetRegistry, indexSetId);

        final Set<String> reopenedIndices = indices.getReopenedIndices(indexSet)
                .stream()
                .filter((indexName) -> isPermitted(RestPermissions.INDICES_READ, indexName) && indexSetRegistry.isManagedIndex(indexName))
                .collect(Collectors.toSet());

        return ClosedIndices.create(reopenedIndices, reopenedIndices.size());
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

    private OpenIndicesInfo getOpenIndicesInfo(Set<IndexStatistics> indicesStats) {
        final Map<String, IndexInfo> indexInfos = new HashMap<>();
        final Map<String, Boolean> areReopened = indices.areReopened(indicesStats.stream().map(IndexStatistics::indexName).collect(Collectors.toSet()));
        for (IndexStatistics indexStatistics : indicesStats) {
            final ImmutableList.Builder<ShardRouting> routing = ImmutableList.builder();
            for (org.elasticsearch.cluster.routing.ShardRouting shardRouting : indexStatistics.shardRoutings()) {
                routing.add(shardRouting(shardRouting));
            }

            final IndexInfo indexInfo = IndexInfo.create(
                    indexStats(indexStatistics.primaries()),
                    indexStats(indexStatistics.total()),
                    routing.build(),
                    areReopened.get(indexStatistics.indexName()));

            indexInfos.put(indexStatistics.indexName(), indexInfo);
        }

        return OpenIndicesInfo.create(indexInfos);
    }
}

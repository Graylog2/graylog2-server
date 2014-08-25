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
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.elasticsearch.action.admin.cluster.node.info.NodeInfo;
import org.elasticsearch.action.admin.cluster.node.info.NodesInfoRequest;
import org.elasticsearch.action.admin.cluster.state.ClusterStateRequest;
import org.elasticsearch.action.admin.indices.close.CloseIndexRequest;
import org.elasticsearch.action.admin.indices.open.OpenIndexRequest;
import org.elasticsearch.action.admin.indices.settings.UpdateSettingsRequest;
import org.elasticsearch.action.admin.indices.stats.CommonStats;
import org.elasticsearch.action.admin.indices.stats.IndexStats;
import org.elasticsearch.action.admin.indices.stats.IndicesStatsRequest;
import org.elasticsearch.action.admin.indices.stats.IndicesStatsResponse;
import org.elasticsearch.action.admin.indices.stats.ShardStats;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.cluster.metadata.IndexMetaData;
import org.elasticsearch.cluster.routing.ShardRouting;
import org.graylog2.Configuration;
import org.graylog2.indexer.Deflector;
import org.graylog2.indexer.Indexer;
import org.graylog2.indexer.ranges.RebuildIndexRangesJob;
import org.graylog2.rest.documentation.annotations.Api;
import org.graylog2.rest.documentation.annotations.ApiOperation;
import org.graylog2.rest.documentation.annotations.ApiParam;
import org.graylog2.rest.documentation.annotations.ApiResponse;
import org.graylog2.rest.documentation.annotations.ApiResponses;
import org.graylog2.rest.resources.RestResource;
import org.graylog2.security.RestPermissions;
import org.graylog2.system.jobs.SystemJob;
import org.graylog2.system.jobs.SystemJobConcurrencyException;
import org.graylog2.system.jobs.SystemJobManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

@RequiresAuthentication
@Api(value = "Indexer/Indices", description = "Index information")
@Path("/system/indexer/indices")
public class IndicesResource extends RestResource {
    private static final Logger LOG = LoggerFactory.getLogger(IndicesResource.class);

    @Inject
    private RebuildIndexRangesJob.Factory rebuildIndexRangesJobFactory;
    @Inject
    private Indexer indexer;
    @Inject
    private Deflector deflector;
    @Inject
    private SystemJobManager systemJobManager;
    @Inject
    private Configuration configuration;

    @GET
    @Timed
    @Path("/{index}")
    @ApiOperation(value = "Get information of an index and its shards.")
    @Produces(MediaType.APPLICATION_JSON)
    public Response single(@ApiParam(title = "index") @PathParam("index") String index) {
        checkPermission(RestPermissions.INDICES_READ, index);

        final ImmutableMap.Builder<String, Object> result = ImmutableMap.builder();

        try {
            final IndicesStatsResponse indicesStatsResponse = indexer.getClient().admin().indices().stats(new IndicesStatsRequest().all()).get();
            final IndexStats indexStats = indicesStatsResponse.getIndex(index);

            if (indexStats == null) {
                LOG.warn("Index [{}] not found.", index);
                return Response.status(Response.Status.NOT_FOUND).build();
            }

            final ImmutableList.Builder<Map<String, Object>> routing = ImmutableList.builder();
            for (ShardStats shardStats : indexStats.getShards()) {
                routing.add(shardRouting(shardStats.getShardRouting()));
            }

            result.put("primary_shards", indexStats(indexStats.getPrimaries()));
            result.put("all_shards", indexStats(indexStats.getTotal()));
            result.put("routing", routing.build());
            result.put("is_reopened", indexer.indices().isReopened(index));
        } catch (Exception e) {
            LOG.error("Could not get indices information.", e);
            return Response.serverError().build();
        }

        return Response.ok().entity(json(result.build())).build();
    }

    @GET
    @Timed
    @Path("/closed")
    @ApiOperation(value = "Get a list of closed indices that can be reopened.")
    @Produces(MediaType.APPLICATION_JSON)
    public Response closed() {
        final ImmutableSet.Builder<String> closedIndicesBuilder = ImmutableSet.builder();

        try {
            // Get a list of all indices and select those that are closed. This is only possible via metadata.
            final ClusterStateRequest csr = new ClusterStateRequest()
                    .filterNodes(true)
                    .filterRoutingTable(true)
                    .filterBlocks(true)
                    .filterMetaData(false);
            final ClusterState state = indexer.getClient().admin().cluster().state(csr).actionGet().getState();

            final Iterator<IndexMetaData> it = state.getMetaData().getIndices().valuesIt();
            while (it.hasNext()) {
                final IndexMetaData indexMeta = it.next();

                // Only search in our indices.
                if (!indexMeta.getIndex().startsWith(configuration.getElasticSearchIndexPrefix())) {
                    continue;
                }
                if (!isPermitted(RestPermissions.INDICES_READ, indexMeta.getIndex())) {
                    continue;
                }
                if (indexMeta.getState().equals(IndexMetaData.State.CLOSE)) {
                    closedIndicesBuilder.add(indexMeta.getIndex());
                }
            }
        } catch (Exception e) {
            LOG.error("Could not get closed indices.", e);
            return Response.serverError().build();
        }

        final Set<String> closedIndices = closedIndicesBuilder.build();
        final Map<String, Object> result = ImmutableMap.of(
                "indices", closedIndices,
                "total", closedIndices.size());

        return Response.ok().entity(json(result)).build();
    }

    @POST
    @Timed
    @Path("/{index}/reopen")
    @ApiOperation(value = "Reopen a closed index. This will also trigger an index ranges rebuild job.")
    @Produces(MediaType.APPLICATION_JSON)
    public Response reopen(@ApiParam(title = "index") @PathParam("index") String index) {
        checkPermission(RestPermissions.INDICES_CHANGESTATE, index);

        // Mark this index as re-opened. It will never be touched by retention.
        final UpdateSettingsRequest settings = new UpdateSettingsRequest(index);
        settings.settings(ImmutableMap.of("graylog2_reopened", true));
        indexer.getClient().admin().indices().updateSettings(settings).actionGet();

        // Open index.
        indexer.getClient().admin().indices().open(new OpenIndexRequest(index)).actionGet();

        // Trigger index ranges rebuild job.
        final SystemJob rebuildJob = rebuildIndexRangesJobFactory.create(deflector);
        try {
            systemJobManager.submit(rebuildJob);
        } catch (SystemJobConcurrencyException e) {
            LOG.error("Concurrency level of this job reached: " + e.getMessage());
            throw new WebApplicationException(Response.Status.FORBIDDEN);
        }

        return Response.noContent().build();
    }

    @POST
    @Timed
    @Path("/{index}/close")
    @ApiOperation(value = "Close an index. This will also trigger an index ranges rebuild job.")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 403, message = "You cannot close the current deflector target index.")
    })
    public Response close(@ApiParam(title = "index") @PathParam("index") String index) {
        checkPermission(RestPermissions.INDICES_CHANGESTATE, index);

        if (deflector.getCurrentActualTargetIndex(indexer).equals(index)) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }

        // Close index.
        indexer.getClient().admin().indices().close(new CloseIndexRequest(index)).actionGet();

        // Trigger index ranges rebuild job.
        final SystemJob rebuildJob = rebuildIndexRangesJobFactory.create(deflector);
        try {
            systemJobManager.submit(rebuildJob);
        } catch (SystemJobConcurrencyException e) {
            LOG.error("Concurrency level of this job reached: " + e.getMessage());
            throw new WebApplicationException(Response.Status.FORBIDDEN);
        }

        return Response.noContent().build();
    }

    @DELETE
    @Timed
    @Path("/{index}")
    @ApiOperation(value = "Delete an index. This will also trigger an index ranges rebuild job.")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 403, message = "You cannot delete the current deflector target index.")
    })
    public Response delete(@ApiParam(title = "index") @PathParam("index") String index) {
        checkPermission(RestPermissions.INDICES_DELETE, index);

        if (deflector.getCurrentActualTargetIndex(indexer).equals(index)) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }

        // Delete index.
        indexer.indices().delete(index);

        // Trigger index ranges rebuild job.
        final SystemJob rebuildJob = rebuildIndexRangesJobFactory.create(deflector);
        try {
            systemJobManager.submit(rebuildJob);
        } catch (SystemJobConcurrencyException e) {
            LOG.error("Concurrency level of this job reached: " + e.getMessage());
            throw new WebApplicationException(Response.Status.FORBIDDEN);
        }

        return Response.noContent().build();
    }

    private Map<String, Object> shardRouting(ShardRouting route) {
        final ImmutableMap.Builder<String, Object> result = ImmutableMap.builder();

        result.put("id", route.shardId().getId());
        result.put("state", route.state().name().toLowerCase());
        result.put("active", route.active());
        result.put("primary", route.primary());
        result.put("node_id", route.currentNodeId());
        result.put("node_name", translateESNodeIdToName(route.currentNodeId()));
        result.put("node_hostname", translateESNodeIdToHostname(route.currentNodeId()));
        result.put("relocating_to", route.relocatingNodeId());

        return result.build();
    }

    private Map<String, Object> indexStats(final CommonStats stats) {
        final ImmutableMap.Builder<String, Object> result = ImmutableMap.builder();

        result.put("flush", ImmutableMap.<String, Object>of(
                "total", stats.getFlush().getTotal(),
                "time_seconds", stats.getFlush().getTotalTime().getSeconds()
        ));

        result.put("get", ImmutableMap.<String, Object>of(
                "total", stats.getGet().getCount(),
                "time_seconds", stats.getGet().getTime().getSeconds()
        ));

        result.put("index", ImmutableMap.<String, Object>of(
                "total", stats.getIndexing().getTotal().getIndexCount(),
                "time_seconds", stats.getIndexing().getTotal().getIndexTime().getSeconds()
        ));

        result.put("merge", ImmutableMap.<String, Object>of(
                "total", stats.getMerge().getTotal(),
                "time_seconds", stats.getMerge().getTotalTime().getSeconds()
        ));

        result.put("refresh", ImmutableMap.<String, Object>of(
                "total", stats.getRefresh().getTotal(),
                "time_seconds", stats.getRefresh().getTotalTime().getSeconds()
        ));

        result.put("search_query", ImmutableMap.<String, Object>of(
                "total", stats.getSearch().getTotal().getQueryCount(),
                "time_seconds", stats.getSearch().getTotal().getQueryTime().getSeconds()
        ));

        result.put("search_fetch", ImmutableMap.<String, Object>of(
                "total", stats.getSearch().getTotal().getFetchCount(),
                "time_seconds", stats.getSearch().getTotal().getFetchTime().getSeconds()
        ));

        result.put("open_search_contexts", stats.getSearch().getOpenContexts());
        result.put("store_size_bytes", stats.getStore().getSize().getBytes());
        result.put("segments", stats.getSegments().getCount());

        result.put("documents", ImmutableMap.<String, Object>of(
                "count", stats.getDocs().getCount(),
                "deleted", stats.getDocs().getDeleted()
        ));

        return result.build();
    }

    private String translateESNodeIdToName(String id) {
        final NodeInfo[] result = indexer.getClient().admin().cluster().nodesInfo(new NodesInfoRequest(id)).actionGet().getNodes();
        if (result == null || result.length == 0) {
            return "unknown";
        }

        return result[0].getNode().getName();
    }

    private String translateESNodeIdToHostname(String id) {
        final NodeInfo[] result = indexer.getClient().admin().cluster().nodesInfo(new NodesInfoRequest(id)).actionGet().getNodes();
        if (result == null || result.length == 0) {
            return "unknown";
        }

        return result[0].getHostname();
    }
}

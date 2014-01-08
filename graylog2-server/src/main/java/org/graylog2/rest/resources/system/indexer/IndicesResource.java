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
package org.graylog2.rest.resources.system.indexer;

import com.codahale.metrics.annotation.Timed;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.elasticsearch.action.admin.cluster.node.info.NodeInfo;
import org.elasticsearch.action.admin.cluster.node.info.NodesInfoRequest;
import org.elasticsearch.action.admin.cluster.state.ClusterStateRequest;
import org.elasticsearch.action.admin.indices.close.CloseIndexRequest;
import org.elasticsearch.action.admin.indices.open.OpenIndexRequest;
import org.elasticsearch.action.admin.indices.settings.UpdateSettingsRequest;
import org.elasticsearch.action.admin.indices.stats.*;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.cluster.metadata.IndexMetaData;
import org.elasticsearch.cluster.routing.ShardRouting;
import org.elasticsearch.common.collect.UnmodifiableIterator;
import org.graylog2.indexer.ranges.RebuildIndexRangesJob;
import org.graylog2.rest.documentation.annotations.*;
import org.graylog2.rest.resources.RestResource;
import org.graylog2.security.RestPermissions;
import org.graylog2.system.jobs.SystemJob;
import org.graylog2.system.jobs.SystemJobConcurrencyException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
@RequiresAuthentication
@Api(value = "Indexer/Indices", description = "Index informations")
@Path("/system/indexer/indices")
public class IndicesResource extends RestResource {

    private static final Logger LOG = LoggerFactory.getLogger(IndicesResource.class);

    @GET @Timed
    @Path("/{index}")
    @ApiOperation(value = "Get information of an index and its shards.")
    @Produces(MediaType.APPLICATION_JSON)
    public Response single(@ApiParam(title = "index") @PathParam("index") String index) {
        checkPermission(RestPermissions.INDICES_READ, index);

        Map<String, Object> result = Maps.newHashMap();

        IndexStats indexStats;
        try {
            IndicesStatsResponse indicesStatsResponse = core.getIndexer().getClient().admin().indices().stats(new IndicesStatsRequest().all()).get();
            indexStats = indicesStatsResponse.getIndex(index);

            if (indexStats == null) {
                LOG.error("Index [{}] not found.", index);
                return Response.status(404).build();
            }

            List<Map<String, Object>> routing = Lists.newArrayList();
            for (ShardStats shardStats : indexStats.getShards()) {
                routing.add(shardRouting(shardStats.getShardRouting()));
            }

            result.put("primary_shards", indexStats(indexStats.getPrimaries()));
            result.put("all_shards", indexStats(indexStats.getTotal()));
            result.put("routing", routing);
            result.put("is_reopened", core.getIndexer().indices().isReopened(index));
        } catch (Exception e) {
            LOG.error("Could not get indices information.", e);
            return Response.status(500).build();
        }

        return Response.ok().entity(json(result)).build();
    }

    @GET @Timed
    @Path("/closed")
    @ApiOperation(value = "Get a list of closed indices that can be reopened.")
    @Produces(MediaType.APPLICATION_JSON)
    public Response closed() {
        Map<String, Object> result = Maps.newHashMap();

        Set<String> closedIndices = Sets.newHashSet();
        try {
            // Get a list of all indices and select those that are closed. This is only possible via metadata.
            ClusterStateRequest csr = new ClusterStateRequest()
                    .filterNodes(true)
                    .filterRoutingTable(true)
                    .filterBlocks(true)
                    .filterMetaData(false);
            ClusterState state = core.getIndexer().getClient().admin().cluster().state(csr).actionGet().getState();

            UnmodifiableIterator<IndexMetaData> it = state.getMetaData().getIndices().valuesIt();

            while(it.hasNext()) {
                IndexMetaData indexMeta = it.next();
                // Only search in our indices.
                if (!indexMeta.getIndex().startsWith(core.getConfiguration().getElasticSearchIndexPrefix())) {
                    continue;
                }
                if (!isPermitted(RestPermissions.INDICES_READ, indexMeta.getIndex())) {
                    continue;
                }
                if(indexMeta.getState().equals(IndexMetaData.State.CLOSE)) {
                    closedIndices.add(indexMeta.getIndex());
                }
            }
        } catch (Exception e) {
            LOG.error("Could not get closed indices.", e);
            return Response.status(500).build();
        }

        result.put("indices", closedIndices);
        result.put("total", closedIndices.size());

        return Response.ok().entity(json(result)).build();
    }

    @POST @Timed
    @Path("/{index}/reopen")
    @ApiOperation(value = "Reopen a closed index. This will also trigger an index ranges rebuild job.")
    @Produces(MediaType.APPLICATION_JSON)
    public Response reopen(@ApiParam(title = "index") @PathParam("index") String index) {
        checkPermission(RestPermissions.INDICES_CHANGESTATE, index);

        // Mark this index as re-opened. It will never be touched by retention.
        UpdateSettingsRequest settings = new UpdateSettingsRequest(index);
        settings.settings(new HashMap() {{
            put("graylog2_reopened", true);
        }});
        core.getIndexer().getClient().admin().indices().updateSettings(settings).actionGet();

        // Open index.
        core.getIndexer().getClient().admin().indices().open(new OpenIndexRequest(index)).actionGet();

        // Trigger index ranges rebuild job.
        SystemJob rebuildJob = new RebuildIndexRangesJob(core);
        try {
            core.getSystemJobManager().submit(rebuildJob);
        } catch (SystemJobConcurrencyException e) {
            LOG.error("Concurrency level of this job reached: " + e.getMessage());
            throw new WebApplicationException(403);
        }

        return Response.noContent().build();
    }

    @POST @Timed
    @Path("/{index}/close")
    @ApiOperation(value = "Close an index. This will also trigger an index ranges rebuild job.")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 403, message = "You cannot close the current deflector target index.")
    })
    public Response close(@ApiParam(title = "index") @PathParam("index") String index) {
        checkPermission(RestPermissions.INDICES_CHANGESTATE, index);

        if (core.getDeflector().getCurrentActualTargetIndex().equals(index)) {
            return Response.status(403).build();
        }

        // Close index.
        core.getIndexer().getClient().admin().indices().close(new CloseIndexRequest(index)).actionGet();

        // Trigger index ranges rebuild job.
        SystemJob rebuildJob = new RebuildIndexRangesJob(core);
        try {
            core.getSystemJobManager().submit(rebuildJob);
        } catch (SystemJobConcurrencyException e) {
            LOG.error("Concurrency level of this job reached: " + e.getMessage());
            throw new WebApplicationException(403);
        }

        return Response.noContent().build();
    }

    @DELETE @Timed
    @Path("/{index}")
    @ApiOperation(value = "Delete an index. This will also trigger an index ranges rebuild job.")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 403, message = "You cannot delete the current deflector target index.")
    })
    public Response delete(@ApiParam(title = "index") @PathParam("index") String index) {
        checkPermission(RestPermissions.INDICES_DELETE, index);

        if (core.getDeflector().getCurrentActualTargetIndex().equals(index)) {
            return Response.status(403).build();
        }

        // Delete index.
        core.getIndexer().indices().delete(index);

        // Trigger index ranges rebuild job.
        SystemJob rebuildJob = new RebuildIndexRangesJob(core);
        try {
            core.getSystemJobManager().submit(rebuildJob);
        } catch (SystemJobConcurrencyException e) {
            LOG.error("Concurrency level of this job reached: " + e.getMessage());
            throw new WebApplicationException(403);
        }

        return Response.noContent().build();
    }

    private Map<String, Object> shardRouting(ShardRouting route) {
        Map<String, Object> result = Maps.newHashMap();

        result.put("id", route.shardId().getId());
        result.put("state", route.state().name().toLowerCase());
        result.put("active", route.active());
        result.put("primary", route.primary());
        result.put("node_id", route.currentNodeId());
        result.put("node_name", translateESNodeIdToName(route.currentNodeId()));
        result.put("node_hostname", translateESNodeIdToHostname(route.currentNodeId()));
        result.put("relocating_to", route.relocatingNodeId());

        return result;
    }

    private Map<String, Object> indexStats(final CommonStats stats) {
        Map<String, Object> result = Maps.newHashMap();

        result.put("flush", new HashMap<String, Object>() {{
            put("total", stats.getFlush().getTotal());
            put("time_seconds", stats.getFlush().getTotalTime().getSeconds());
        }});

        result.put("get", new HashMap<String, Object>() {{
            put("total", stats.getGet().getCount());
            put("time_seconds", stats.getGet().getTime().getSeconds());
        }});

        result.put("index", new HashMap<String, Object>() {{
            put("total", stats.getIndexing().getTotal().getIndexCount());
            put("time_seconds", stats.getIndexing().getTotal().getIndexTime().getSeconds());
        }});

        result.put("merge", new HashMap<String, Object>() {{
            put("total", stats.getMerge().getTotal());
            put("time_seconds", stats.getMerge().getTotalTime().getSeconds());
        }});

        result.put("refresh", new HashMap<String, Object>() {{
            put("total", stats.getRefresh().getTotal());
            put("time_seconds", stats.getRefresh().getTotalTime().getSeconds());
        }});

        result.put("search_query", new HashMap<String, Object>() {{
            put("total", stats.getSearch().getTotal().getQueryCount());
            put("time_seconds", stats.getSearch().getTotal().getQueryTime().getSeconds());
        }});

        result.put("search_fetch", new HashMap<String, Object>() {{
            put("total", stats.getSearch().getTotal().getFetchCount());
            put("time_seconds", stats.getSearch().getTotal().getFetchTime().getSeconds());
        }});

        result.put("open_search_contexts", stats.getSearch().getOpenContexts());
        result.put("store_size_bytes", stats.getStore().getSize().getBytes());
        result.put("segments", stats.getSegments().getCount());

        result.put("documents", new HashMap<String, Object>() {{
            put("count", stats.getDocs().getCount());
            put("deleted", stats.getDocs().getDeleted());
        }});

        return result;
    }

    private String translateESNodeIdToName(String id) {
        NodeInfo[] result = core.getIndexer().getClient().admin().cluster().nodesInfo(new NodesInfoRequest(id)).actionGet().getNodes();
        if (result == null || result.length == 0) {
            return "unknown";
        }

        return result[0].getNode().getName();
    }

    private String translateESNodeIdToHostname(String id) {
        NodeInfo[] result = core.getIndexer().getClient().admin().cluster().nodesInfo(new NodesInfoRequest(id)).actionGet().getNodes();
        if (result == null || result.length == 0) {
            return "unknown";
        }

        return result[0].getHostname();
    }

}

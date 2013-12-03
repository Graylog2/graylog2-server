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
import org.elasticsearch.action.admin.cluster.node.info.NodeInfo;
import org.elasticsearch.action.admin.cluster.node.info.NodesInfoRequest;
import org.elasticsearch.action.admin.indices.stats.*;
import org.elasticsearch.cluster.routing.ShardRouting;
import org.graylog2.rest.documentation.annotations.Api;
import org.graylog2.rest.documentation.annotations.ApiOperation;
import org.graylog2.rest.documentation.annotations.ApiParam;
import org.graylog2.rest.resources.RestResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
@Api(value = "Indexer/Indices", description = "Index informations")
@Path("/system/indexer/indices")
public class IndicesResource extends RestResource {

    private static final Logger LOG = LoggerFactory.getLogger(IndicesResource.class);

    @GET @Timed
    @Path("/{index}")
    @ApiOperation(value = "Get information of an index and its shards.")
    @Produces(MediaType.APPLICATION_JSON)
    public Response list(@ApiParam(title = "index") @PathParam("index") String index) {
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
        } catch (Exception e) {
            LOG.error("Could not get indices information.", e);
            return Response.status(500).build();
        }

        return Response.ok().entity(json(result)).build();
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

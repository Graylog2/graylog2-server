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
package org.graylog2.system.stats.elasticsearch;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.google.common.collect.Lists;
import com.google.inject.Singleton;
import io.searchbox.client.JestClient;
import io.searchbox.client.JestResult;
import io.searchbox.cluster.Health;
import io.searchbox.cluster.PendingClusterTasks;
import io.searchbox.cluster.Stats;
import org.graylog2.indexer.IndexSetRegistry;
import org.graylog2.indexer.cluster.jest.JestUtils;
import org.graylog2.indexer.indices.HealthStatus;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

@Singleton
public class ElasticsearchProbe {
    private final JestClient jestClient;
    private final IndexSetRegistry indexSetRegistry;

    @Inject
    public ElasticsearchProbe(JestClient jestClient, IndexSetRegistry indexSetRegistry) {
        this.jestClient = jestClient;
        this.indexSetRegistry = indexSetRegistry;
    }

    public ElasticsearchStats elasticsearchStats() {
        final JestResult clusterStatsResponse = JestUtils.execute(jestClient, new Stats.Builder().build(), () -> "Couldn't read Elasticsearch cluster stats");
        final JsonNode clusterStatsResponseJson = clusterStatsResponse.getJsonObject();
        final String clusterName = clusterStatsResponseJson.path("cluster_name").asText();

        String clusterVersion = null;
        if (clusterStatsResponseJson.path("nodes").path("versions").isArray()) {
            final ArrayNode versions = (ArrayNode) clusterStatsResponseJson.path("nodes").path("versions");
            // We just use the first version in the "versions" array. This is not correct if there are different
            // versions running in the cluster, but that is not recommended anyway.
            final JsonNode versionNode = versions.path(0);
            if (versionNode.getNodeType() != JsonNodeType.MISSING) {
                clusterVersion = versionNode.asText();
            }
        }

        final JsonNode countStats = clusterStatsResponseJson.path("nodes").path("count");

        final NodesStats nodesStats = NodesStats.create(
                countStats.path("total").asInt(-1),
                countStats.path("master_only").asInt(-1),
                countStats.path("data_only").asInt(-1),
                countStats.path("master_data").asInt(-1),
                countStats.path("client").asInt(-1)
        );

        final JsonNode clusterIndicesStats = clusterStatsResponseJson.path("indices");
        final IndicesStats indicesStats = IndicesStats.create(
                clusterIndicesStats.path("count").asInt(-1),
                clusterIndicesStats.path("store").path("size_in_bytes").asLong(-1L),
                clusterIndicesStats.path("fielddata").path("memory_size_in_bytes").asLong(-1L)
        );

        final JestResult pendingClusterTasksResponse = JestUtils.execute(jestClient, new PendingClusterTasks.Builder().build(), () -> "Couldn't read Elasticsearch pending cluster tasks");
        final JsonNode pendingClusterTasks = pendingClusterTasksResponse.getJsonObject().path("tasks");
        final int pendingTasksSize = pendingClusterTasks.size();
        final List<Long> pendingTasksTimeInQueue = Lists.newArrayListWithCapacity(pendingTasksSize);
        for (JsonNode jsonElement : pendingClusterTasks) {
            if (jsonElement.has("time_in_queue_millis")) {
                pendingTasksTimeInQueue.add(jsonElement.get("time_in_queue_millis").asLong());
            }
        }

        final Health clusterHealthRequest = new Health.Builder()
                .addIndex(Arrays.asList(indexSetRegistry.getIndexWildcards()))
                .build();
        final JestResult clusterHealthResponse = JestUtils.execute(jestClient, clusterHealthRequest, () -> "Couldn't read Elasticsearch cluster health");
        final JsonNode clusterHealthJson = clusterHealthResponse.getJsonObject();
        final ClusterHealth clusterHealth = ClusterHealth.create(
                clusterHealthJson.path("number_of_nodes").asInt(-1),
                clusterHealthJson.path("number_of_data_nodes").asInt(-1),
                clusterHealthJson.path("active_shards").asInt(-1),
                clusterHealthJson.path("relocating_shards").asInt(-1),
                clusterHealthJson.path("active_primary_shards").asInt(-1),
                clusterHealthJson.path("initializing_shards").asInt(-1),
                clusterHealthJson.path("unassigned_shards").asInt(-1),
                clusterHealthJson.path("timed_out").asBoolean(),
                pendingTasksSize,
                pendingTasksTimeInQueue
        );
        final HealthStatus healthStatus = getHealthStatus(clusterHealthJson.path("status").asText("RED"));

        return ElasticsearchStats.create(
                clusterName,
                clusterVersion,
                healthStatus,
                clusterHealth,
                nodesStats,
                indicesStats);
    }

    private HealthStatus getHealthStatus(String status) {
        return HealthStatus.fromString(status.toUpperCase(Locale.ENGLISH));
    }
}

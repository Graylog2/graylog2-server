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

import com.google.common.collect.Lists;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.inject.Singleton;
import io.searchbox.client.JestClient;
import io.searchbox.client.JestResult;
import io.searchbox.cluster.Health;
import io.searchbox.cluster.PendingClusterTasks;
import io.searchbox.cluster.Stats;
import org.graylog2.indexer.IndexSetRegistry;

import javax.inject.Inject;
import java.io.IOException;
import java.io.UncheckedIOException;
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
        final JestResult clusterStatsResponse;
        try {
            clusterStatsResponse = jestClient.execute(new Stats.Builder().build());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        final JsonObject json = clusterStatsResponse.getJsonObject();
        final String clusterName = json.get("cluster_name").getAsString();

        final JsonObject clusterNodesStats = json.getAsJsonObject("nodes");
        final JsonObject countStats = clusterNodesStats.getAsJsonObject("count");
        final NodesStats nodesStats = NodesStats.create(
                countStats.get("total").getAsInt(),
                countStats.get("master_only").getAsInt(),
                countStats.get("data_only").getAsInt(),
                countStats.get("master_data").getAsInt(),
                countStats.get("client").getAsInt()
        );

        final JsonObject clusterIndicesStats = json.getAsJsonObject("indices");
        final IndicesStats indicesStats = IndicesStats.create(
                clusterIndicesStats.get("count").getAsInt(),
                clusterIndicesStats.getAsJsonObject("store").get("size_in_bytes").getAsLong(),
                clusterIndicesStats.getAsJsonObject("fielddata").get("memory_size_in_bytes").getAsLong()
        );

        final JestResult pendingClusterTasksResponse;
        try {
            pendingClusterTasksResponse = jestClient.execute(new PendingClusterTasks.Builder().build()
            );
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        final JsonArray pendingClusterTasks = pendingClusterTasksResponse.getJsonObject().getAsJsonArray("tasks");
        final int pendingTasksSize = pendingClusterTasks.size();
        final List<Long> pendingTasksTimeInQueue = Lists.newArrayListWithCapacity(pendingTasksSize);
        for (JsonElement jsonElement : pendingClusterTasks) {
            if (jsonElement.isJsonObject()) {
                final JsonObject pendingClusterTask = jsonElement.getAsJsonObject();
                pendingTasksTimeInQueue.add(pendingClusterTask.get("time_in_queue_millis").getAsLong());
            }
        }

        final Health clusterHealthRequest = new Health.Builder()
                .addIndex(Arrays.asList(indexSetRegistry.getIndexWildcards()))
                .build();
        final JestResult clusterHealthResponse;
        try {
            clusterHealthResponse = jestClient.execute(clusterHealthRequest);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        final JsonObject clusterHealthJson = clusterHealthResponse.getJsonObject();
        final ClusterHealth clusterHealth = ClusterHealth.create(
                clusterHealthJson.get("number_of_nodes").getAsInt(),
                clusterHealthJson.get("number_of_data_nodes").getAsInt(),
                clusterHealthJson.get("active_shards").getAsInt(),
                clusterHealthJson.get("relocating_shards").getAsInt(),
                clusterHealthJson.get("active_primary_shards").getAsInt(),
                clusterHealthJson.get("initializing_shards").getAsInt(),
                clusterHealthJson.get("unassigned_shards").getAsInt(),
                clusterHealthJson.get("timed_out").getAsBoolean(),
                pendingTasksSize,
                pendingTasksTimeInQueue
        );

        final ElasticsearchStats.HealthStatus healthStatus = getHealthStatus(clusterHealthJson.get("status").getAsString());

        return ElasticsearchStats.create(
                clusterName,
                healthStatus,
                clusterHealth,
                nodesStats,
                indicesStats);
    }

    private ElasticsearchStats.HealthStatus getHealthStatus(String status) {
        return ElasticsearchStats.HealthStatus.valueOf(status.toUpperCase(Locale.ENGLISH));
    }
}

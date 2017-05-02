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
import org.graylog2.indexer.cluster.jest.JestUtils;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static org.graylog2.indexer.gson.GsonUtils.asBoolean;
import static org.graylog2.indexer.gson.GsonUtils.asInteger;
import static org.graylog2.indexer.gson.GsonUtils.asJsonArray;
import static org.graylog2.indexer.gson.GsonUtils.asJsonObject;
import static org.graylog2.indexer.gson.GsonUtils.asLong;
import static org.graylog2.indexer.gson.GsonUtils.asString;

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
        final JsonObject clusterStatsResponseJson = clusterStatsResponse.getJsonObject();
        final String clusterName = asString(clusterStatsResponseJson.get("cluster_name"));

        final Optional<JsonObject> countStats = Optional.ofNullable(asJsonObject(clusterStatsResponseJson.get("nodes")))
                .map(nodes -> asJsonObject(nodes.get("count")));

        final NodesStats nodesStats = NodesStats.create(
                countStats.map(count -> asInteger(count.get("total"))).orElse(-1),
                countStats.map(count -> asInteger(count.get("master_only"))).orElse(-1),
                countStats.map(count -> asInteger(count.get("data_only"))).orElse(-1),
                countStats.map(count -> asInteger(count.get("master_data"))).orElse(-1),
                countStats.map(count -> asInteger(count.get("client"))).orElse(-1)
        );

        final Optional<JsonObject> clusterIndicesStats = Optional.ofNullable(asJsonObject(clusterStatsResponseJson.get("indices")));
        final IndicesStats indicesStats = IndicesStats.create(
                clusterIndicesStats.map(indices -> asInteger(indices.get("count"))).orElse(-1),
                clusterIndicesStats
                        .map(indices -> asJsonObject(indices.get("store")))
                        .map(store -> asLong(store.get("size_in_bytes")))
                        .orElse(-1L),
                clusterIndicesStats
                        .map(indices -> asJsonObject(indices.get("fielddata")))
                        .map(fielddata -> asLong(fielddata.get("memory_size_in_bytes")))
                        .orElse(-1L)
        );

        final JestResult pendingClusterTasksResponse = JestUtils.execute(jestClient, new PendingClusterTasks.Builder().build(), () -> "Couldn't read Elasticsearch pending cluster tasks");
        final JsonArray pendingClusterTasks = Optional.of(pendingClusterTasksResponse.getJsonObject())
                .map(json -> asJsonArray(json.get("tasks")))
                .orElse(new JsonArray());
        final int pendingTasksSize = pendingClusterTasks.size();
        final List<Long> pendingTasksTimeInQueue = Lists.newArrayListWithCapacity(pendingTasksSize);
        for (JsonElement jsonElement : pendingClusterTasks) {
            Optional.ofNullable(asJsonObject(jsonElement))
                    .map(pendingClusterTask -> asLong(pendingClusterTask.get("time_in_queue_millis")))
                    .ifPresent(pendingTasksTimeInQueue::add);
        }

        final Health clusterHealthRequest = new Health.Builder()
                .addIndex(Arrays.asList(indexSetRegistry.getIndexWildcards()))
                .build();
        final JestResult clusterHealthResponse = JestUtils.execute(jestClient, clusterHealthRequest, () -> "Couldn't read Elasticsearch cluster health");
        final Optional<JsonObject> clusterHealthJson = Optional.of(clusterHealthResponse.getJsonObject());
        final ClusterHealth clusterHealth = ClusterHealth.create(
                clusterHealthJson.map(json -> asInteger(json.get("number_of_nodes"))).orElse(-1),
                clusterHealthJson.map(json -> asInteger(json.get("number_of_data_nodes"))).orElse(-1),
                clusterHealthJson.map(json -> asInteger(json.get("active_shards"))).orElse(-1),
                clusterHealthJson.map(json -> asInteger(json.get("relocating_shards"))).orElse(-1),
                clusterHealthJson.map(json -> asInteger(json.get("active_primary_shards"))).orElse(-1),
                clusterHealthJson.map(json -> asInteger(json.get("initializing_shards"))).orElse(-1),
                clusterHealthJson.map(json -> asInteger(json.get("unassigned_shards"))).orElse(-1),
                clusterHealthJson.map(json -> asBoolean(json.get("timed_out"))).orElse(false),
                pendingTasksSize,
                pendingTasksTimeInQueue
        );

        final ElasticsearchStats.HealthStatus healthStatus = clusterHealthJson
                .map(json -> asString(json.get("status")))
                .map(this::getHealthStatus)
                .orElse(ElasticsearchStats.HealthStatus.RED);

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

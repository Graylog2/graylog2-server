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
package org.graylog2.indexer.indices;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.graylog2.indexer.cluster.Cluster;
import org.graylog2.rest.models.system.indexer.responses.IndexInfo;
import org.graylog2.rest.models.system.indexer.responses.IndexStats;
import org.graylog2.rest.models.system.indexer.responses.ShardRouting;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import static com.google.common.base.MoreObjects.firstNonNull;
import static org.graylog2.indexer.gson.GsonUtils.asBoolean;
import static org.graylog2.indexer.gson.GsonUtils.asJsonArray;
import static org.graylog2.indexer.gson.GsonUtils.asJsonObject;
import static org.graylog2.indexer.gson.GsonUtils.asLong;
import static org.graylog2.indexer.gson.GsonUtils.asString;

@AutoValue
public abstract class IndexStatistics {
    public abstract String indexName();

    public abstract JsonObject primaries();

    public abstract JsonObject total();

    public abstract JsonObject shardRoutings();

    public static IndexStatistics create(String indexName,
                                         JsonObject primaries,
                                         JsonObject total,
                                         JsonObject shardRoutings) {
        return new AutoValue_IndexStatistics(indexName, primaries, total, shardRoutings);
    }

    public IndexInfo toIndexInfo(boolean isReopened, Cluster cluster) {
        return IndexInfo.create(buildIndexStats(primaries()), buildIndexStats(total()), buildShardRoutings(shardRoutings(), cluster), isReopened);
    }

    private IndexStats buildIndexStats(final JsonObject stats) {
        final Optional<JsonObject> flush = Optional.of(stats).map(json -> asJsonObject(json.get("flush")));
        final long flushTotal = flush.map(json -> asLong(json.get("total"))).orElse(0L);
        final long flushTotalTimeSeconds = flush.map(json -> asLong(json.get("total_time_in_millis"))).map(ms -> ms / 1000L).orElse(0L);

        final Optional<JsonObject> get = Optional.of(stats).map(json -> asJsonObject(json.get("get")));
        final long getTotal = get.map(json -> asLong(json.get("total"))).orElse(0L);
        final long getTotalTimeSeconds = get.map(json -> asLong(json.get("total_time_in_millis"))).map(ms -> ms / 1000L).orElse(0L);

        final Optional<JsonObject> indexing = Optional.of(stats).map(json -> asJsonObject(json.get("indexing")));
        final long indexingTotal = indexing.map(json -> asLong(json.get("total"))).orElse(0L);
        final long indexingTotalTimeSeconds = indexing.map(json -> asLong(json.get("total_time_in_millis"))).map(ms -> ms / 1000L).orElse(0L);

        final Optional<JsonObject> merge = Optional.of(stats).map(json -> asJsonObject(json.get("merge")));
        final long mergeTotal = merge.map(json -> asLong(json.get("total"))).orElse(0L);
        final long mergeTotalTimeSeconds = merge.map(json -> asLong(json.get("total_time_in_millis"))).map(ms -> ms / 1000L).orElse(0L);

        final Optional<JsonObject> refresh = Optional.of(stats).map(json -> asJsonObject(json.get("refresh")));
        final long refreshTotal = refresh.map(json -> asLong(json.get("total"))).orElse(0L);
        final long refreshTotalTimeSeconds = refresh.map(json -> asLong(json.get("total_time_in_millis"))).map(ms -> ms / 1000L).orElse(0L);

        final Optional<JsonObject> search = Optional.of(stats).map(json -> asJsonObject(json.get("search")));
        final long searchQueryTotal = search.map(json -> asLong(json.get("query_total"))).orElse(0L);
        final long searchQueryTotalTimeSeconds = search.map(json -> asLong(json.get("query_time_in_millis"))).map(ms -> ms / 1000L).orElse(0L);
        final long searchFetchTotal = search.map(json -> asLong(json.get("fetch_total"))).orElse(0L);
        final long searchFetchTotalTimeSeconds = search.map(json -> asLong(json.get("fetch_time_in_millis"))).map(ms -> ms / 1000L).orElse(0L);
        final long searchOpenContexts = search.map(json -> asLong(json.get("open_contexts"))).orElse(0L);

        final long storeSizeInBytes = Optional.of(stats)
                .map(json -> asJsonObject(json.get("store")))
                .map(json -> asLong(json.get("size_in_bytes")))
                .orElse(0L);

        final long segmentsCount = Optional.of(stats)
                .map(json -> asJsonObject(json.get("segments")))
                .map(json -> asLong(json.get("count")))
                .orElse(0L);

        final Optional<JsonObject> docs = Optional.of(stats).map(json -> asJsonObject(json.get("docs")));
        final long docsCount = docs.map(json -> asLong(json.get("count"))).orElse(0L);
        final long docsDeleted = docs.map(json -> asLong(json.get("deleted"))).orElse(0L);

        return IndexStats.create(
                IndexStats.TimeAndTotalStats.create(flushTotal, flushTotalTimeSeconds),
                IndexStats.TimeAndTotalStats.create(getTotal, getTotalTimeSeconds),
                IndexStats.TimeAndTotalStats.create(indexingTotal, indexingTotalTimeSeconds),
                IndexStats.TimeAndTotalStats.create(mergeTotal, mergeTotalTimeSeconds),
                IndexStats.TimeAndTotalStats.create(refreshTotal, refreshTotalTimeSeconds),
                IndexStats.TimeAndTotalStats.create(searchQueryTotal, searchQueryTotalTimeSeconds),
                IndexStats.TimeAndTotalStats.create(searchFetchTotal, searchFetchTotalTimeSeconds),
                searchOpenContexts,
                storeSizeInBytes,
                segmentsCount,
                IndexStats.DocsStats.create(docsCount, docsDeleted)
        );
    }

    private List<ShardRouting> buildShardRoutings(JsonObject shardRoutings, Cluster cluster) {
        final ImmutableList.Builder<ShardRouting> shardRoutingsBuilder = ImmutableList.builder();
        for (Map.Entry<String, JsonElement> entry : shardRoutings.entrySet()) {
            final int shardId = Integer.parseInt(entry.getKey());
            final JsonArray shards = firstNonNull(asJsonArray(entry.getValue()), new JsonArray());

            for (JsonElement jsonElement : shards) {
                final Optional<JsonObject> routing = Optional.ofNullable(asJsonObject(jsonElement))
                        .map(json -> asJsonObject(json.get("routing")));
                final String state = routing.map(json -> asString(json.get("state")))
                        .map(s -> s.toLowerCase(Locale.ENGLISH))
                        .orElse("unknown");
                // Taken from org.elasticsearch.cluster.routing.ShardRouting
                final boolean active = "started".equals(state) || "relocating".equals(state);

                final boolean primary = routing.map(json -> asBoolean(json.get("primary"))).orElse(false);
                final String nodeId = routing.map(json -> asString(json.get("node"))).orElse("Unknown");

                final String nodeName = cluster.nodeIdToName(nodeId).orElse(null);
                final String nodeHostname = cluster.nodeIdToHostName(nodeId).orElse(null);

                final String relocatingNode = routing.map(json -> asString(json.get("relocating_node"))).orElse(null);

                final ShardRouting shardRouting = ShardRouting.create(shardId, state, active, primary, nodeId, nodeName, nodeHostname, relocatingNode);
                shardRoutingsBuilder.add(shardRouting);
            }


        }
        return shardRoutingsBuilder.build();
    }
}

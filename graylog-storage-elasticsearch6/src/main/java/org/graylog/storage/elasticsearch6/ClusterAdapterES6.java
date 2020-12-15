/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
package org.graylog.storage.elasticsearch6;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.databind.node.MissingNode;
import com.github.joschi.jadconfig.util.Duration;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.primitives.Ints;
import io.searchbox.client.JestClient;
import io.searchbox.client.JestResult;
import io.searchbox.cluster.Health;
import io.searchbox.cluster.NodesInfo;
import io.searchbox.cluster.PendingClusterTasks;
import io.searchbox.cluster.Stats;
import io.searchbox.core.Cat;
import io.searchbox.core.CatResult;
import org.graylog.storage.elasticsearch6.cluster.GetAllocationDiskSettings;
import org.graylog.storage.elasticsearch6.jest.JestUtils;
import org.graylog2.indexer.ElasticsearchException;
import org.graylog2.indexer.cluster.ClusterAdapter;
import org.graylog2.indexer.cluster.PendingTasksStats;
import org.graylog2.indexer.cluster.health.ClusterAllocationDiskSettings;
import org.graylog2.indexer.cluster.health.ClusterAllocationDiskSettingsFactory;
import org.graylog2.indexer.cluster.health.NodeDiskUsageStats;
import org.graylog2.indexer.cluster.health.NodeFileDescriptorStats;
import org.graylog2.indexer.indices.HealthStatus;
import org.graylog2.rest.models.system.indexer.responses.ClusterHealth;
import org.graylog2.system.stats.elasticsearch.ClusterStats;
import org.graylog2.system.stats.elasticsearch.IndicesStats;
import org.graylog2.system.stats.elasticsearch.NodesStats;
import org.graylog2.system.stats.elasticsearch.ShardStats;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

public class ClusterAdapterES6 implements ClusterAdapter {
    private static final Logger LOG = LoggerFactory.getLogger(ClusterAdapterES6.class);
    private final JestClient jestClient;
    private final Duration requestTimeout;

    @Inject
    public ClusterAdapterES6(JestClient jestClient,
                             @Named("elasticsearch_socket_timeout") Duration requestTimeout) {
        this.jestClient = jestClient;
        this.requestTimeout = requestTimeout;
    }

    @Override
    public Optional<HealthStatus> health(Collection<String> indices) {
        final Optional<JsonNode> result = clusterHealth(indices);
        return result.map(this::extractHealthStatus);
    }

    private HealthStatus extractHealthStatus(JsonNode node) {
        final String statusString = node.path("status").asText().toLowerCase(Locale.ENGLISH);
        return HealthStatus.fromString(statusString);
    }

    @Override
    public Set<NodeFileDescriptorStats> fileDescriptorStats() {
        final JsonNode nodes = catNodes("name", "host", "ip", "fileDescriptorMax");
        final ImmutableSet.Builder<NodeFileDescriptorStats> setBuilder = ImmutableSet.builder();
        for (JsonNode jsonElement : nodes) {
            if (jsonElement.isObject()) {
                final String name = jsonElement.path("name").asText();
                final String host = jsonElement.path("host").asText(null);
                final String ip = jsonElement.path("ip").asText();
                final JsonNode fileDescriptorMax = jsonElement.path("fileDescriptorMax");
                final Long maxFileDescriptors = fileDescriptorMax.isLong() ? fileDescriptorMax.asLong() : null;
                setBuilder.add(NodeFileDescriptorStats.create(name, ip, host, maxFileDescriptors));
            }
        }

        return setBuilder.build();
    }

    @Override
    public Set<NodeDiskUsageStats> diskUsageStats() {
        final JsonNode nodes = catNodes("name", "host", "ip", "diskUsed", "diskTotal","diskUsedPercent");
        final ImmutableSet.Builder<NodeDiskUsageStats> setBuilder = ImmutableSet.builder();
        for (JsonNode jsonElement : nodes) {
            if (jsonElement.isObject()) {
                setBuilder.add(
                        NodeDiskUsageStats.create(
                                jsonElement.path("name").asText(),
                                jsonElement.path("ip").asText(),
                                jsonElement.path("host").asText(null),
                                jsonElement.path("diskUsed").asText(),
                                jsonElement.path("diskTotal").asText(),
                                jsonElement.path("diskUsedPercent").asDouble(NodeDiskUsageStats.DEFAULT_DISK_USED_PERCENT)
                        )
                );
            }
        }
        return setBuilder.build();
    }

    @Override
    public ClusterAllocationDiskSettings clusterAllocationDiskSettings() {
        final GetAllocationDiskSettings request = new GetAllocationDiskSettings.Builder().build();
        final JestResult response = JestUtils.execute(jestClient, request, () -> "Unable to read Elasticsearch cluster settings");
        final JsonNode json = response.getJsonObject();
        final JsonNode floodStageSetting = findEffectiveSettingInSettingsGroups(json, "flood_stage", true);
        return ClusterAllocationDiskSettingsFactory.create(
                findEffectiveSettingInSettingsGroups(json, "threshold_enabled", false).asBoolean(),
                findEffectiveSettingInSettingsGroups(json, "low", false).asText(),
                findEffectiveSettingInSettingsGroups(json, "high", false).asText(),
                floodStageSetting != null ? floodStageSetting.asText() : ""
        );
    }

    @Override
    public Optional<String> nodeIdToName(String nodeId) {
        return Optional.ofNullable(getNodeInfo(nodeId).path("name").asText(null));
    }

    @Override
    public Optional<String> nodeIdToHostName(String nodeId) {
        return Optional.ofNullable(getNodeInfo(nodeId).path("host").asText(null));
    }

    @Override
    public boolean isConnected() {
        final Health request = new Health.Builder()
                .local()
                .timeout(Ints.saturatedCast(requestTimeout.toSeconds()))
                .build();

        try {
            final JestResult result = JestUtils.execute(jestClient, request, () -> "Couldn't check connection status of Elasticsearch");
            final int numberOfDataNodes = result.getJsonObject().path("number_of_data_nodes").asInt();
            return numberOfDataNodes > 0;
        } catch (ElasticsearchException e) {
            if (LOG.isDebugEnabled()) {
                LOG.error(e.getMessage(), e);
            }
            return false;
        }
    }

    @Override
    public Optional<String> clusterName(Collection<String> indices) {
        return clusterHealth(indices).map(health -> health.path("cluster_name").asText("<unknown>"));
    }

    @Override
    public Optional<ClusterHealth> clusterHealthStats(Collection<String> indices) {
        return clusterHealth(indices).map(health -> {
            final ClusterHealth.ShardStatus shards = ClusterHealth.ShardStatus.create(
                    health.path("active_shards").asInt(),
                    health.path("initializing_shards").asInt(),
                    health.path("relocating_shards").asInt(),
                    health.path("unassigned_shards").asInt());

            return ClusterHealth.create(health.path("status").asText().toLowerCase(Locale.ENGLISH), shards);
        });
    }

    @Override
    public ShardStats shardStats(Collection<String> indices) {
        final Health clusterHealthRequest = new Health.Builder()
                .addIndex(indices)
                .build();
        final JestResult clusterHealthResponse = JestUtils.execute(jestClient, clusterHealthRequest, () -> "Couldn't read Elasticsearch cluster health");
        final JsonNode clusterHealthJson = clusterHealthResponse.getJsonObject();
        return ShardStats.create(
                clusterHealthJson.path("number_of_nodes").asInt(-1),
                clusterHealthJson.path("number_of_data_nodes").asInt(-1),
                clusterHealthJson.path("active_shards").asInt(-1),
                clusterHealthJson.path("relocating_shards").asInt(-1),
                clusterHealthJson.path("active_primary_shards").asInt(-1),
                clusterHealthJson.path("initializing_shards").asInt(-1),
                clusterHealthJson.path("unassigned_shards").asInt(-1),
                clusterHealthJson.path("timed_out").asBoolean()
        );
    }

    @Override
    public PendingTasksStats pendingTasks() {
        final JestResult pendingClusterTasksResponse = JestUtils.execute(jestClient, new PendingClusterTasks.Builder().build(), () -> "Couldn't read Elasticsearch pending cluster tasks");
        final JsonNode pendingClusterTasks = pendingClusterTasksResponse.getJsonObject().path("tasks");
        final int pendingTasksSize = pendingClusterTasks.size();
        final List<Long> pendingTasksTimeInQueue = Lists.newArrayListWithCapacity(pendingTasksSize);
        for (JsonNode jsonElement : pendingClusterTasks) {
            if (jsonElement.has("time_in_queue_millis")) {
                pendingTasksTimeInQueue.add(jsonElement.get("time_in_queue_millis").asLong());
            }
        }

        return PendingTasksStats.create(pendingTasksSize, pendingTasksTimeInQueue);
    }

    @Override
    public ClusterStats clusterStats() {
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

        return ClusterStats.create(clusterName, clusterVersion, nodesStats, indicesStats);
    }

    private JsonNode getNodeInfo(String nodeId) {
        if (nodeId == null || nodeId.isEmpty()) {
            return MissingNode.getInstance();
        }

        final NodesInfo request = new NodesInfo.Builder().addNode(nodeId).build();
        final JestResult result = JestUtils.execute(jestClient, request, () -> "Couldn't read information of Elasticsearch node " + nodeId);
        return result.getJsonObject().path("nodes").path(nodeId);
    }

    private JsonNode findEffectiveSettingInSettingsGroups(JsonNode jsonNode, String setting, boolean optional) {
        List<String> settingsGroup = Arrays.asList("transient", "persistent", "defaults");
        for(String group: settingsGroup) {
            JsonNode foundGroup = jsonNode.findPath(group);
            if (!(foundGroup instanceof MissingNode)) {
                JsonNode foundSetting = foundGroup.findPath(setting);
                if (!(foundSetting instanceof MissingNode)) {
                    return foundSetting;
                }
            }
        }
        if (optional) {
            return null;
        }
        throw new IllegalStateException(String.format(Locale.ENGLISH, "Could not find setting %s in Elasticsearch response", setting));
    }

    /**
     * Retrieve the response for the <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/cat-nodes.html">cat nodes</a> request from Elasticsearch.
     *
     * @param fields The fields to show, see <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/cat-nodes.html">cat nodes API</a>.
     * @return A {@link JsonNode} with the result of the cat nodes request.
     */
    private JsonNode catNodes(String... fields) {
        final String fieldNames = String.join(",", fields);
        final Cat request = new Cat.NodesBuilder()
                .setParameter("h", fieldNames)
                .setParameter("full_id", true)
                .setParameter("format", "json")
                .build();
        final CatResult response = JestUtils.execute(jestClient, request, () -> "Unable to read Elasticsearch node information");
        return response.getJsonObject().path("result");
    }

    private Optional<JsonNode> clusterHealth(Collection<? extends String> indices) {
        final Health request = new Health.Builder()
                .addIndex(indices)
                .timeout(Ints.saturatedCast(requestTimeout.toSeconds()))
                .build();
        try {
            final JestResult jestResult = JestUtils.execute(jestClient, request, () -> "Couldn't read cluster health for indices " + indices);
            return Optional.of(jestResult.getJsonObject());
        } catch(ElasticsearchException e) {
            if (LOG.isDebugEnabled()) {
                LOG.error("{} ({})", e.getMessage(), Optional.ofNullable(e.getCause()).map(Throwable::getMessage).orElse("n/a"), e);
            } else {
                LOG.error("{} ({})", e.getMessage(), Optional.ofNullable(e.getCause()).map(Throwable::getMessage).orElse("n/a"));
            }
            return Optional.empty();
        }
    }
}

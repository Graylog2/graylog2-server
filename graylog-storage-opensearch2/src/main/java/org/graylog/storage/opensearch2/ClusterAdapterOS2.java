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
package org.graylog.storage.opensearch2;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.github.joschi.jadconfig.util.Duration;
import com.google.common.base.Strings;
import com.google.common.primitives.Ints;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.json.JsonObject;
import org.graylog.shaded.opensearch2.org.opensearch.OpenSearchException;
import org.graylog.storage.opensearch2.cat.NodeResponse;
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
import org.graylog2.system.stats.elasticsearch.NodeInfo;
import org.graylog2.system.stats.elasticsearch.NodesStats;
import org.graylog2.system.stats.elasticsearch.ShardStats;
import org.opensearch.client.Request;
import org.opensearch.client.json.JsonData;
import org.opensearch.client.opensearch.cat.aliases.AliasesRecord;
import org.opensearch.client.opensearch.cat.indices.IndicesRecord;
import org.opensearch.client.opensearch.cat.nodes.NodesRecord;
import org.opensearch.client.opensearch.cluster.HealthResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class ClusterAdapterOS2 implements ClusterAdapter {
    private static final Logger LOG = LoggerFactory.getLogger(ClusterAdapterOS2.class);
    private final OpenSearchClient client;
    private final Duration requestTimeout;
    private final PlainJsonApi jsonApi;

    @Inject
    public ClusterAdapterOS2(OpenSearchClient client,
                             @Named("elasticsearch_socket_timeout") Duration requestTimeout,
                             PlainJsonApi jsonApi) {
        this.client = client;
        this.requestTimeout = requestTimeout;
        this.jsonApi = jsonApi;
    }

    @Override
    public Optional<HealthStatus> health() {
        return clusterHealth().map(response -> healthStatusFrom(response.status()));
    }

    private HealthStatus healthStatusFrom(org.opensearch.client.opensearch._types.HealthStatus status) {
        switch (status) {
            case Red:
                return HealthStatus.Red;
            case Yellow:
                return HealthStatus.Yellow;
            case Green:
                return HealthStatus.Green;
        }

        throw new IllegalStateException("Invalid health status received: " + status);
    }

    @Override
    public Set<NodeFileDescriptorStats> fileDescriptorStats() {
        final List<NodeResponse> result = nodes();
        return result.stream()
                .map(node -> NodeFileDescriptorStats.create(node.name(), node.ip(), node.host(), node.fileDescriptorMax()))
                .collect(Collectors.toSet());
    }

    private List<NodeResponse> nodes() {
        final var response = client.execute(c -> c.cat().nodes(builder -> builder.fullId(true)
                .headers("id,name,role,host,ip,fileDescriptorMax,diskUsed,diskTotal,diskUsedPercent")));
        final var allNodes = response.valueBody()
                .stream()
                .map(this::createNodeResponse)
                .toList();
        final var nodesWithDiskStatistics = allNodes.stream().filter(NodeResponse::hasDiskStatistics).toList();
        if (allNodes.size() != nodesWithDiskStatistics.size()) {
            final List<NodeResponse> nodesWithMissingDiskStatistics = allNodes.stream().filter(nr -> !nr.hasDiskStatistics()).toList();
            LOG.info("_cat/nodes API has returned " + nodesWithMissingDiskStatistics.size() + " nodes without disk statistics:");
            nodesWithMissingDiskStatistics.forEach(node -> LOG.info(node.toString()));
        }
        return nodesWithDiskStatistics;
    }

    private NodeResponse createNodeResponse(NodesRecord node) {
        return NodeResponse.create(node.id(), node.name(), node.nodeRole(), node.httpAddress(), node.ip(), node.diskUsed(),
                node.diskTotal(), node.diskUsedPercent() == null ? null : Double.valueOf(node.diskUsedPercent()), node.fileDescMax() == null ? null : Long.valueOf(node.fileDescMax()));
    }

    @Override
    public Set<NodeDiskUsageStats> diskUsageStats() {
        final List<NodeResponse> result = nodes();
        return result.stream()
                .map(node -> NodeDiskUsageStats.create(node.name(), node.role(), node.ip(), node.host(), node.diskUsed(), node.diskTotal(), node.diskUsedPercent()))
                .collect(Collectors.toSet());
    }

    @Override
    public ClusterAllocationDiskSettings clusterAllocationDiskSettings() {
        final var response = client.execute((c) -> c.cluster().getSettings(builder -> builder.includeDefaults(true)));
        final var diskDefaults = getDiskDefaults(response.defaults());
        final var watermarkDefaults = diskDefaults.getJsonObject("watermark");
        LOG.info("Got response: " + diskDefaults);
        return ClusterAllocationDiskSettingsFactory.create(
                Boolean.parseBoolean(diskDefaults.getString("threshold_enabled")),
                watermarkDefaults.getString("low"),
                watermarkDefaults.getString("high"),
                watermarkDefaults.getString("flood_stage")
        );
    }

    private JsonObject getDiskDefaults(Map<String, JsonData> defaults) {
        return defaults.get("cluster").toJson().asJsonObject()
                .getJsonObject("routing")
                .getJsonObject("allocation")
                .getJsonObject("disk");
    }

    @Override
    public Optional<String> nodeIdToName(String nodeId) {
        return nodeById(nodeId)
                .map(Node::name);
    }

    @Override
    public Optional<String> nodeIdToHostName(String nodeId) {
        return nodeById(nodeId)
                .map(Node::host);
    }

    record Node(String id, String name, String host) {}

    private Optional<Node> nodeById(String nodeId) {
        if (Strings.isNullOrEmpty(nodeId)) {
            return Optional.empty();
        }
        final var request = new Request("GET", "/_nodes/" + nodeId);
        final var response = jsonApi.perform(request, "Unable to retrieve node information for node id " + nodeId);

        return Optional.ofNullable(response)
                .map(r -> r.get("nodes"))
                .filter(n -> !n.isMissingNode())
                .map(n -> n.get(nodeId))
                .filter(n -> !n.isMissingNode())
                .map(n -> new Node(nodeId, n.get("name").asText(), n.get("host").asText()));
    }

    @Override
    public boolean isConnected() {
        try {
            final var result = client.execute((c) -> c.cluster().health(builder -> builder.local(true)
                    .timeout(timeBuilder -> timeBuilder.time(durationToString(requestTimeout)))));
            return result.numberOfDataNodes() > 0;
        } catch (OpenSearchException e) {
            LOG.error("Check for connectivity failed with exception '{}' - enable debug level for this class to see the stack trace.", e.getMessage());
            if (LOG.isDebugEnabled()) {
                LOG.error(e.getMessage(), e);
            }
            return false;
        }
    }

    private String durationToString(Duration requestTimeout) {
        return Ints.saturatedCast(requestTimeout.toSeconds()) + "s";
    }

    @Override
    public Optional<String> clusterName() {
        return clusterHealth().map(HealthResponse::clusterName);
    }

    @Override
    public Optional<ClusterHealth> clusterHealthStats() {
        return clusterHealth()
                .map(this::clusterHealthFrom);
    }

    private ClusterHealth clusterHealthFrom(HealthResponse response) {
        return ClusterHealth.create(response.status().toString().toLowerCase(Locale.ENGLISH),
                ClusterHealth.ShardStatus.create(
                        response.activeShards(),
                        response.initializingShards(),
                        response.relocatingShards(),
                        response.unassignedShards()
                )
        );
    }

    @Override
    public PendingTasksStats pendingTasks() {
        final var response = client.execute(c -> c.cluster().pendingTasks(), "Couldn't read pending cluster tasks");

        final var pendingTasksTimeInQueue = response.tasks().stream()
                .map(task -> Long.valueOf(task.timeInQueueMillis()))
                .toList();

        return PendingTasksStats.create(response.tasks().size(), pendingTasksTimeInQueue);
    }

    @Override
    public ClusterStats clusterStats() {
        final JsonNode clusterStatsResponseJson = rawClusterStats();
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

    @Override
    public JsonNode rawClusterStats() {
        final Request request = new Request("GET", "/_cluster/stats/nodes/*");
        return jsonApi.perform(request, "Couldn't read Elasticsearch cluster stats");
    }

    @Override
    public Map<String, NodeInfo> nodesInfo() {
        final var response = client.execute(c -> c.nodes().info(), "Could not read nodes list from indexer!");
        return response.nodes().entrySet()
                .stream().collect(Collectors.toMap(Map.Entry::getKey, entry -> createNodeInfo(entry.getValue())));
    }

    private NodeInfo createNodeInfo(org.opensearch.client.opensearch.nodes.info.NodeInfo nodeInfo) {
        return NodeInfo.builder()
                .version(nodeInfo.version())
                .os(nodeInfo.os())
                .roles(nodeInfo.roles().stream().map(Enum::toString).toList())
                .jvmMemHeapMaxInBytes(nodeInfo.jvm().mem().heapMaxInBytes())
                .build();
    }

    @Override
    public ShardStats shardStats() {
        return clusterHealth()
                .map(response -> ShardStats.create(
                        response.numberOfNodes(),
                        response.numberOfDataNodes(),
                        response.activeShards(),
                        response.relocatingShards(),
                        response.activePrimaryShards(),
                        response.initializingShards(),
                        response.unassignedShards(),
                        response.timedOut()
                ))
                .orElseThrow(() -> new ElasticsearchException("Unable to retrieve shard stats."));
    }

    private Optional<HealthResponse> clusterHealth() {
        try {
            return Optional.of(client.execute((c) -> c.cluster().health(builder -> builder.timeout(timeBuilder -> timeBuilder.time(durationToString(requestTimeout))))));
        } catch (OpenSearchException e) {
            if (LOG.isDebugEnabled()) {
                LOG.error("{} ({})", e.getMessage(), Optional.ofNullable(e.getCause()).map(Throwable::getMessage).orElse("n/a"), e);
            } else {
                LOG.error("{} ({})", e.getMessage(), Optional.ofNullable(e.getCause()).map(Throwable::getMessage).orElse("n/a"));
            }
            return Optional.empty();
        }
    }

    @Override
    public Optional<HealthStatus> deflectorHealth(Collection<String> indices) {
        if (indices.isEmpty()) {
            return Optional.of(HealthStatus.Green);
        }

        final var aliasMapping = client.execute(c -> c.cat().aliases(builder -> builder.headers("alias,index")))
                .valueBody()
                .stream()
                .collect(Collectors.toMap(AliasesRecord::alias, AliasesRecord::index));
        final Set<String> mappedIndices = indices
                .stream()
                .map(index -> aliasMapping.getOrDefault(index, index))
                .collect(Collectors.toSet());

        final var indexSummaries = client.execute(c -> c.cat().indices(builder -> builder.headers("index,status,health")))
                .valueBody()
                .stream()
                .filter(indexSummary -> mappedIndices.contains(indexSummary.index()))
                .collect(Collectors.toSet());

        if (indexSummaries.size() < mappedIndices.size()) {
            return Optional.empty();
        }

        return indexSummaries.stream()
                .map(IndicesRecord::health)
                .map(HealthStatus::fromString)
                .min(HealthStatus::compareTo);
    }
}

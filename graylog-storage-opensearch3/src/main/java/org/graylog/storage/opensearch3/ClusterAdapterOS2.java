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
package org.graylog.storage.opensearch3;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.github.joschi.jadconfig.util.Duration;
import com.google.common.base.Strings;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.graylog2.indexer.ElasticsearchException;
import org.graylog2.indexer.cluster.ClusterAdapter;
import org.graylog2.indexer.cluster.PendingTasksStats;
import org.graylog2.indexer.cluster.health.ClusterAllocationDiskSettings;
import org.graylog2.indexer.cluster.health.ClusterAllocationDiskSettingsFactory;
import org.graylog2.indexer.cluster.health.ClusterShardAllocation;
import org.graylog2.indexer.cluster.health.NodeDiskUsageStats;
import org.graylog2.indexer.cluster.health.NodeFileDescriptorStats;
import org.graylog2.indexer.cluster.health.NodeShardAllocation;
import org.graylog2.indexer.indices.HealthStatus;
import org.graylog2.rest.models.system.indexer.responses.ClusterHealth;
import org.graylog2.system.stats.elasticsearch.ClusterStats;
import org.graylog2.system.stats.elasticsearch.IndicesStats;
import org.graylog2.system.stats.elasticsearch.NodesStats;
import org.graylog2.system.stats.elasticsearch.ShardStats;
import org.opensearch.client.json.JsonData;
import org.opensearch.client.opensearch._types.Time;
import org.opensearch.client.opensearch.cat.OpenSearchCatClient;
import org.opensearch.client.opensearch.cat.aliases.AliasesRecord;
import org.opensearch.client.opensearch.cat.allocation.AllocationRecord;
import org.opensearch.client.opensearch.cat.indices.IndicesRecord;
import org.opensearch.client.opensearch.cat.nodes.NodesRecord;
import org.opensearch.client.opensearch.cluster.GetClusterSettingsRequest;
import org.opensearch.client.opensearch.cluster.GetClusterSettingsResponse;
import org.opensearch.client.opensearch.cluster.HealthRequest;
import org.opensearch.client.opensearch.cluster.HealthResponse;
import org.opensearch.client.opensearch.cluster.OpenSearchClusterClient;
import org.opensearch.client.opensearch.cluster.PendingTasksResponse;
import org.opensearch.client.opensearch.cluster.pending_tasks.PendingTask;
import org.opensearch.client.opensearch.generic.Request;
import org.opensearch.client.opensearch.generic.Requests;
import org.opensearch.client.opensearch.nodes.NodesInfoRequest;
import org.opensearch.client.opensearch.nodes.NodesInfoResponse;
import org.opensearch.client.opensearch.nodes.OpenSearchNodesClient;
import org.opensearch.client.opensearch.nodes.info.NodeInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class ClusterAdapterOS2 implements ClusterAdapter {
    private static final Logger LOG = LoggerFactory.getLogger(ClusterAdapterOS2.class);
    private final Duration requestTimeout;
    private final OfficialOpensearchClient opensearchClient;
    private final PlainJsonApi jsonApi;
    private final OpenSearchCatClient catClient;
    private final OpenSearchClusterClient clusterClient;
    private final OpenSearchNodesClient nodesClient;

    @Inject
    public ClusterAdapterOS2(OfficialOpensearchClient opensearchClient,
                             @Named("elasticsearch_socket_timeout") Duration requestTimeout,
                             PlainJsonApi jsonApi) {
        this.requestTimeout = requestTimeout;
        this.jsonApi = jsonApi;
        this.opensearchClient = opensearchClient;
        this.catClient = opensearchClient.sync().cat();
        this.clusterClient = opensearchClient.sync().cluster();
        this.nodesClient = opensearchClient.sync().nodes();

    }

    @Override
    public Optional<HealthStatus> health() {
        return clusterHealth().map(this::healthStatusFrom);
    }

    private HealthStatus healthStatusFrom(HealthResponse response) {
        return switch (response.status()) {
            case Red -> HealthStatus.Red;
            case Yellow -> HealthStatus.Yellow;
            case Green -> HealthStatus.Green;
        };

    }

    @Override
    public Set<NodeFileDescriptorStats> fileDescriptorStats() {
        final List<NodesRecord> result = nodes();
        return result.stream()
                .map(node -> {
                    assert node.fileDescMax() != null; // checked beforehand in nodes()
                    return NodeFileDescriptorStats.create(node.name(), node.ip(), node.name(), Long.valueOf(node.fileDescMax()));
                })
                .collect(Collectors.toSet());
    }

    private List<NodesRecord> nodes() {
        List<NodesRecord> allNodes = opensearchClient.execute(() ->
                        catClient.nodes().valueBody(),
                "Unable to retrieve nodes list"
        );
        List<NodesRecord> nodesWithDiskStatistics = allNodes.stream().filter(this::hasDiskStatistics).toList();
        if (allNodes.size() != nodesWithDiskStatistics.size()) {
            final List<NodesRecord> nodesWithMissingDiskStatistics = allNodes.stream()
                    .filter(nr -> !this.hasDiskStatistics(nr)).toList();
            LOG.info("_cat/nodes API has returned " + nodesWithMissingDiskStatistics.size() + " nodes without disk statistics:");
            nodesWithMissingDiskStatistics.forEach(node -> LOG.info(node.toString()));
        }
        return nodesWithDiskStatistics;
    }

    private boolean hasDiskStatistics(NodesRecord nodesRecord) {
        return nodesRecord.diskUsed() != null &&
                nodesRecord.diskTotal() != null &&
                nodesRecord.diskUsedPercent() != null &&
                nodesRecord.fileDescMax() != null;
    }


    @Override
    public ClusterShardAllocation clusterShardAllocation() {
        GetClusterSettingsResponse settings = getClusterSettings();
        int maxShardsPerNode = Integer.MAX_VALUE;
        try {
            maxShardsPerNode = Integer.parseInt(getSetting("cluster.max_shards_per_node", settings));
        } catch (Exception e) {
            LOG.warn("Could not retrieve max_shards_per_node setting from cluster settings. Threshold warnings disabled.", e);
        }

        List<NodeShardAllocation> nodeShardAllocations = opensearchClient.execute(() ->
                        catClient.allocation().valueBody().stream().map(this::toNodeShardAllocation).toList(),
                "Unable to retrieve node shard allocation"
        );
        return new ClusterShardAllocation(maxShardsPerNode, nodeShardAllocations);

    }

    private NodeShardAllocation toNodeShardAllocation(AllocationRecord allocationRecord) {
        int shards;
        try {
            shards = (allocationRecord.shards() != null) ? Integer.parseInt(allocationRecord.shards()) : 0;
        } catch (NumberFormatException e) {
            LOG.error("Could not retrieve shards from allocation record for node {}.", allocationRecord.node(), e);
            shards = 0;
        }
        return new NodeShardAllocation(allocationRecord.node(), shards);
    }

    @Override
    public Set<NodeDiskUsageStats> diskUsageStats() {
        final List<NodesRecord> result = nodes();
        return result.stream()
                .map(node -> {
                    assert node.diskUsedPercent() != null; // checked beforehand in nodes()
                    return NodeDiskUsageStats.create(node.name(), node.nodeRole(), node.ip(), node.name(), node.diskUsed(), node.diskTotal(), Double.valueOf(node.diskUsedPercent()));
                })
                .collect(Collectors.toSet());
    }

    @Override
    public ClusterAllocationDiskSettings clusterAllocationDiskSettings() {
        GetClusterSettingsResponse settings = getClusterSettings();
        return ClusterAllocationDiskSettingsFactory.create(
                Boolean.parseBoolean(getSetting("cluster.routing.allocation.disk.threshold_enabled", settings)),
                getSetting("cluster.routing.allocation.disk.watermark.low", settings),
                getSetting("cluster.routing.allocation.disk.watermark.high", settings),
                getSetting("cluster.routing.allocation.disk.watermark.flood_stage", settings)
        );
    }

    @Override
    public Optional<String> nodeIdToName(String nodeId) {
        return nodeById(nodeId)
                .map(NodeInfo::name);
    }

    @Override
    public Optional<String> nodeIdToHostName(String nodeId) {
        return nodeById(nodeId)
                .map(NodeInfo::host);
    }

    private Optional<NodeInfo> nodeById(String nodeId) {
        if (Strings.isNullOrEmpty(nodeId)) {
            return Optional.empty();
        }
        NodesInfoResponse info = opensearchClient.execute(() -> nodesClient.info(NodesInfoRequest.builder()
                .nodeId(nodeId)
                .build()
        ), "Unable to retrieve node information for node id");
        return Optional.ofNullable(info.nodes().get(nodeId));
    }

    @Override
    public boolean isConnected() {
        return clusterHealth().map(HealthResponse::numberOfDataNodes).map(dataNodes -> dataNodes > 0).orElse(false);
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
        PendingTasksResponse pendingTasks = opensearchClient.execute(
                clusterClient::pendingTasks,
                "Unable to retrieve pending tasks"
        );

        final int pendingTasksSize = pendingTasks.tasks().size();
        List<Long> pendingTasksTimeInQueue = pendingTasks.tasks().stream().map(PendingTask::timeInQueueMillis).toList();
        return PendingTasksStats.create(pendingTasksSize, pendingTasksTimeInQueue);
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
        Request request = Requests.builder()
                .endpoint("/_cluster/stats/nodes/*")
                .method("GET")
                .build();
        return jsonApi.perform(request, "Couldn't read Elasticsearch cluster stats");
    }

    @Override
    public Map<String, org.graylog2.system.stats.elasticsearch.NodeInfo> nodesInfo() {
        NodesInfoResponse info = opensearchClient.execute(() -> nodesClient.info(NodesInfoRequest.builder()
                .build()
        ), "Couldn't read Opensearch nodes data!");

        return info.nodes().entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> createNodeInfo(entry.getValue())
                ));
    }

    private org.graylog2.system.stats.elasticsearch.NodeInfo createNodeInfo(NodeInfo nodeInfo) {
        assert nodeInfo.jvm() != null;
        return org.graylog2.system.stats.elasticsearch.NodeInfo.builder()
                .version(nodeInfo.version())
                .os(nodeInfo.os())
                .roles(nodeInfo.roles().stream().map(Enum::name).toList())
                .jvmMemHeapMaxInBytes(nodeInfo.jvm().mem().heapMaxInBytes())
                .build();
    }

    public <T> Stream<T> toStream(Iterator<T> iterator) {
        return StreamSupport.stream(((Iterable<T>) () -> iterator).spliterator(), false);
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
        final Time timeout = new Time.Builder().time(requestTimeout.toSeconds() + "s").build();
        try {
            HealthResponse health = clusterClient.health(HealthRequest.builder()
                    .timeout(timeout)
                    .build()
            );
            return Optional.of(health);
        } catch (IOException e) {
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

        final Map<String, String> aliasMapping;
        aliasMapping = opensearchClient.execute(() -> catClient.aliases().valueBody()
                        .stream()
                        .filter(alias -> Objects.nonNull(alias.index()))
                        .collect(Collectors.toMap(AliasesRecord::alias, AliasesRecord::index)),
                "Unable to retrieve aliases"
        );

        final Set<String> mappedIndices = indices
                .stream()
                .map(index -> aliasMapping.getOrDefault(index, index))
                .collect(Collectors.toSet());

        final Set<IndicesRecord> indexSummaries = opensearchClient.execute(() ->
                        catClient.indices().valueBody()
                .stream()
                .filter(indexSummary -> mappedIndices.contains(indexSummary.index()))
                                .collect(Collectors.toSet()),
                "Unable to retrieve indices");

        if (indexSummaries.size() < mappedIndices.size()) {
            return Optional.empty();
        }

        return indexSummaries.stream()
                .map(IndicesRecord::health)
                .map(HealthStatus::fromString)
                .min(HealthStatus::compareTo);

    }

    private GetClusterSettingsResponse getClusterSettings() {
        return opensearchClient.execute(() -> clusterClient.getSettings(
                GetClusterSettingsRequest.builder()
                        .includeDefaults(true)
                        .build()
        ), "Unable to retrieve cluster settings");
    }

    private static String getSetting(String setting, GetClusterSettingsResponse settings) {
        JsonData value = settings.transient_().getOrDefault(setting,
                settings.persistent().getOrDefault(setting,
                        settings.defaults().get(setting)));

        return (value == null) ? "" : value.toString();
    }
}

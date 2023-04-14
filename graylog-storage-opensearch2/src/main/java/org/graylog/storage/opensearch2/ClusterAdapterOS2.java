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
import com.google.common.collect.Lists;
import com.google.common.primitives.Ints;
import org.graylog.shaded.opensearch2.org.opensearch.OpenSearchException;
import org.graylog.shaded.opensearch2.org.opensearch.action.admin.cluster.health.ClusterHealthRequest;
import org.graylog.shaded.opensearch2.org.opensearch.action.admin.cluster.health.ClusterHealthResponse;
import org.graylog.shaded.opensearch2.org.opensearch.action.admin.cluster.settings.ClusterGetSettingsRequest;
import org.graylog.shaded.opensearch2.org.opensearch.action.admin.cluster.settings.ClusterGetSettingsResponse;
import org.graylog.shaded.opensearch2.org.opensearch.client.Request;
import org.graylog.shaded.opensearch2.org.opensearch.cluster.health.ClusterHealthStatus;
import org.graylog.shaded.opensearch2.org.opensearch.common.unit.TimeValue;
import org.graylog.storage.opensearch2.cat.CatApi;
import org.graylog.storage.opensearch2.cat.IndexSummaryResponse;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class ClusterAdapterOS2 implements ClusterAdapter {
    private static final Logger LOG = LoggerFactory.getLogger(ClusterAdapterOS2.class);
    private final OpenSearchClient client;
    private final Duration requestTimeout;
    private final CatApi catApi;
    private final PlainJsonApi jsonApi;

    @Inject
    public ClusterAdapterOS2(OpenSearchClient client,
                             @Named("elasticsearch_socket_timeout") Duration requestTimeout,
                             CatApi catApi,
                             PlainJsonApi jsonApi) {
        this.client = client;
        this.requestTimeout = requestTimeout;
        this.catApi = catApi;
        this.jsonApi = jsonApi;
    }

    @Override
    public Optional<HealthStatus> health() {
        return clusterHealth().map(response -> healthStatusFrom(response.getStatus()));
    }

    private HealthStatus healthStatusFrom(ClusterHealthStatus status) {
        switch (status) {
            case RED:
                return HealthStatus.Red;
            case YELLOW:
                return HealthStatus.Yellow;
            case GREEN:
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
        final List<NodeResponse> allNodes = catApi.nodes();
        final List<NodeResponse> nodesWithDiskStatistics = allNodes.stream().filter(NodeResponse::hasDiskStatistics).collect(Collectors.toList());
        if (allNodes.size() != nodesWithDiskStatistics.size()) {
            final List<NodeResponse> nodesWithMissingDiskStatistics = allNodes.stream().filter(nr -> !nr.hasDiskStatistics()).collect(Collectors.toList());
            LOG.info("_cat/nodes API has returned " + nodesWithMissingDiskStatistics.size() + " nodes without disk statistics:");
            nodesWithMissingDiskStatistics.forEach(node -> LOG.info(node.toString()));
        }
        return nodesWithDiskStatistics;
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
        final ClusterGetSettingsRequest request = new ClusterGetSettingsRequest();
        request.includeDefaults(true);

        final ClusterGetSettingsResponse response = client.execute((c, requestOptions) -> c.cluster().getSettings(request, requestOptions));
        return ClusterAllocationDiskSettingsFactory.create(
                Boolean.parseBoolean(response.getSetting("cluster.routing.allocation.disk.threshold_enabled")),
                response.getSetting("cluster.routing.allocation.disk.watermark.low"),
                response.getSetting("cluster.routing.allocation.disk.watermark.high"),
                response.getSetting("cluster.routing.allocation.disk.watermark.flood_stage")
        );
    }

    @Override
    public Optional<String> nodeIdToName(String nodeId) {
        return nodeById(nodeId)
                .map(jsonNode -> jsonNode.get("name").asText());
    }

    @Override
    public Optional<String> nodeIdToHostName(String nodeId) {
        return nodeById(nodeId)
                .map(jsonNode -> jsonNode.path("host"))
                .filter(host -> !host.isMissingNode())
                .map(JsonNode::asText);
    }

    private Optional<JsonNode> nodeById(String nodeId) {
        if (Strings.isNullOrEmpty(nodeId)) {
            return Optional.empty();
        }
        final Request request = new Request("GET", "/_nodes/" + nodeId);
        return Optional.of(jsonApi.perform(request, "Unable to retrieve node information for node id " + nodeId))
                .map(jsonNode -> jsonNode.path("nodes").path(nodeId))
                .filter(node -> !node.isMissingNode());
    }

    @Override
    public boolean isConnected() {
        final ClusterHealthRequest request = new ClusterHealthRequest()
                .timeout(new TimeValue(requestTimeout.getQuantity(), requestTimeout.getUnit()))
                .local(true);
        try {
            final ClusterHealthResponse result = client.execute((c, requestOptions) -> c.cluster().health(request, requestOptions));
            return result.getNumberOfDataNodes() > 0;
        } catch (OpenSearchException e) {
            if (LOG.isDebugEnabled()) {
                LOG.error(e.getMessage(), e);
            }
            return false;
        }
    }

    @Override
    public Optional<String> clusterName() {
        return clusterHealth().map(ClusterHealthResponse::getClusterName);
    }

    @Override
    public Optional<ClusterHealth> clusterHealthStats() {
        return clusterHealth()
                .map(this::clusterHealthFrom);
    }

    private ClusterHealth clusterHealthFrom(ClusterHealthResponse response) {
        return ClusterHealth.create(response.getStatus().toString().toLowerCase(Locale.ENGLISH),
                ClusterHealth.ShardStatus.create(
                        response.getActiveShards(),
                        response.getInitializingShards(),
                        response.getRelocatingShards(),
                        response.getUnassignedShards()
                )
        );
    }

    @Override
    public PendingTasksStats pendingTasks() {
        final Request request = new Request("GET", "/_cluster/pending_tasks");

        final JsonNode response = jsonApi.perform(request, "Couldn't read Elasticsearch pending cluster tasks");

        final JsonNode pendingClusterTasks = response.path("tasks");
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
        final Request request = new Request("GET", "/_nodes");
        final JsonNode nodesJson = jsonApi.perform(request, "Couldn't read Opensearch nodes data!");

        return toStream(nodesJson.at("/nodes").fields())
                .collect(Collectors.toMap(Map.Entry::getKey, o -> createNodeInfo(o.getValue())));
    }

    private NodeInfo createNodeInfo(JsonNode nodesJson) {
        return NodeInfo.builder()
                .version(nodesJson.at("/version").asText())
                .os(nodesJson.at("/os"))
                .roles(toStream(nodesJson.at("/roles").elements()).map(JsonNode::asText).toList())
                .jvmMemHeapMaxInBytes(nodesJson.at("/jvm/mem/heap_max_in_bytes").asLong())
                .build();
    }

    public <T> Stream<T> toStream(Iterator<T> iterator) {
        return StreamSupport.stream(((Iterable<T>) () -> iterator).spliterator(), false);
    }

    @Override
    public ShardStats shardStats() {
        return clusterHealth()
                .map(response -> ShardStats.create(
                        response.getNumberOfNodes(),
                        response.getNumberOfDataNodes(),
                        response.getActiveShards(),
                        response.getRelocatingShards(),
                        response.getActivePrimaryShards(),
                        response.getInitializingShards(),
                        response.getUnassignedShards(),
                        response.isTimedOut()
                ))
                .orElseThrow(() -> new ElasticsearchException("Unable to retrieve shard stats."));
    }

    private Optional<ClusterHealthResponse> clusterHealth() {
        try {
            final ClusterHealthRequest request = new ClusterHealthRequest()
                    .timeout(TimeValue.timeValueSeconds(Ints.saturatedCast(requestTimeout.toSeconds())));
            return Optional.of(client.execute((c, requestOptions) -> c.cluster().health(request, requestOptions)));
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

        final Map<String, String> aliasMapping = catApi.aliases();
        final Set<String> mappedIndices = indices
                .stream()
                .map(index -> aliasMapping.getOrDefault(index, index))
                .collect(Collectors.toSet());

        final Set<IndexSummaryResponse> indexSummaries = catApi.indices()
                .stream()
                .filter(indexSummary -> mappedIndices.contains(indexSummary.index()))
                .collect(Collectors.toSet());

        if (indexSummaries.size() < mappedIndices.size()) {
            return Optional.empty();
        }

        return indexSummaries.stream()
                .map(IndexSummaryResponse::health)
                .map(HealthStatus::fromString)
                .min(HealthStatus::compareTo);
    }
}

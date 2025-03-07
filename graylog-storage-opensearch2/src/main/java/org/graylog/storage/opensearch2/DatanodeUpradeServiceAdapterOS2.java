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
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.inject.Inject;
import org.graylog.plugins.datanode.DatanodeUpgradeServiceAdapter;
import org.graylog.plugins.datanode.dto.ClusterState;
import org.graylog.plugins.datanode.dto.FlushResponse;
import org.graylog.plugins.datanode.dto.ManagerNode;
import org.graylog.plugins.datanode.dto.Node;
import org.graylog.plugins.datanode.dto.ShardReplication;
import org.graylog.shaded.opensearch2.org.opensearch.action.admin.cluster.health.ClusterHealthRequest;
import org.graylog.shaded.opensearch2.org.opensearch.action.admin.cluster.health.ClusterHealthResponse;
import org.graylog.shaded.opensearch2.org.opensearch.action.admin.cluster.settings.ClusterGetSettingsRequest;
import org.graylog.shaded.opensearch2.org.opensearch.action.admin.cluster.settings.ClusterGetSettingsResponse;
import org.graylog.shaded.opensearch2.org.opensearch.action.admin.cluster.settings.ClusterUpdateSettingsRequest;
import org.graylog.shaded.opensearch2.org.opensearch.action.admin.cluster.settings.ClusterUpdateSettingsResponse;
import org.graylog.shaded.opensearch2.org.opensearch.client.Request;
import org.graylog.shaded.opensearch2.org.opensearch.client.Response;
import org.graylog.shaded.opensearch2.org.opensearch.common.settings.Settings;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class DatanodeUpradeServiceAdapterOS2 implements DatanodeUpgradeServiceAdapter {

    public static final String REPLICATION_PRIMARIES = "primaries";
    public static final String REPLICATION_ALL = "all";
    private final OpenSearchClient client;
    private final ObjectMapper objectMapper;

    @Inject
    public DatanodeUpradeServiceAdapterOS2(OpenSearchClient client, ObjectMapper objectMapper) {
        this.client = client;
        this.objectMapper = objectMapper;
    }

    @Override
    public ClusterState getClusterState() {
        final ClusterHealthResponse response = client.execute((restHighLevelClient, requestOptions) -> restHighLevelClient.cluster().health(new ClusterHealthRequest(), requestOptions));
        final String shardReplication = queryShardReplication();
        final ManagerNode managerNode = managerNode();
        return new ClusterState(
                response.getStatus().name(),
                response.getClusterName(),
                response.getNumberOfNodes(),
                response.getActiveShards(),
                response.getRelocatingShards(),
                response.getInitializingShards(),
                response.getUnassignedShards(),
                response.getActivePrimaryShards(),
                response.getDelayedUnassignedShards(),
                Optional.ofNullable(shardReplication).map(v -> v.toUpperCase(Locale.ROOT)).map(ShardReplication::valueOf).orElse(ShardReplication.ALL),
                managerNode,
                nodesResponse());
    }

    @Override
    public void disableShardReplication() {
        configureShardReplication(REPLICATION_PRIMARIES);
    }

    @Override
    public void enableShardReplication() {
        configureShardReplication(REPLICATION_ALL);
    }

    private String queryShardReplication() {
        return client.execute((restHighLevelClient, requestOptions) ->
        {
            final ClusterGetSettingsResponse settings = restHighLevelClient.cluster().getSettings(new ClusterGetSettingsRequest().includeDefaults(true), requestOptions);
            return settings.getPersistentSettings().get("cluster.routing.allocation.enable");
        });
    }

    private void configureShardReplication(String primaries) {
        final ClusterUpdateSettingsRequest request = new ClusterUpdateSettingsRequest().persistentSettings(Settings.builder()
                .put("cluster.routing.allocation.enable", primaries).build());
        final ClusterUpdateSettingsResponse result = client.execute((restHighLevelClient, requestOptions) -> restHighLevelClient.cluster().putSettings(request, requestOptions));
        final String value = result.getPersistentSettings().get("cluster.routing.allocation.enable");
        if (!value.equals(primaries)) {
            throw new IllegalStateException("Failed to disable shard replication. Current cluster.routing.allocation.enable: " + value);
        }
    }

    @Override
    public FlushResponse flush() {
        final Response response = client.execute((restHighLevelClient, requestOptions) -> restHighLevelClient.getLowLevelClient().performRequest(new Request("POST", "_flush")));
        try {
            final JsonNode flushResponse = objectMapper.readValue(response.getEntity().getContent(), JsonNode.class);
            final JsonNode shards = flushResponse.path("_shards");
            return new FlushResponse(
                    shards.path("total").asInt(),
                    shards.path("successful").asInt(),
                    shards.path("failed").asInt()
            );
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private List<Node> nodesResponse() {
        final Response nodes = client.execute((restHighLevelClient, requestOptions) -> restHighLevelClient.getLowLevelClient().performRequest(new Request("GET", "_nodes")));
        try {
            final JsonNode parsed = objectMapper.readValue(nodes.getEntity().getContent(), JsonNode.class);
            return parseNodes(parsed.path("nodes"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private ManagerNode managerNode() {
        final Response nodes = client.execute((restHighLevelClient, requestOptions) -> restHighLevelClient.getLowLevelClient().performRequest(new Request("GET", "_cluster/state")));
        try {
            final JsonNode parsed = objectMapper.readValue(nodes.getEntity().getContent(), JsonNode.class);
            final String managerNodeID = parsed.path("cluster_manager_node").asText();
            final String managerNodeName = parsed.path("nodes").path(managerNodeID).path("name").asText();
            return new ManagerNode(managerNodeID, managerNodeName);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private List<org.graylog.plugins.datanode.dto.Node> parseNodes(JsonNode nodes) {
        return StreamSupport.stream(nodes.spliterator(), false)
                .map(node -> new Node(
                        node.path("host").asText(),
                        node.path("ip").asText(),
                        node.path("name").asText(),
                        node.path("version").asText(),
                        parseRoles(node.path("roles"))))
                .sorted(Comparator.comparing(Node::name))
                .collect(Collectors.toList());
    }

    private List<String> parseRoles(JsonNode roles) {
        return StreamSupport.stream(roles.spliterator(), false).map(JsonNode::asText)
                .sorted(Comparator.naturalOrder())
                .collect(Collectors.toList());
    }
}

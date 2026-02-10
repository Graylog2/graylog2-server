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
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.inject.Inject;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;
import org.graylog.plugins.datanode.DatanodeUpgradeServiceAdapter;
import org.graylog.plugins.datanode.dto.ClusterState;
import org.graylog.plugins.datanode.dto.FlushResponse;
import org.graylog.plugins.datanode.dto.ManagerNode;
import org.graylog.plugins.datanode.dto.Node;
import org.graylog.plugins.datanode.dto.ShardReplication;
import org.opensearch.client.json.JsonData;
import org.opensearch.client.opensearch._types.HealthStatus;
import org.opensearch.client.opensearch.cluster.GetClusterSettingsResponse;
import org.opensearch.client.opensearch.cluster.HealthResponse;
import org.opensearch.client.opensearch.cluster.PutClusterSettingsResponse;
import org.opensearch.client.opensearch.cluster.StateResponse;
import org.opensearch.client.opensearch.generic.Request;
import org.opensearch.client.opensearch.generic.Requests;
import org.opensearch.client.opensearch.generic.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class DatanodeUpgradeServiceAdapterOS implements DatanodeUpgradeServiceAdapter {

    private static final Logger LOG = LoggerFactory.getLogger(DatanodeUpgradeServiceAdapterOS.class);

    public static final String REPLICATION_PRIMARIES = "primaries";
    public static final String REPLICATION_ALL = "all";
    private final OfficialOpensearchClient officialOpensearchClient;
    private final ObjectMapper objectMapper;

    @Inject
    public DatanodeUpgradeServiceAdapterOS(OfficialOpensearchClient officialOpensearchClient, ObjectMapper objectMapper) {
        this.officialOpensearchClient = officialOpensearchClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public ClusterState getClusterState() {
        final HealthResponse response = getClusterHealthResponse();
        final String shardReplication = queryShardReplication();
        final ManagerNode managerNode = managerNode();
        return new ClusterState(
                org.graylog2.indexer.indices.HealthStatus.fromString(response.status().name()),
                response.clusterName(),
                response.numberOfNodes(),
                response.activeShards(),
                response.relocatingShards(),
                response.initializingShards(),
                response.unassignedShards(),
                response.activePrimaryShards(),
                response.delayedUnassignedShards(),
                Optional.ofNullable(shardReplication).map(v -> v.toUpperCase(Locale.ROOT)).map(ShardReplication::valueOf).orElse(ShardReplication.ALL),
                managerNode,
                nodesResponse());
    }

    private HealthResponse getClusterHealthResponse() {
        return officialOpensearchClient.sync(c -> c.cluster().health(), "Failed to obtain cluster health!");
    }

    @Override
    public FlushResponse disableShardReplication() {
        LOG.info("Disabling shard replication for opensearch cluster");
        final HealthStatus clusterHealthStatus = getClusterHealthResponse().status();
        if (clusterHealthStatus == HealthStatus.Green) {
            return configureShardReplication(REPLICATION_PRIMARIES);
        } else {
            throw new IllegalStateException("Can't disable shard replication, cluster is not in healthy state. Current state: " + clusterHealthStatus);
        }
    }

    @Override
    public FlushResponse enableShardReplication() {
        LOG.info("Enabling shard replication for opensearch cluster");
        return configureShardReplication(REPLICATION_ALL);
    }

    private String queryShardReplication() {
        final GetClusterSettingsResponse response = officialOpensearchClient.sync(c -> c.cluster().getSettings(settings -> settings.includeDefaults(true).flatSettings(true)), "Failed to obtain shard replication settings!");
        return getSetting("cluster.routing.allocation.enable", response);
    }

    private static String getSetting(String setting, GetClusterSettingsResponse settings) {
        JsonData value = settings.transient_().getOrDefault(setting,
                settings.persistent().getOrDefault(setting,
                        settings.defaults().get(setting)));
        if (value == null) {
            throw new RuntimeException("Failed to read setting " + setting + "from cluster state");
        }
        return value.to(String.class);
    }

    private FlushResponse configureShardReplication(String shardReplication) {
        final PutClusterSettingsResponse response = officialOpensearchClient.sync(c -> c.cluster().putSettings(setting -> setting.flatSettings(true).persistent("cluster.routing.allocation.enable", JsonData.of(shardReplication))), "Failed to configure shard replication!");
        final String value = response.persistent().get("cluster.routing.allocation.enable").to(String.class);
        if (!value.equals(shardReplication)) {
            throw new IllegalStateException("Failed to configure shard replication. Expected cluster.routing.allocation.enable=" + shardReplication + " but was: " + value);
        }
        return flush();
    }

    private FlushResponse flush() {
        LOG.info("Flushing opensearch nodes, storing all in-memory operations to segments on disk");
        final org.opensearch.client.opensearch.indices.FlushResponse response = officialOpensearchClient.sync(c -> c.indices().flush(f -> f.force(true).waitIfOngoing(true)), "Failed to flush opensearch nodes!");
        return new FlushResponse(response.shards().total(), response.shards().successful(), response.shards().failed());
    }

    private List<Node> nodesResponse() {
        //https://github.com/opensearch-project/opensearch-java/issues/894
        //final NodesInfoResponse nodes = officialOpensearchClient.sync(c -> c.nodes().info(), "Failed to obtain opensearch nodes");
        final Request req = Requests.builder()
                .method("GET")
                .endpoint("/_nodes")
                .build();
        return officialOpensearchClient.sync(c -> {
            try (final Response response = c.generic().execute(req)) {
                return parseNodesResponse(response);
            }
        }, "Failed to obtain node infos");
    }

    private List<Node> parseNodesResponse(Response response) {
        return response.getBody().map(body -> {
            try (final InputStream is = body.body()) {
                final JsonNode parsed = objectMapper.readValue(is, JsonNode.class);
                return parseNodes(parsed.path("nodes"));
            } catch (IOException e) {
                throw new RuntimeException("Failed to parse node response from /_nodes", e);
            }
        }).orElseThrow(() -> new IllegalStateException("Failed to obtain node response"));
    }

    private ManagerNode managerNode() {
        // https://github.com/opensearch-project/opensearch-java/issues/1791
        final StateResponse response = officialOpensearchClient.sync(c -> c.cluster().state(), "Failed to obtain manager node!");
        final JsonValue json = response.valueBody().toJson();
        final String managerNodeID = parseString(json.asJsonObject().get("cluster_manager_node"));
        final String managerNodeName = parseString(json.asJsonObject().get("nodes").asJsonObject().get(managerNodeID).asJsonObject().get("name"));
        return new ManagerNode(managerNodeID, managerNodeName);
    }

    private static String parseString(JsonValue clusterManagerNode) {
        if (clusterManagerNode instanceof JsonString jsonString) {
            return jsonString.getString();
        } else {
            throw new IllegalStateException("Failed to obtain String value from json object!");
        }
    }

    private List<Node> parseNodes(JsonNode nodes) {
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

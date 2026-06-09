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
package org.graylog2.cluster.nodes;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.zafarkhaja.semver.Version;
import jakarta.annotation.Nullable;
import org.graylog2.datanode.DataNodeInformation;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public record OpensearchVersionsOverview(@JsonProperty("nodes") List<NodeVersionStatus> nodes) {

    public record NodeVersionStatus(
            @JsonProperty("node_id") String nodeId,
            @JsonProperty("current_version") String currentVersion,
            @JsonProperty("available_version") @Nullable String availableVersion,
            @JsonProperty("upgradeable") boolean upgradeable,
            @JsonProperty("datanode") @Nullable DatanodeDetails datanode
    ) {}

    public record DatanodeDetails(
            @JsonProperty("node_name") @Nullable String nodeName,
            @JsonProperty("datanode_status") DataNodeStatus datanodeStatus,
            @JsonProperty("datanode_version") @Nullable String datanodeVersion,
            @JsonProperty("hostname") @Nullable String hostname,
            @JsonProperty("ip") @Nullable String ip,
            @JsonProperty("roles") @Nullable List<String> roles,
            @JsonProperty("manager_node") boolean managerNode
    ) {
        static DatanodeDetails of(DataNodeInformation info) {
            return new DatanodeDetails(info.nodeName(), info.dataNodeStatus(), info.datanodeVersion(),
                    info.hostname(), info.ip(), info.roles(), info.managerNode());
        }
    }

    @JsonProperty("upgrade_available")
    public boolean upgradeAvailable() {
        return nodes.stream().anyMatch(NodeVersionStatus::upgradeable);
    }

    @JsonProperty("up_to_date_count")
    public long upToDateCount() {
        return nodes.stream().filter(n -> !n.upgradeable()).count();
    }

    @JsonProperty("upgradeable_count")
    public long upgradeableCount() {
        return nodes.stream().filter(NodeVersionStatus::upgradeable).count();
    }

    @JsonProperty("lowest_current_version")
    public Optional<String> lowestCurrentVersion() {
        return nodes.stream()
                .map(NodeVersionStatus::currentVersion)
                .min(Comparator.comparing(Version::parse));
    }

    @JsonProperty("highest_available_version")
    public Optional<String> highestAvailableVersion() {
        return nodes.stream()
                .map(NodeVersionStatus::availableVersion)
                .filter(v -> v != null)
                .max(Comparator.comparing(Version::parse));
    }

    public static OpensearchVersionsOverview of(List<DataNodeMetadata> nodes, Map<String, DataNodeInformation> datanodes) {
        final List<NodeVersionStatus> statuses = nodes.stream()
                .map(n -> new NodeVersionStatus(
                        n.nodeId(),
                        n.currentOpensearchVersion(),
                        n.latestAvailableOpensearchVersion(),
                        n.latestAvailableOpensearchVersion() != null,
                        Optional.ofNullable(datanodes.get(n.nodeId())).map(DatanodeDetails::of).orElse(null)
                ))
                .sorted(Comparator.comparing(NodeVersionStatus::currentVersion, Comparator.comparing(Version::parse)))
                .toList();
        return new OpensearchVersionsOverview(statuses);
    }
}

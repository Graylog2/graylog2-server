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
package org.graylog2.datanode;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.graylog.plugins.datanode.dto.ClusterState;
import org.graylog2.plugin.Version;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public record DatanodeUpgradeStatus(Version serverVersion, ClusterState clusterState,
                                    boolean clusterReadyForUpgrade, List<DataNodeInformation> upToDateNodes,
                                    List<DataNodeInformation> outdatedNodes) {

    @JsonProperty("upgrade_available")
    public boolean upgradeAvailable() {
        return !outdatedNodes.isEmpty();
    }

    @JsonProperty("upgradeable_nodes")
    public @Nonnull Set<String> upgradeableNodes() {
        return outdatedNodes.stream().map(DataNodeInformation::nodeName).collect(Collectors.toSet());
    }
}

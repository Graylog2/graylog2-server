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
package org.graylog.datanode.configuration;

import com.github.zafarkhaja.semver.Version;
import jakarta.inject.Inject;
import org.graylog2.cluster.nodes.DataNodeMetadataService;
import org.graylog2.plugin.system.NodeId;

public class OpensearchUpgradeAction {

    private final DataNodeMetadataService metadataService;
    private final NodeId nodeId;

    @Inject
    public OpensearchUpgradeAction(DataNodeMetadataService metadataService, NodeId nodeId) {
        this.metadataService = metadataService;
        this.nodeId = nodeId;
    }

    public boolean upgradeToLatestAvaiable() {
        return metadataService.findByNodeId(nodeId.getNodeId())
                .filter(m -> m.latestAvailableOpensearchVersion() != null)
                .filter(m -> Version.parse(m.latestAvailableOpensearchVersion()).isHigherThan(Version.parse(m.currentOpensearchVersion())))
                .map(
                        dataNodeMetadata -> {
                            metadataService.setOpensearchVersions(nodeId.getNodeId(), dataNodeMetadata.latestAvailableOpensearchVersion(), null);
                            return true;
                        })
                .orElse(false);
    }
}

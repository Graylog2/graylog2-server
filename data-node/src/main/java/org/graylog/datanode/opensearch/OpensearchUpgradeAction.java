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
package org.graylog.datanode.opensearch;

import com.github.zafarkhaja.semver.Version;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.graylog.datanode.OpensearchDistribution;
import org.graylog.datanode.configuration.DatanodeConfiguration;
import org.graylog2.cluster.nodes.DataNodeMetadataService;
import org.graylog2.plugin.system.NodeId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;

@Singleton
public class OpensearchUpgradeAction {

    private static final Logger LOG = LoggerFactory.getLogger(OpensearchUpgradeAction.class);

    private final DatanodeConfiguration configuration;
    private final DataNodeMetadataService metadataService;
    private final NodeId nodeId;

    @Inject
    public OpensearchUpgradeAction(DatanodeConfiguration configuration, DataNodeMetadataService metadataService, NodeId nodeId) {
        this.configuration = configuration;
        this.metadataService = metadataService;
        this.nodeId = nodeId;
    }

    void enableLatestVersion() {
        final OpensearchDistribution opensearchDistribution = configuration.opensearchDistribution();
        final String opensearchVersion = opensearchDistribution.version();

        final Version currentVersion = Version.parse(opensearchVersion);
        final String latestAvailableVersion = opensearchDistribution.otherCandidates().stream()
                .filter(candidate -> Version.parse(candidate.version()).isHigherThan(currentVersion))
                .max(Comparator.comparing(d -> Version.parse(d.version())))
                .map(OpensearchDistribution::version)
                .orElse(null);
        metadataService.setOpensearchVersions(nodeId.getNodeId(), latestAvailableVersion, null);
        LOG.info("Persisting confirmed opensearch version in data node metadata {}", opensearchVersion);
        if (latestAvailableVersion != null) {
            LOG.warn("You are running outdated Opensearch version. Please go to the data node upgrade page in administration to update to {}", latestAvailableVersion);
        }
    }
}


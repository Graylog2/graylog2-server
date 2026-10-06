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
package org.graylog.datanode.opensearch.statemachine.tracer;

import com.github.zafarkhaja.semver.Version;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import org.graylog.datanode.OpensearchDistribution;
import org.graylog.datanode.configuration.DatanodeConfiguration;
import org.graylog.datanode.filesystem.index.DataDirVerificationMarker;
import org.graylog.datanode.opensearch.statemachine.OpensearchEvent;
import org.graylog.datanode.opensearch.statemachine.OpensearchState;
import org.graylog.datanode.process.statemachine.tracer.StateMachineTracer;
import org.graylog2.cluster.nodes.DataNodeMetadataService;
import org.graylog2.plugin.system.NodeId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;

public class OpensearchVersionTracer implements StateMachineTracer<OpensearchState, OpensearchEvent> {

    private static final Logger LOG = LoggerFactory.getLogger(OpensearchVersionTracer.class);

    private final DatanodeConfiguration configuration;
    private final DataNodeMetadataService metadataService;
    private final DataDirVerificationMarker verificationMarker;
    private final NodeId nodeId;

    @Inject
    public OpensearchVersionTracer(DatanodeConfiguration configuration, DataNodeMetadataService metadataService,
                                   DataDirVerificationMarker verificationMarker, NodeId nodeId) {
        this.configuration = configuration;
        this.metadataService = metadataService;
        this.verificationMarker = verificationMarker;
        this.nodeId = nodeId;
    }

    @Override
    public void trigger(OpensearchEvent trigger) {
    }

    @Override
    public void transition(OpensearchEvent trigger, OpensearchState source, OpensearchState destination) {
        if (source != destination && destination == OpensearchState.AVAILABLE) {
            final OpensearchDistribution opensearchDistribution = configuration.opensearchDistribution();
            final String opensearchVersion = opensearchDistribution.version();

            final Version currentVersion = Version.parse(opensearchVersion);
            final String latestAvailableVersion = latestAvailableVersion(opensearchDistribution, currentVersion);

            if (isCurrentNewerThanPersisted(currentVersion)) {
                metadataService.setOpensearchVersions(nodeId.getNodeId(), opensearchVersion, latestAvailableVersion);
                LOG.info("Persisting confirmed opensearch version in data node metadata {}", opensearchVersion);
            } else {
                // The running version hasn't moved, so the recorded one stays as it is. The latest available version
                // still has to be refreshed: newly shipped distributions only become visible on the upgrade page
                // through this field, and without this the node would never be offered another upgrade.
                metadataService.setLatestAvailableVersion(nodeId.getNodeId(), latestAvailableVersion);
            }

            if (latestAvailableVersion != null) {
                LOG.warn("You are running outdated Opensearch version. Please go to the data node upgrade page in administration to update to {}", latestAvailableVersion);
            }

            // Opensearch is up, so this version demonstrably opened the data directory. Recorded here rather than in
            // the preflight check because an in-process upgrade swaps the distribution without re-running preflight.
            verificationMarker.record(configuration.datanodeDirectories().getDataTargetDir(), opensearchVersion);
        }
    }

    @Nullable
    private String latestAvailableVersion(OpensearchDistribution distribution, Version currentVersion) {
        return distribution.otherCandidates().stream()
                .filter(candidate -> Version.parse(candidate.version()).isHigherThan(currentVersion))
                .max(Comparator.comparing(d -> Version.parse(d.version())))
                .map(OpensearchDistribution::version)
                .orElse(null);
    }

    private boolean isCurrentNewerThanPersisted(Version currentVersion) {
        return metadataService.findByNodeId(nodeId.getNodeId())
                .map(m -> currentVersion.isHigherThan(Version.parse(m.currentOpensearchVersion())))
                .orElse(true);
    }
}

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
import org.graylog.datanode.OpensearchDistribution;
import org.graylog2.cluster.nodes.DataNodeMetadata;
import org.graylog2.cluster.nodes.DataNodeMetadataService;
import org.graylog2.plugin.system.NodeId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class OpensearchVersionSelectorImpl implements OpensearchVersionSelector {

    private static final Logger LOG = LoggerFactory.getLogger(OpensearchVersionSelectorImpl.class);

    private final DataNodeMetadataService dataNodeMetadataService;
    private final NodeId nodeId;

    @Inject
    public OpensearchVersionSelectorImpl(final DataNodeMetadataService dataNodeMetadataService, NodeId nodeId) {
        this.dataNodeMetadataService = dataNodeMetadataService;
        this.nodeId = nodeId;
    }

    public OpensearchDistribution select(final List<OpensearchDistribution> candidates) {
        final Optional<DataNodeMetadata> metadata = dataNodeMetadataService.findByNodeId(nodeId.getNodeId());
        return metadata.map(DataNodeMetadata::currentOpensearchVersion)
                .flatMap(v -> requestedVersion(v, candidates))
                .orElseGet(() ->fallbackVersion(candidates));
    }

    private Optional<OpensearchDistribution> requestedVersion(String version, List<OpensearchDistribution> candidates) {
        final Optional<OpensearchDistribution> foundDistribution = candidates.stream()
                .filter(d -> version.equals(d.version()))
                .findFirst();
        if (foundDistribution.isEmpty()) {
            LOG.warn("Requested opensearch version {} is not available in this distribution!", version);
        }
        return foundDistribution;
    }

    private static OpensearchDistribution fallbackVersion(List<OpensearchDistribution> candidates) {
        return candidates.stream()
                .min(Comparator.comparing(OpensearchDistribution::version, OpensearchVersionSelectorImpl::compareVersions))
                .orElseThrow(() -> new IllegalArgumentException("No suitable OpenSearch distribution found"));
    }

    private static int compareVersions(final String v1, final String v2) {
        return Version.parse(v1).compareTo(Version.parse(v2));
    }
}

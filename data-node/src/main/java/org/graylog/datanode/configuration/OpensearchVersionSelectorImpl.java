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
import org.graylog.datanode.Configuration;
import org.graylog.datanode.OpensearchDistribution;
import org.graylog.datanode.filesystem.index.DataDirVerificationMarker;
import org.graylog.datanode.filesystem.index.IndicesDirectoryParseResult;
import org.graylog.datanode.filesystem.index.IndicesDirectoryParser;
import org.graylog.datanode.filesystem.index.RequiredOpensearchDistribution;
import org.graylog2.cluster.nodes.DataNodeMetadata;
import org.graylog2.cluster.nodes.DataNodeMetadataService;
import org.graylog2.plugin.system.NodeId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import static org.graylog2.shared.utilities.StringUtils.f;

/**
 * Picks the opensearch distribution this datanode will run.
 * <p>
 * Three signals feed the decision, in this order:
 * <ol>
 *     <li>the version recorded for this node, which is authoritative — a generation change is an explicit
 *     administrator action, never a side effect of upgrading the datanode package;</li>
 *     <li>whether the data directory can be opened by the current generation at all, which is a hard bound;</li>
 *     <li>whether the directory holds any indices, which decides whether we may be optimistic about a node that
 *     has no recorded version yet.</li>
 * </ol>
 * The data directory is only scanned when the answer actually depends on it. A recorded version whose major already
 * opened this directory needs no scan, which keeps plain restarts and configuration rebuilds free.
 */
public class OpensearchVersionSelectorImpl implements OpensearchVersionSelector {

    private static final Logger LOG = LoggerFactory.getLogger(OpensearchVersionSelectorImpl.class);

    private final DataNodeMetadataService dataNodeMetadataService;
    private final IndicesDirectoryParser indicesDirectoryParser;
    private final DataDirVerificationMarker verificationMarker;
    private final Configuration localConfiguration;
    private final NodeId nodeId;

    @Inject
    public OpensearchVersionSelectorImpl(final DataNodeMetadataService dataNodeMetadataService,
                                         final IndicesDirectoryParser indicesDirectoryParser,
                                         final DataDirVerificationMarker verificationMarker,
                                         final Configuration localConfiguration,
                                         NodeId nodeId) {
        this.dataNodeMetadataService = dataNodeMetadataService;
        this.indicesDirectoryParser = indicesDirectoryParser;
        this.verificationMarker = verificationMarker;
        this.localConfiguration = localConfiguration;
        this.nodeId = nodeId;
    }

    public OpensearchDistribution select(final List<OpensearchDistribution> candidates) {
        final Path dataDir = dataDir();
        final Optional<String> recordedVersion = dataNodeMetadataService.findByNodeId(nodeId.getNodeId())
                .map(DataNodeMetadata::currentOpensearchVersion);

        if (localConfiguration.isAutoUpdateOpensearch()) {
            LOG.warn("auto_update_opensearch is enabled. This is a testing option: the node takes the newest "
                    + "distribution its data allows instead of the recorded version.");
            return highestOf(withinBound(candidates, dataDir));
        }

        if (recordedVersion.isPresent()) {
            final String version = recordedVersion.get();
            final Optional<OpensearchDistribution> recorded = findVersion(version, candidates);
            if (recorded.isEmpty()) {
                LOG.warn("Requested opensearch version {} is not available in this distribution!", version);
            } else if (verificationMarker.isVerifiedFor(dataDir, version)) {
                // This major already opened this directory and nothing older can have appeared since, so there is
                // nothing to re-verify.
                return recorded.get();
            } else {
                // Either the recorded version moved up a generation, or opensearch never confirmed this one. Both
                // need the bound checked before we hand the directory to it.
                final List<OpensearchDistribution> bound = withinBound(candidates, dataDir);
                if (bound.stream().anyMatch(d -> d.version().equals(version))) {
                    return recorded.get();
                }
                throw new IllegalArgumentException(f("Recorded opensearch version %s cannot open the data directory "
                        + "%s. Its indices were written by an older opensearch generation and have to be reindexed or "
                        + "removed before this version can be used", version, dataDir));
            }
        }

        return fallbackVersion(candidates, dataDir);
    }

    /**
     * No version recorded for this node. An empty directory has nothing to preserve, so we take the newest
     * distribution available. A directory that already holds indices is left on the oldest one that can read it —
     * it may have been adopted from an existing cluster during an in-place migration, and moving it up a generation
     * rewrites index formats irreversibly. The upgrade page offers the newer version afterwards.
     */
    private OpensearchDistribution fallbackVersion(List<OpensearchDistribution> candidates, Path dataDir) {
        final Optional<IndicesDirectoryParseResult> parseResult = parse(dataDir);
        final List<OpensearchDistribution> bound = withinBound(candidates, parseResult);

        final boolean holdsIndices = parseResult
                .map(result -> !result.info().nodes().isEmpty())
                // Unreadable or unparseable: assume there is data and stay conservative. The preflight check reports
                // the actual problem with a usable message.
                .orElse(true);

        return holdsIndices ? lowestOf(bound) : highestOf(bound);
    }

    private List<OpensearchDistribution> withinBound(List<OpensearchDistribution> candidates, Path dataDir) {
        return withinBound(candidates, parse(dataDir));
    }

    /**
     * Restricts the candidates to those that can actually open the directory. Only a directory that needs the
     * compatibility reader constrains anything; everything else stays eligible.
     */
    private List<OpensearchDistribution> withinBound(List<OpensearchDistribution> candidates,
                                                     Optional<IndicesDirectoryParseResult> parseResult) {
        final boolean needsCompat = parseResult
                .map(result -> result.requiredDistribution() == RequiredOpensearchDistribution.COMPAT)
                .orElse(false);
        if (!needsCompat) {
            return candidates;
        }
        final List<OpensearchDistribution> bound = candidates.stream()
                .filter(d -> RequiredOpensearchDistribution.COMPAT.matches(d.version()))
                .toList();
        LOG.info("The opensearch data directory requires the {} compatibility distribution",
                RequiredOpensearchDistribution.COMPAT.versionSelector);
        return bound.isEmpty() ? candidates : bound;
    }

    private Optional<IndicesDirectoryParseResult> parse(Path dataDir) {
        try {
            return Optional.of(indicesDirectoryParser.parse(dataDir));
        } catch (Exception e) {
            // Not our error to report: the preflight check validates the directory and produces a message naming the
            // actual problem. Here it only means we have no opinion.
            LOG.debug("Could not inspect the opensearch data directory {} while selecting a distribution", dataDir, e);
            return Optional.empty();
        }
    }

    private Optional<OpensearchDistribution> findVersion(String version, List<OpensearchDistribution> candidates) {
        return candidates.stream().filter(d -> version.equals(d.version())).findFirst();
    }

    private static OpensearchDistribution lowestOf(List<OpensearchDistribution> candidates) {
        return candidates.stream()
                .min(Comparator.comparing(OpensearchDistribution::version, OpensearchVersionSelectorImpl::compareVersions))
                .orElseThrow(() -> new IllegalArgumentException("No suitable OpenSearch distribution found"));
    }

    private static OpensearchDistribution highestOf(List<OpensearchDistribution> candidates) {
        return candidates.stream()
                .max(Comparator.comparing(OpensearchDistribution::version, OpensearchVersionSelectorImpl::compareVersions))
                .orElseThrow(() -> new IllegalArgumentException("No suitable OpenSearch distribution found"));
    }

    private Path dataDir() {
        return DatanodeDirectories.fromConfiguration(localConfiguration, nodeId).getDataTargetDir();
    }

    private static int compareVersions(final String v1, final String v2) {
        return Version.parse(v1).compareTo(Version.parse(v2));
    }
}

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
package org.graylog.datanode.filesystem.index;

import com.github.zafarkhaja.semver.Version;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.graylog.datanode.DirectoryReadableValidator;
import org.graylog.datanode.configuration.DatanodeConfiguration;
import org.graylog.datanode.filesystem.index.dto.IndexInformation;
import org.graylog.datanode.filesystem.index.dto.IndexerDirectoryInformation;
import org.graylog.datanode.filesystem.index.dto.NodeInformation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.graylog2.shared.utilities.StringUtils.f;

/**
 * Inspects the configured opensearch data directory and reports whether the indices it contains can be used by the
 * opensearch version this datanode ships with.
 * <p>
 * Both the {@code OpensearchDataDirCompatibilityCheck} preflight check, which refuses to start the datanode on
 * incompatible data, and the {@code IndicesDirectoryController} REST resource, which reports the same information to
 * the migration UI and to the migration state machine, are based on this service. Keeping the inspection in one place
 * makes sure the migration isn't allowed to proceed on data that the datanode would then refuse to start on.
 */
@Singleton
public class OpensearchDataDirCompatibilityService {

    private static final Logger LOG = LoggerFactory.getLogger(OpensearchDataDirCompatibilityService.class);

    private final DatanodeConfiguration datanodeConfiguration;
    private final IndicesDirectoryParser indicesDirectoryParser;
    private final DirectoryReadableValidator directoryReadableValidator = new DirectoryReadableValidator();

    @Inject
    public OpensearchDataDirCompatibilityService(DatanodeConfiguration datanodeConfiguration,
                                                 IndicesDirectoryParser indicesDirectoryParser) {
        this.datanodeConfiguration = datanodeConfiguration;
        this.indicesDirectoryParser = indicesDirectoryParser;
    }

    /**
     * Parses the opensearch data directory and collects all compatibility problems it can find. Never throws, a
     * directory that can't be read or parsed at all is reported as a single error instead.
     */
    public OpensearchDataDirCompatibility check() {
        final Path dataDir = datanodeConfiguration.datanodeDirectories().getDataTargetDir();
        final String opensearchVersion = datanodeConfiguration.opensearchDistribution().version();
        try {
            directoryReadableValidator.validate(dataDir.toUri().toString(), dataDir);
            final IndexerDirectoryInformation info = indicesDirectoryParser.parse(dataDir);
            return new OpensearchDataDirCompatibility(dataDir, opensearchVersion, info,
                    collectErrors(Version.parse(opensearchVersion), info),
                    collectWarnings(dataDir, info));
        } catch (Exception e) {
            // The stack trace is only of interest here, callers just report the message to the user.
            LOG.warn("Failed to inspect opensearch data directory {}", dataDir, e);
            return OpensearchDataDirCompatibility.failed(dataDir, opensearchVersion,
                    Optional.ofNullable(e.getMessage()).orElseGet(e::toString));
        }
    }

    private List<String> collectErrors(Version currentVersion, IndexerDirectoryInformation info) {
        final List<String> errors = new ArrayList<>();
        for (NodeInformation node : info.nodes()) {
            if (node.nodeVersion() != null) {
                final Version nodeVersion = Version.parse(node.nodeVersion());
                if (!OpensearchUtils.isCompatible(currentVersion, nodeVersion)) {
                    errors.add(f("Current version %s of Opensearch is not compatible with index version %s",
                            currentVersion, nodeVersion));
                }
            }
            for (IndexInformation index : node.indices()) {
                final String indexVersionCreated = index.indexVersionCreated();
                if (indexVersionCreated == null) {
                    errors.add(f("Unknown index version for index %s", index.indexName()));
                    continue;
                }
                final Version indexVersion = Version.parse(indexVersionCreated);
                if (!OpensearchUtils.isCompatible(currentVersion, indexVersion)) {
                    errors.add(f("Current version %s is not compatible with index version %s of index %s",
                            currentVersion, indexVersion, index.indexName()));
                }
            }
        }
        return errors;
    }

    private List<String> collectWarnings(Path dataDir, IndexerDirectoryInformation info) {
        if (info.nodes().isEmpty() || info.nodes().stream().allMatch(NodeInformation::isEmpty)) {
            return List.of(f("Your configured opensearch_data_location directory %s doesn't contain any indices! " +
                    "Do you want to continue without migrating existing data?", dataDir.toAbsolutePath()));
        }
        return List.of();
    }
}

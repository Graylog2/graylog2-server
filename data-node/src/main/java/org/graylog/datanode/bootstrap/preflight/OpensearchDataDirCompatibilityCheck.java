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
package org.graylog.datanode.bootstrap.preflight;

import com.github.joschi.jadconfig.ValidationException;
import jakarta.inject.Inject;
import org.graylog.datanode.DirectoryReadableValidator;
import org.graylog.datanode.configuration.DatanodeConfiguration;
import org.graylog.datanode.filesystem.index.IncompatibleIndexVersionException;
import org.graylog.datanode.filesystem.index.IndicesDirectoryParser;
import org.graylog.datanode.filesystem.index.dto.IndexerDirectoryInformation;
import org.graylog.datanode.filesystem.index.dto.NodeInformation;
import org.graylog.shaded.opensearch2.org.opensearch.Version;
import org.graylog2.bootstrap.preflight.PreflightCheck;
import org.graylog2.bootstrap.preflight.PreflightCheckException;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.Locale;
import java.util.Optional;

public class OpensearchDataDirCompatibilityCheck implements PreflightCheck {

    private static final Logger LOG = LoggerFactory.getLogger(OpensearchDataDirCompatibilityCheck.class);

    private final DatanodeConfiguration datanodeConfiguration;
    private final IndicesDirectoryParser indicesDirectoryParser;
    private final ClusterConfigService clusterConfigService;
    private final DirectoryReadableValidator directoryReadableValidator = new DirectoryReadableValidator();


    @Inject
    public OpensearchDataDirCompatibilityCheck(DatanodeConfiguration datanodeConfiguration, IndicesDirectoryParser indicesDirectoryParser, ClusterConfigService clusterConfigService) {
        this.datanodeConfiguration = datanodeConfiguration;
        this.indicesDirectoryParser = indicesDirectoryParser;
        this.clusterConfigService = clusterConfigService;
    }

    @Override
    public void runCheck() throws PreflightCheckException {

        final Path opensearchDataDir = datanodeConfiguration.datanodeDirectories().getDataTargetDir();
        final String opensearchVersion = datanodeConfiguration.opensearchDistributionProvider().get().version();

        // We want to run the compatibility check only once for this specific data dir and opensearch version. Let's memorize
        // the run and these two parameters and skip every time we are starting in the same configuration.
        // Change in the dir or in opensearch version will re-run the full check again.
        final CompatibilityCheck previousCompatibilityCheck = clusterConfigService.get(CompatibilityCheck.class);
        final boolean compatibilityAlreadyVerified = Optional.ofNullable(previousCompatibilityCheck)
                .map(result -> result.isMatching(opensearchDataDir, opensearchVersion))
                .orElse(false);

        if (compatibilityAlreadyVerified) {
            LOG.info("Opensearch data directory compatibility already successfully verified for data directory {} and opensearch version {}, skipping check", opensearchDataDir, opensearchVersion);
            return;
        }

        try {
            directoryReadableValidator.validate(opensearchDataDir.toUri().toString(), opensearchDataDir);
            final IndexerDirectoryInformation info = indicesDirectoryParser.parse(opensearchDataDir);
            checkCompatibility(opensearchVersion, info);
            final int indicesCount = info.nodes().stream().mapToInt(n -> n.indices().size()).sum();
            LOG.info("Found {} indices and all of them are valid with current opensearch version {}", indicesCount, opensearchVersion);
            // The check succeeded, let's remember this configuration and skip next time
            clusterConfigService.write(new CompatibilityCheck(opensearchDataDir, opensearchVersion));
        } catch (IncompatibleIndexVersionException e) {
            throw new PreflightCheckException("Index directory is not compatible with current version " + opensearchVersion + " of Opensearch, terminating.", e);
        } catch (ValidationException e) {
            throw new PreflightCheckException(e);
        }
    }

    private void checkCompatibility(String opensearchVersion, IndexerDirectoryInformation info) {
        final Version currentVersion = Version.fromString(opensearchVersion);
        for (NodeInformation node : info.nodes()) {
            final Version nodeVersion = Version.fromString(node.nodeVersion());
            if (node.nodeVersion() != null && !currentVersion.isCompatible(nodeVersion)) {
                final String error = String.format(Locale.ROOT, "Current version %s of Opensearch is not compatible with index version %s", currentVersion, nodeVersion);
                throw new IncompatibleIndexVersionException(error);
            }
        }
    }

    record CompatibilityCheck(Path opensearchDataDir, String opensearchVersion) {
        public boolean isMatching(Path opensearchDataDir, String opensearchVersion) {
            return this.opensearchDataDir.equals(opensearchDataDir) && this.opensearchVersion.equals(opensearchVersion);
        }
    }
}

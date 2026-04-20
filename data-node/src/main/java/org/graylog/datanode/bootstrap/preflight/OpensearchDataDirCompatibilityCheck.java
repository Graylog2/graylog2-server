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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

public class OpensearchDataDirCompatibilityCheck implements PreflightCheck {

    private static final Logger LOG = LoggerFactory.getLogger(OpensearchDataDirCompatibilityCheck.class);

    /**
     * Name of the marker file written into the opensearch data directory after a successful compatibility check.
     * The file contains the opensearch version string; the path context is implicit from the file's location.
     */
    static final String COMPATIBILITY_CHECK_FILENAME = ".dn-compat-check";

    private final DatanodeConfiguration datanodeConfiguration;
    private final IndicesDirectoryParser indicesDirectoryParser;
    private final DirectoryReadableValidator directoryReadableValidator = new DirectoryReadableValidator();

    @Inject
    public OpensearchDataDirCompatibilityCheck(DatanodeConfiguration datanodeConfiguration, IndicesDirectoryParser indicesDirectoryParser) {
        this.datanodeConfiguration = datanodeConfiguration;
        this.indicesDirectoryParser = indicesDirectoryParser;
    }

    @Override
    public void runCheck() throws PreflightCheckException {

        final Path opensearchDataDir = datanodeConfiguration.datanodeDirectories().getDataTargetDir();
        final String opensearchVersion = datanodeConfiguration.opensearchDistributionProvider().get().version();

        // We want to run the compatibility check only once per major opensearch version for this data dir. Let's memorize
        // the run and skip every time we are starting with the same major version in the same data dir.
        // A change in the major version will re-run the full check; minor/patch upgrades are skipped.
        if (isCompatibilityAlreadyVerified(opensearchDataDir, opensearchVersion)) {
            LOG.info("Opensearch data directory compatibility already successfully verified for data directory {} and opensearch major version {}, skipping check", opensearchDataDir, Version.fromString(opensearchVersion).major);
            return;
        }

        try {
            directoryReadableValidator.validate(opensearchDataDir.toUri().toString(), opensearchDataDir);
            final IndexerDirectoryInformation info = indicesDirectoryParser.parse(opensearchDataDir);
            checkCompatibility(opensearchVersion, info);
            final int indicesCount = info.nodes().stream().mapToInt(n -> n.indices().size()).sum();
            LOG.info("Found {} indices and all of them are valid with current opensearch version {}", indicesCount, opensearchVersion);
            // The check succeeded, let's remember this configuration and skip next time
            writeCompatibilityCheckResult(opensearchDataDir, opensearchVersion);
        } catch (IncompatibleIndexVersionException e) {
            throw new PreflightCheckException("Index directory is not compatible with current version " + opensearchVersion + " of Opensearch, terminating.", e);
        } catch (ValidationException e) {
            throw new PreflightCheckException(e);
        }
    }

    private boolean isCompatibilityAlreadyVerified(Path opensearchDataDir, String opensearchVersion) {
        final Path checkFile = opensearchDataDir.resolve(COMPATIBILITY_CHECK_FILENAME);
        if (!Files.exists(checkFile)) {
            return false;
        }
        try {
            final String storedVersion = Files.readString(checkFile, StandardCharsets.UTF_8).trim();
            final int storedMajor = Version.fromString(storedVersion).major;
            final int currentMajor = Version.fromString(opensearchVersion).major;
            return storedMajor == currentMajor;
        } catch (Exception e) {
            LOG.warn("Failed to read compatibility check file, re-running check", e);
            return false;
        }
    }

    private void writeCompatibilityCheckResult(Path opensearchDataDir, String opensearchVersion) {
        final Path checkFile = opensearchDataDir.resolve(COMPATIBILITY_CHECK_FILENAME);
        try {
            Files.writeString(checkFile, opensearchVersion, StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOG.warn("Failed to write compatibility check result, check will re-run on next startup", e);
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
}

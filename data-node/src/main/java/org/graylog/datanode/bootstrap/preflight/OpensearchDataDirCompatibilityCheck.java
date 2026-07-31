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

import com.github.zafarkhaja.semver.Version;
import jakarta.inject.Inject;
import org.graylog.datanode.configuration.DatanodeConfiguration;
import org.graylog.datanode.filesystem.index.DataDirVerificationMarker;
import org.graylog.datanode.filesystem.index.OpensearchDataDirCompatibility;
import org.graylog.datanode.filesystem.index.OpensearchDataDirCompatibilityService;
import org.graylog2.bootstrap.preflight.PreflightCheck;
import org.graylog2.bootstrap.preflight.PreflightCheckException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

import static org.graylog2.shared.utilities.StringUtils.f;

public class OpensearchDataDirCompatibilityCheck implements PreflightCheck {

    private static final Logger LOG = LoggerFactory.getLogger(OpensearchDataDirCompatibilityCheck.class);

    private final DatanodeConfiguration datanodeConfiguration;
    private final OpensearchDataDirCompatibilityService compatibilityService;
    private final DataDirVerificationMarker verificationMarker;

    @Inject
    public OpensearchDataDirCompatibilityCheck(DatanodeConfiguration datanodeConfiguration,
                                               OpensearchDataDirCompatibilityService compatibilityService,
                                               DataDirVerificationMarker verificationMarker) {
        this.datanodeConfiguration = datanodeConfiguration;
        this.compatibilityService = compatibilityService;
        this.verificationMarker = verificationMarker;
    }

    @Override
    public void runCheck() throws PreflightCheckException {

        final Path opensearchDataDir = datanodeConfiguration.datanodeDirectories().getDataTargetDir();
        final String opensearchVersion = datanodeConfiguration.opensearchDistribution().version();

        // We want to run the compatibility check only once per major opensearch version for this data dir. The marker
        // file tells us which major already opened it successfully, so we can skip the full scan when nothing about
        // the format can have changed. A change in the major version re-runs the full check; minor/patch upgrades are
        // skipped. The marker itself is written once opensearch is actually up, see OpensearchVersionTracer.
        if (verificationMarker.isVerifiedFor(opensearchDataDir, opensearchVersion)) {
            LOG.info("Opensearch data directory compatibility already successfully verified for data directory {} and opensearch major version {}, skipping check", opensearchDataDir, Version.parse(opensearchVersion).majorVersion());
            return;
        }

        final OpensearchDataDirCompatibility compatibility = compatibilityService.check();
        compatibility.warnings().forEach(LOG::warn);

        if (!compatibility.isCompatible()) {
            throw new PreflightCheckException(f("Index directory %s is not compatible with current version %s of Opensearch, terminating. %s",
                    opensearchDataDir, opensearchVersion, String.join(" ", compatibility.errors())));
        }

        LOG.info("Found {} indices and all of them are valid with current opensearch version {}", compatibility.indicesCount(), opensearchVersion);
    }
}

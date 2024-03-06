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

import org.graylog.datanode.OpensearchDistribution;
import org.graylog.datanode.configuration.DatanodeConfiguration;
import org.graylog2.bootstrap.preflight.PreflightCheck;
import org.graylog2.bootstrap.preflight.PreflightCheckException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.inject.Inject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class OpensearchBinPreflightCheck implements PreflightCheck {

    private final Supplier<OpensearchDistribution> opensearchDistributionSupplier;

    private static final Logger LOG = LoggerFactory.getLogger(OpensearchBinPreflightCheck.class);

    @Inject
    public OpensearchBinPreflightCheck(DatanodeConfiguration datanodeConfiguration) {
        this(datanodeConfiguration.opensearchDistributionProvider()::get);
    }

    OpensearchBinPreflightCheck(Supplier<OpensearchDistribution> distribution) {
        this.opensearchDistributionSupplier = distribution;
    }

    @Override
    public void runCheck() throws PreflightCheckException {
        final OpensearchDistribution distribution = opensearchDistributionSupplier.get();
        final Path opensearchDir = distribution.directory();

        if (!Files.isDirectory(opensearchDir)) {
            throw new PreflightCheckException("Opensearch base directory " + opensearchDir + " doesn't exist!");
        }

        final Path binPath = distribution.getOpensearchExecutable();

        if (!Files.exists(binPath)) {
            throw new PreflightCheckException("Opensearch binary " + binPath + " doesn't exist!");
        }

        if (!Files.isExecutable(binPath)) {
            final String permissions = getPermissions(binPath)
                    .map(p -> " Permissions of the binary are: " + p)
                    .orElse("");
            throw new PreflightCheckException("Opensearch binary " + binPath + " is not executable!" + permissions);
        }
    }

    private static Optional<String> getPermissions(Path binPath) {
        try {
            return Optional.of(Files.getPosixFilePermissions(binPath))
                    .map(perms -> perms.stream().map(Enum::toString).collect(Collectors.joining(",")));
        } catch (IOException e) {
            LOG.warn("Failed to obtain opensearch binary permissions: " + e.getMessage());
            return Optional.empty();
        }
    }
}

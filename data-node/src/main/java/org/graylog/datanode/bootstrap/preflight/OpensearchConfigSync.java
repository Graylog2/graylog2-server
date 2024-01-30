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

import org.graylog.datanode.configuration.DatanodeConfiguration;
import org.graylog2.bootstrap.preflight.PreflightCheck;
import org.graylog2.bootstrap.preflight.PreflightCheckException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.inject.Inject;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Collections;
import java.util.Set;

public class OpensearchConfigSync implements PreflightCheck {

    private static final Logger LOG = LoggerFactory.getLogger(OpensearchConfigSync.class);

    private final DatanodeConfiguration configuration;

    @Inject
    public OpensearchConfigSync(DatanodeConfiguration datanodeConfiguration) {
        this.configuration = datanodeConfiguration;
    }

    @Override
    public void runCheck() throws PreflightCheckException {
        try {

            final Path opensearchProcessConfigurationDir = configuration.datanodeDirectories().createOpensearchProcessConfigurationDir();
            LOG.info("Directory used for Opensearch process configuration is {}", opensearchProcessConfigurationDir.toAbsolutePath());

            // this is a directory in main/resources that holds all the initial configuration files needed by the opensearch
            // we manage this directory in git. Generally we assume that this is a read-only location and we need to copy
            // its content to a read-write location for the managed opensearch process.
            // This copy happens during each opensearch process start and will override any files that already exist
            // from previous runs.
            final Path sourceOfInitialConfiguration = Path.of("opensearch", "config");
            synchronizeConfig(sourceOfInitialConfiguration, opensearchProcessConfigurationDir);
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException("Failed to prepare opensearch config directory", e);
        }
    }

    public void synchronizeConfig(Path configRelativePath, final Path target) throws URISyntaxException, IOException {
        final URI uriToConfig = OpensearchConfigSync.class.getResource("/" + configRelativePath.toString()).toURI();
        if ("jar".equals(uriToConfig.getScheme())) {
            copyFromJar(configRelativePath, target, uriToConfig);
        } else {
            copyFromLocalFs(configRelativePath, target);
        }
    }

    private static void copyFromJar(Path configRelativePath, Path target, URI uri) throws IOException {
        try (
                final FileSystem fs = FileSystems.newFileSystem(uri, Collections.emptyMap());
        ) {
            // Get hold of the path to the top level directory of the JAR file
            final Path resourcesRoot = fs.getPath("/");
            final Path source = resourcesRoot.resolve(configRelativePath.toString()); // caution, the toString is needed here to resolve properly!
            copyRecursively(source, target);
        }
    }

    private static void copyFromLocalFs(Path configRelativePath, Path target) throws URISyntaxException, IOException {
        final Path resourcesRoot = Paths.get(OpensearchConfigSync.class.getResource("/").toURI());
        final Path source = resourcesRoot.resolve(configRelativePath);
        copyRecursively(source, target);
    }

    private static void copyRecursively(Path source, Path target) throws IOException {
        LOG.info("Synchronizing Opensearch configuration");
        FullDirSync.run(source, target);
    }
}

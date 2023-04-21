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

import org.graylog.datanode.Configuration;
import org.graylog2.bootstrap.preflight.PreflightCheck;
import org.graylog2.bootstrap.preflight.PreflightCheckException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collections;

public class OpensearchConfigCheck implements PreflightCheck {

    private static final Logger LOG = LoggerFactory.getLogger(OpensearchConfigCheck.class);

    private final Configuration configuration;

    @Inject
    public OpensearchConfigCheck(Configuration configuration) {
        this.configuration = configuration;
    }

    @Override
    public void runCheck() throws PreflightCheckException {
        final Path localOpensearchConfigDir = Path.of(configuration.getOpensearchConfigLocation()).resolve("opensearch");
        LOG.info("Directory used for Opensearch configuration is {}", localOpensearchConfigDir.toAbsolutePath());

        try {
            Files.createDirectories(localOpensearchConfigDir);
            synchronizeConfig(Path.of("opensearch", "config"), localOpensearchConfigDir);
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException("Failed to prepare opensearch config directory", e);

        }
    }

    public void synchronizeConfig(Path configRelativePath, final Path target) throws URISyntaxException, IOException {
        final URI uriToConfig = OpensearchConfigCheck.class.getResource("/" + configRelativePath.toString()).toURI();
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
        final Path resourcesRoot = Paths.get(OpensearchConfigCheck.class.getResource("/").toURI());
        final Path source = resourcesRoot.resolve(configRelativePath);
        copyRecursively(source, target);
    }

    private static void copyRecursively(Path source, Path target) throws IOException {
        LOG.info("Synchronizing Opensearch configuration");
        Files.walkFileTree(source, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                Path currentTarget = target.resolve(source.relativize(dir).toString());
                Files.createDirectories(currentTarget);
                LOG.info("Synchronizing directory {}", currentTarget);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                final Path currentTarget = target.resolve(source.relativize(file).toString());
                Files.copy(file, currentTarget, StandardCopyOption.REPLACE_EXISTING);
                LOG.info("Synchronizing file {}", currentTarget);
                return FileVisitResult.CONTINUE;
            }
        });
    }
}

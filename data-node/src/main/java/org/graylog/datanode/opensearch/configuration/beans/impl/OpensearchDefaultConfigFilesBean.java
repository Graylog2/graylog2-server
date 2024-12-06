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
package org.graylog.datanode.opensearch.configuration.beans.impl;

import org.graylog.datanode.opensearch.configuration.ConfigurationBuildParams;
import org.graylog.datanode.opensearch.configuration.beans.OpensearchConfigurationBean;
import org.graylog.datanode.opensearch.configuration.beans.OpensearchConfigurationPart;
import org.graylog.datanode.opensearch.configuration.beans.files.ConfigFile;
import org.graylog.datanode.opensearch.configuration.beans.files.InputStreamConfigFile;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
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
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class OpensearchDefaultConfigFilesBean implements OpensearchConfigurationBean {

    @Override
    public OpensearchConfigurationPart buildConfigurationPart(ConfigurationBuildParams trustedCertificates) {
        return OpensearchConfigurationPart.builder()
                .configFiles(collectConfigFiles())
                .build();
    }

    private List<ConfigFile> collectConfigFiles() {
        // this is a directory in main/resources that holds all the initial configuration files needed by the opensearch
        // we manage this directory in git. Generally we assume that this is a read-only location and we need to copy
        // its content to a read-write location for the managed opensearch process.
        // This copy happens during each opensearch process start and will override any files that already exist
        // from previous runs.
        final Path sourceOfInitialConfiguration = Path.of("opensearch", "config");
        try {
            return synchronizeConfig(sourceOfInitialConfiguration);
        } catch (URISyntaxException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    public List<ConfigFile> synchronizeConfig(Path configRelativePath) throws URISyntaxException, IOException {
        final URI uriToConfig = OpensearchDefaultConfigFilesBean.class.getResource("/" + configRelativePath.toString()).toURI();
        if ("jar".equals(uriToConfig.getScheme())) {
            return copyFromJar(configRelativePath, uriToConfig);
        } else {
            return copyFromLocalFs(configRelativePath);
        }
    }

    private static List<ConfigFile> copyFromJar(Path configRelativePath, URI uri) throws IOException {
        try (
                final FileSystem fs = FileSystems.newFileSystem(uri, Collections.emptyMap());
        ) {
            // Get hold of the path to the top level directory of the JAR file
            final Path resourcesRoot = fs.getPath("/");
            final Path source = resourcesRoot.resolve(configRelativePath.toString()); // caution, the toString is needed here to resolve properly!
            return collectRecursively(source);
        }
    }

    private static List<ConfigFile> copyFromLocalFs(Path configRelativePath) throws URISyntaxException, IOException {
        final Path resourcesRoot = Paths.get(OpensearchDefaultConfigFilesBean.class.getResource("/").toURI());
        final Path source = resourcesRoot.resolve(configRelativePath);
        return collectRecursively(source);
    }

    private static List<ConfigFile> collectRecursively(Path source) throws IOException {
        List<ConfigFile> configFiles = new LinkedList<>();
        Files.walkFileTree(source, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path sourceFile, BasicFileAttributes attrs) {
                final Path relativePath = source.relativize(sourceFile);
                try {
                    configFiles.add(new InputStreamConfigFile(relativePath, new FileInputStream(sourceFile.toFile())));
                } catch (FileNotFoundException e) {
                    throw new RuntimeException(e);
                }
                return FileVisitResult.CONTINUE;
            }
        });
        return configFiles;
    }
}

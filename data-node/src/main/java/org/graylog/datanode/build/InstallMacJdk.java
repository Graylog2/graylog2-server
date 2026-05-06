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
package org.graylog.datanode.build;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Copies pre-downloaded Mac JDKs from a staging directory into each OpenSearch distribution directory.
 * The staging directory must be organized as {@code <stagingDir>/<opensearchVersion>/<arch>/}.
 */
public class InstallMacJdk {

    private static final Pattern DIST_PATTERN = Pattern.compile("^opensearch-(\\d+\\.\\d+\\.\\d+)-linux-(x64|aarch64)$");

    public static void main(String[] args) throws IOException {
        if (args.length != 2) {
            System.err.println("Syntax: " + InstallMacJdk.class.getSimpleName() + " <opensearchParentDir> <macJdkStagingDir>");
            System.exit(1);
        }

        final Path opensearchParentDir = Path.of(args[0]);
        final Path macJdkStagingDir = Path.of(args[1]);

        try (Stream<Path> dirs = Files.list(opensearchParentDir)) {
            dirs.filter(Files::isDirectory)
                    .sorted()
                    .forEach(distDir -> {
                        final Matcher m = DIST_PATTERN.matcher(distDir.getFileName().toString());
                        if (!m.matches()) {
                            System.err.println("Skipping unexpected directory: " + distDir.getFileName());
                            return;
                        }
                        final Path jdkSource = macJdkStagingDir.resolve(m.group(1)).resolve(m.group(2));
                        try {
                            installJdk(distDir, jdkSource);
                        } catch (IOException e) {
                            throw new RuntimeException("Failed to install Mac JDK for " + distDir.getFileName(), e);
                        }
                    });
        }
    }

    private static void installJdk(Path distDir, Path jdkSource) throws IOException {
        final Path jdkTarget = distDir.resolve("jdk-mac");
        System.out.println("Installing Mac JDK: " + jdkSource + " -> " + jdkTarget);
        copyDirectory(jdkSource, jdkTarget);
    }

    private static void copyDirectory(Path source, Path target) throws IOException {
        try (Stream<Path> stream = Files.walk(source)) {
            for (final Path sourcePath : stream.toList()) {
                final Path targetPath = target.resolve(source.relativize(sourcePath));
                if (Files.isDirectory(sourcePath)) {
                    Files.createDirectories(targetPath);
                } else {
                    Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }
}

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
package org.graylog.datanode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record OpensearchDistribution(Path directory, String version, @Nullable String platform,
                                     @Nullable String architecture) {

    private static final Logger LOG = LoggerFactory.getLogger(OpensearchDistribution.class);
    public static final Pattern FULL_NAME_PATTERN = Pattern.compile("opensearch-(.*)-(.+)-(.+)");
    public static final Pattern SHORT_NAME_PATTERN = Pattern.compile("opensearch-(.*)");

    public OpensearchDistribution(Path path, String version) {
        this(path, version, null, null);
    }

    public static OpensearchDistribution detectInDirectory(Path directory) throws IOException {
        final var osArch = System.getProperty("os.arch");
        return detectInDirectory(directory, osArch);
    }

    static OpensearchDistribution detectInDirectory(Path rootDistDirectory, String osArch) throws IOException {
        Objects.requireNonNull(rootDistDirectory, "Dist directory needs to be provided");

        // if the base directory points directly to one opensearch distribution, we should return it directly.
        // If the format doesn't fit, we'll look for opensearch distributions in this root directory.
        final Optional<OpensearchDistribution> distDirectory = parse(rootDistDirectory);
        return distDirectory.orElseGet(() -> detectInSubdirectory(rootDistDirectory, osArch));

    }

    private static OpensearchDistribution detectInSubdirectory(Path directory, String osArch) {
        final List<OpensearchDistribution> opensearchDistributions;
        try (
                var files = Files.list(directory);
        ) {
            opensearchDistributions = files
                    .filter(Files::isDirectory)
                    .flatMap(f -> parse(f).stream())
                    .toList();
        } catch (IOException e) {
            throw new RuntimeException("Failed to list content of provided directory " + directory.toAbsolutePath(), e);
        }

        if (opensearchDistributions.isEmpty()) {
            throw new IllegalArgumentException("Could not detect any opensearch distribution in " + directory.toAbsolutePath());
        }

        LOG.info("Found following opensearch distributions: " + opensearchDistributions.stream().map(d -> d.directory().toAbsolutePath()).toList());

        final var archCode = archCode(osArch);

        return findByArchitecture(opensearchDistributions, archCode)
                .orElseGet(() -> findWithoutArchitecture(opensearchDistributions)
                        .orElseThrow(() -> new IllegalArgumentException("No Opensearch distribution found for architecture " + osArch)));
    }

    private static String archCode(final String osArch) {
        return switch (osArch) {
            case "amd64" -> "x64";
            case "x86_64" -> "x64";
            case "aarch64" -> "aarch64";
            case "arm64" -> "aarch64";
            default ->
                    throw new UnsupportedOperationException("Unsupported OpenSearch distribution architecture: " + osArch);
        };
    }

    private static Optional<OpensearchDistribution> findByArchitecture(List<OpensearchDistribution> available, String archCode) {
        return available.stream()
                .filter(d -> archCode.equals(d.architecture()))
                .findAny();
    }

    private static Optional<OpensearchDistribution> findWithoutArchitecture(List<OpensearchDistribution> available) {
        return available.stream().filter(d -> d.architecture() == null).findFirst();
    }

    private static Optional<OpensearchDistribution> parse(Path path) {
        final String filename = path.getFileName().toString();
        final Matcher matcher = FULL_NAME_PATTERN.matcher(filename);
        if (matcher.matches()) {
            return Optional.of(new OpensearchDistribution(path, matcher.group(1), matcher.group(2), matcher.group(3)));
        } else {
            final Matcher shortMatcher = SHORT_NAME_PATTERN.matcher(filename);
            if (shortMatcher.matches()) {
                return Optional.of(new OpensearchDistribution(path, shortMatcher.group(1)));
            } else {
                return Optional.empty();
            }
        }
    }
}

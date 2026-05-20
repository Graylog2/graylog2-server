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
package org.graylog.datanode.configuration;

import com.github.zafarkhaja.semver.Version;
import com.google.common.base.Suppliers;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;
import org.graylog.datanode.Configuration;
import org.graylog.datanode.OpensearchDistribution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.graylog2.shared.utilities.StringUtils.f;

@Singleton
public class OpensearchDistributionProvider implements Provider<OpensearchDistribution> {

    private static final Logger LOG = LoggerFactory.getLogger(OpensearchDistributionProvider.class);
    public static final Pattern FULL_NAME_PATTERN = Pattern.compile("opensearch-(.*)-(.+)-(.+)");
    public static final Pattern SHORT_NAME_PATTERN = Pattern.compile("opensearch-(.*)");

    private final Supplier<OpensearchDistribution> distribution;

    @Inject
    public OpensearchDistributionProvider(final Configuration localConfiguration,
                                          final OpensearchVersionSelector versionSelector) {
        this(
                Path.of(localConfiguration.getOpensearchDistributionRoot()),
                OpensearchArchitecture.fromOperatingSystem(),
                versionSelector.requestedVersion()
        );
    }

    public OpensearchDistributionProvider(final Path opensearchDistributionRoot, OpensearchArchitecture architecture) {
        this(opensearchDistributionRoot, architecture, Optional.empty());
    }

    public OpensearchDistributionProvider(final Path opensearchDistributionRoot, OpensearchArchitecture architecture, Optional<String> requestedVersion) {
        this.distribution = Suppliers.memoize(() -> detectInDirectory(opensearchDistributionRoot, architecture, requestedVersion));
    }

    @Override
    public OpensearchDistribution get() {
        return distribution.get();
    }

    private static OpensearchDistribution detectInDirectory(Path rootDistDirectory, OpensearchArchitecture osArch, Optional<String> requestedVersion) {
        Objects.requireNonNull(rootDistDirectory, "Dist directory needs to be provided");

        // if the base directory points directly to one opensearch distribution, we should return it directly.
        // If the format doesn't fit, we'll look for opensearch distributions in this root directory.
        final Optional<OpensearchDistribution> distDirectory = parse(rootDistDirectory);
        return distDirectory.orElseGet(() -> detectInSubdirectory(rootDistDirectory, osArch, requestedVersion));
    }

    private static OpensearchDistribution detectInSubdirectory(Path directory, OpensearchArchitecture arch, Optional<String> requestedVersion) {
        final List<OpensearchDistribution> opensearchDistributions;
        try (
                var files = Files.list(directory);
        ) {
            opensearchDistributions = files
                    .filter(Files::isDirectory)
                    .flatMap(f -> parse(f).stream())
                    .toList();
        } catch (IOException e) {
            throw createErrorMessage(directory, arch, "Failed to list content of provided directory", e);
        }

        if (opensearchDistributions.isEmpty()) {
            throw createErrorMessage(directory, arch, "Could not detect any opensearch distribution");
        }

        LOG.info("Found following opensearch distributions: " + opensearchDistributions.stream().map(d -> d.directory().toAbsolutePath()).toList());

        final List<OpensearchDistribution> candidates = requestedVersion
                .map(v -> filterByVersion(opensearchDistributions, v))
                .orElse(opensearchDistributions);

        final OpensearchDistribution result = findByArchitecture(candidates, arch)
                .orElseGet(() -> findWithoutArchitecture(candidates)
                        .orElseThrow(() -> createErrorMessage(directory, arch, "No Opensearch distribution found for your system architecture")));
        LOG.info("Using opensearch distribution {}", result.directory().toAbsolutePath());
        return result;
    }

    private static List<OpensearchDistribution> filterByVersion(List<OpensearchDistribution> distributions, String version) {
        final List<OpensearchDistribution> matches = distributions.stream()
                .filter(d -> version.equals(d.version()))
                .toList();
        if (matches.isEmpty()) {
            throw new IllegalArgumentException(f(
                    "No OpenSearch distribution found for requested version '%s'. Available versions: %s",
                    version,
                    distributions.stream().map(OpensearchDistribution::version).distinct().sorted().toList()
            ));
        }
        return matches;
    }

    private static IllegalArgumentException createErrorMessage(Path directory, OpensearchArchitecture arch, String message) {
        return createErrorMessage(directory, arch, message, null);
    }


    private static IllegalArgumentException createErrorMessage(Path directory, OpensearchArchitecture arch, String errorMessage, Exception cause) {
        final String message = String.format(Locale.ROOT, "%s. Directory used for Opensearch detection: %s. Please configure opensearch_location to a directory that contains an opensearch distribution for your architecture %s. You can download Opensearch from https://opensearch.org/downloads.html . Please extract the downloaded distribution and point opensearch_location configuration option to that directory.", errorMessage, directory.toAbsolutePath(), arch);
        return new IllegalArgumentException(message, cause);
    }

    public static String archCode(final String osArch) {
        return switch (osArch) {
            case "amd64" -> "x64";
            case "x86_64" -> "x64";
            case "aarch64" -> "aarch64";
            case "arm64" -> "aarch64";
            default ->
                    throw new UnsupportedOperationException("Unsupported OpenSearch distribution architecture: " + osArch);
        };
    }

    private static Optional<OpensearchDistribution> findByArchitecture(List<OpensearchDistribution> available, OpensearchArchitecture arch) {
        return available.stream()
                .filter(d -> arch.equals(d.architecture()))
                .min(Comparator.comparing(OpensearchDistribution::version, OpensearchDistributionProvider::compareVersions));
    }

    private static Optional<OpensearchDistribution> findWithoutArchitecture(List<OpensearchDistribution> available) {
        return available.stream()
                .filter(d -> d.architecture() == null)
                .min(Comparator.comparing(OpensearchDistribution::version, OpensearchDistributionProvider::compareVersions));
    }

    private static int compareVersions(String v1, String v2) {
        final Version version1 = Version.parse(v1);
        final Version version2 = Version.parse(v2);
        return version1.compareTo(version2);
    }

    private static Optional<OpensearchDistribution> parse(Path path) {
        final String filename = path.getFileName().toString();
        final Matcher matcher = FULL_NAME_PATTERN.matcher(filename);
        if (matcher.matches()) {
            return Optional.of(new OpensearchDistribution(path, matcher.group(1), matcher.group(2), OpensearchArchitecture.fromCode(matcher.group(3))));
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

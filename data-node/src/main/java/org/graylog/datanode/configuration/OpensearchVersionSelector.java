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
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.graylog.datanode.Configuration;
import org.graylog.datanode.OpensearchDistribution;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import static org.graylog2.shared.utilities.StringUtils.f;

@Singleton
public class OpensearchVersionSelector {

    private final Optional<String> requestedVersion;

    @Inject
    public OpensearchVersionSelector(final Configuration configuration) {
        this.requestedVersion = Optional.ofNullable(configuration.getOpensearchVersion());
    }

    OpensearchVersionSelector(final Optional<String> requestedVersion) {
        this.requestedVersion = requestedVersion;
    }

    public OpensearchDistribution select(final List<OpensearchDistribution> candidates) {
        final List<OpensearchDistribution> versionFiltered = requestedVersion
                .map(v -> filterByVersion(candidates, v))
                .orElse(candidates);
        return versionFiltered.stream()
                .min(Comparator.comparing(OpensearchDistribution::version, OpensearchVersionSelector::compareVersions))
                .orElseThrow(() -> new IllegalArgumentException("No suitable OpenSearch distribution found"));
    }

    private static List<OpensearchDistribution> filterByVersion(final List<OpensearchDistribution> distributions, final String version) {
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

    private static int compareVersions(final String v1, final String v2) {
        return Version.parse(v1).compareTo(Version.parse(v2));
    }
}

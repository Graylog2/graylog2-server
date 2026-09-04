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
package org.graylog.datanode.filesystem.index;

import com.github.zafarkhaja.semver.Version;
import jakarta.annotation.Nullable;
import org.graylog.datanode.filesystem.index.dto.IndexInformation;
import org.graylog.datanode.filesystem.index.dto.IndexerDirectoryInformation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.stream.Stream;

/**
 * @param info                  what the directory contains
 * @param requiredDistribution  the opensearch distribution needed to open it.
 *                              {@link RequiredOpensearchDistribution#CURRENT} unless at least one shard or state
 *                              file could only be read by the compatibility reader.
 */
public record IndicesDirectoryParseResult(IndexerDirectoryInformation info,
                                          RequiredOpensearchDistribution requiredDistribution) {

    private static final Logger LOG = LoggerFactory.getLogger(IndicesDirectoryParseResult.class);

    /**
     * Whether the given opensearch version is able to open everything in this directory.
     * <p>
     * An index can be opened by the opensearch generation that created it and by the following one, and by no other:
     * a newer release reads it and migrates it forward, an older one cannot read it at all. That makes the answer
     * asymmetric, which is why {@code OpensearchUtils#isCompatible} is not enough here — it treats a candidate one
     * generation <em>behind</em> the data as acceptable, and picking such a candidate leaves opensearch unable to
     * open its own indices.
     * <p>
     * The comparison runs on {@link OpensearchUtils#generation(Version) generations} rather than on major versions,
     * so data written by elasticsearch 7.x counts as the opensearch 1.x generation it shares its index format with.
     * <p>
     * A directory we can't extract any version from imposes no constraint. That means a corrupt or partial directory
     * is not silently narrowed down here; the preflight check reports it with a message naming the actual indices.
     */
    public boolean canBeOpenedBy(String opensearchVersion) {
        final Long parsed = generationOrNull(opensearchVersion);
        if (parsed == null) {
            return true;
        }
        final long candidateGeneration = parsed;
        return dataGenerations()
                .allMatch(dataGeneration -> candidateGeneration == dataGeneration
                        || candidateGeneration == dataGeneration + 1);
    }

    /**
     * The distinct index generations that wrote this directory, taken from the node and index state files.
     */
    private Stream<Long> dataGenerations() {
        return info.nodes().stream()
                .flatMap(node -> Stream.concat(
                        Stream.ofNullable(node.nodeVersion()),
                        node.indices().stream().map(IndexInformation::indexVersionCreated)))
                .filter(Objects::nonNull)
                .map(IndicesDirectoryParseResult::generationOrNull)
                .filter(Objects::nonNull)
                .distinct();
    }

    @Nullable
    private static Long generationOrNull(String version) {
        try {
            return OpensearchUtils.generation(Version.parse(version));
        } catch (Exception e) {
            LOG.warn("Failed to parse opensearch version {}, ignoring it when deciding which distribution can open "
                    + "the data directory", version, e);
            return null;
        }
    }
}

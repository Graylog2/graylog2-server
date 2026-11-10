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

import jakarta.annotation.Nullable;
import org.graylog.datanode.filesystem.index.dto.IndexerDirectoryInformation;

import java.nio.file.Path;
import java.util.List;

/**
 * Outcome of an {@link OpensearchDataDirCompatibilityService#check()} run.
 *
 * @param requiredDistribution the opensearch distribution that could read the directory, {@code null} if it
 *                             couldn't be read at all. Needing
 *                             {@link RequiredOpensearchDistribution#COMPAT} is not an error on its own — the data is
 *                             readable, it just predates the current distribution.
 * @param errors               reasons why the data directory can't be used with {@code opensearchVersion}. Empty if
 *                             compatible.
 * @param warnings             problems that don't prevent usage of the data directory, but the user should know
 *                             about them.
 */
public record OpensearchDataDirCompatibility(Path dataDir,
                                             String opensearchVersion,
                                             IndexerDirectoryInformation info,
                                             @Nullable RequiredOpensearchDistribution requiredDistribution,
                                             List<String> errors,
                                             List<String> warnings) {

    /**
     * The data directory couldn't be inspected at all, e.g. because it's not readable, or because neither the
     * current nor the compatibility distribution could make sense of its content.
     */
    public static OpensearchDataDirCompatibility failed(Path dataDir, String opensearchVersion, String error) {
        return new OpensearchDataDirCompatibility(dataDir, opensearchVersion, IndexerDirectoryInformation.empty(dataDir),
                null, List.of(error), List.of());
    }

    public boolean isCompatible() {
        return errors.isEmpty();
    }

    public int indicesCount() {
        return info.nodes().stream().mapToInt(node -> node.indices().size()).sum();
    }
}

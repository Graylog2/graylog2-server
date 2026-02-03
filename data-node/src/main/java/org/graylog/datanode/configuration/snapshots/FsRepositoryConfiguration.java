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
package org.graylog.datanode.configuration.snapshots;

import com.github.joschi.jadconfig.Parameter;
import com.github.joschi.jadconfig.documentation.Documentation;
import jakarta.annotation.Nonnull;
import org.graylog.datanode.DirectoriesWritableValidator;
import org.graylog.datanode.PathListConverter;
import org.graylog.datanode.configuration.DatanodeDirectories;
import org.graylog.datanode.process.configuration.beans.OpensearchKeystoreItem;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class FsRepositoryConfiguration implements RepositoryConfiguration {
    /**
     * <a href="https://opensearch.org/docs/latest/tuning-your-cluster/availability-and-recovery/snapshots/snapshot-restore/#shared-file-system">See snapshot documentation</a>
     */
    @Documentation("Filesystem path where searchable snapshots should be stored")
    @Parameter(value = "path_repo", converter = PathListConverter.class, validators = DirectoriesWritableValidator.class)
    private List<Path> pathRepo;

    @Override
    public boolean isRepositoryEnabled() throws IllegalStateException {
        return pathRepo != null && !pathRepo.isEmpty();
    }

    @Override
    public Map<String, String> opensearchProperties() {
        // https://opensearch.org/docs/latest/tuning-your-cluster/availability-and-recovery/snapshots/snapshot-restore/#shared-file-system
        return Map.of("path.repo", serialize(pathRepo));
    }

    @Override
    public Collection<OpensearchKeystoreItem> keystoreItems(DatanodeDirectories datanodeDirectories) {
        return Collections.emptyList();
    }

    @Nonnull
    private String serialize(List<Path> pathRepo) {
        return pathRepo.stream().map(Path::toString).collect(Collectors.joining(","));
    }
}

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
import org.graylog.datanode.DirectoriesWritableValidator;
import org.graylog.datanode.PathListConverter;
import org.graylog.datanode.configuration.DatanodeDirectories;
import org.graylog.datanode.process.configuration.beans.OpensearchKeystoreItem;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class FsRepositoryConfiguration implements RepositoryConfiguration {

    private static final String PATH_REPO = "path.repo";

    private static final PathListConverter PATH_LIST_CONVERTER = new PathListConverter();

    /**
     * By default, this property is called path_repo. But the name is almost the same as opensearch property path.repo,
     * so people tend to use the dot instead of underscore here. Let's add the doc version as fallback, catching
     * possible misconfiguration, using it and providing a warning automatically if such situation occurs.
     *
     * <a href="https://opensearch.org/docs/latest/tuning-your-cluster/availability-and-recovery/snapshots/snapshot-restore/#shared-file-system">See snapshot documentation</a>
     */
    @Documentation("Filesystem path where searchable snapshots should be stored")
    @Parameter(value = "path_repo", fallbackPropertyName = "path.repo", converter = PathListConverter.class, validators = DirectoriesWritableValidator.class)
    private List<Path> pathRepo;

    public FsRepositoryConfiguration() {
    }

    public FsRepositoryConfiguration(List<Path> pathRepo) {
        this.pathRepo = List.copyOf(pathRepo);
    }

    @Override
    public boolean isRepositoryEnabled() throws IllegalStateException {
        return pathRepo != null && !pathRepo.isEmpty();
    }

    @Override
    public Map<String, String> opensearchProperties() {
        // https://opensearch.org/docs/latest/tuning-your-cluster/availability-and-recovery/snapshots/snapshot-restore/#shared-file-system
        return Map.of(PATH_REPO, PATH_LIST_CONVERTER.convertTo(pathRepo));
    }

    /**
     * path.repo is a node-level setting shared by all filesystem repositories, merge all their paths into one list.
     */
    @Override
    public String mergeProperty(String key, String existingValue, String newValue) {
        if (PATH_REPO.equals(key)) {
            final List<Path> paths = Stream.of(existingValue, newValue)
                    .flatMap(value -> PATH_LIST_CONVERTER.convertFrom(value).stream())
                    .distinct()
                    .toList();
            return PATH_LIST_CONVERTER.convertTo(paths);
        }
        return RepositoryConfiguration.super.mergeProperty(key, existingValue, newValue);
    }

    @Override
    public Collection<OpensearchKeystoreItem> keystoreItems(DatanodeDirectories datanodeDirectories) {
        return Collections.emptyList();
    }
}

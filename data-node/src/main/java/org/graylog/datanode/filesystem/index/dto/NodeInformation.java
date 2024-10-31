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
package org.graylog.datanode.filesystem.index.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.graylog.datanode.filesystem.index.statefile.StateFile;
import org.graylog.shaded.opensearch2.org.opensearch.Version;

import javax.annotation.Nullable;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public record NodeInformation(@JsonIgnore java.nio.file.Path nodePath, List<IndexInformation> indices,
                              @JsonIgnore @Nullable StateFile stateFile) {
    public static NodeInformation empty(Path nodePath) {
        return new NodeInformation(nodePath, Collections.emptyList(), null);
    }

    public boolean isEmpty() {
        return indices.isEmpty();
    }

    @JsonProperty
    public String nodeVersion() {
        return Optional.ofNullable(stateFile).map(sf -> (Integer) sf.document().get("node_version"))
                .map(Version::fromId)
                .map(Version::toString)
                .orElseGet(this::parseFromIndices);
    }

    private String parseFromIndices() {
        return indices.stream().map(IndexInformation::indexVersionCreated).distinct().findFirst().orElse(null);
    }
}

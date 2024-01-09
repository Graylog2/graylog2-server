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

import java.nio.file.Path;
 import java.util.Collections;
import java.util.List;

public record IndexerDirectoryInformation(@JsonIgnore Path path, List<NodeInformation> nodes) {
    public static IndexerDirectoryInformation empty(Path path) {
        return new IndexerDirectoryInformation(path, Collections.emptyList());
    }

    /**
     * The property name is matching the configuration property name, to minimize any confusion.
     * Jackson is by default serializing paths with file:/ prefix, so we are doing the conversion to string
     * here on our own to deliver only the real path value.
     */
    @JsonProperty("opensearch_data_location")
    public String baseDir() {
        return path.toString();
    }
}

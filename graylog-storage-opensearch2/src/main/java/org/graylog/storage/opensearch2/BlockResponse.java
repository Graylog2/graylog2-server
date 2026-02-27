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
package org.graylog.storage.opensearch2;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@JsonIgnoreProperties(ignoreUnknown = true)
public record BlockResponse(@JsonProperty("blocks") Blocks blocks) {

    /**
     * This is some internal format structure. IDK what the integer number in the map means. For now
     * we just ignore it and skip directly into the content.
     * @param indices
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    record Blocks(@JsonProperty("indices") Map<String, Map<Integer, IndexBlock>> indices) {
        public Optional<IndexBlock> forIndex(String indexName) {
            return Optional.ofNullable(indices())
                    .stream()
                    .flatMap(indices -> indices.entrySet().stream())
                    .filter(entry -> entry.getKey().equals(indexName))
                    .map(e -> e.getValue().values().iterator().next())
                    .findFirst();
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record IndexBlock(String description, boolean retryable, List<BlockLevel> levels) {}

    enum BlockLevel {
        read, write, metadata_write
    }
}

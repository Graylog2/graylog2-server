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
package org.graylog.plugins.views.storage.migration.state.actions;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CompatibilityResult(String hostname, String opensearchVersion,
                                  IndexerDirectoryInformation info,
                                  java.util.List<String> compatibilityErrors) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    record IndexerDirectoryInformation(List<NodeInformation> nodes,
                                       @JsonProperty("opensearch_data_location") String baseDir) {
    }


    @JsonIgnoreProperties(ignoreUnknown = true)
    record IndexInformation(String indexID, List<ShardInformation> shards, String indexName,
                            String indexVersionCreated, String creationDate) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record NodeInformation(List<CompatibilityResult.IndexInformation> indices, String nodeVersion) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ShardInformation(int documentsCount, String name, String minLuceneVersion, boolean primary) {
    }
}

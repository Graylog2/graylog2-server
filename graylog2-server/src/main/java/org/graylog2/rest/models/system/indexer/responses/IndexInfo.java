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
package org.graylog2.rest.models.system.indexer.responses;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;

import java.util.List;

@JsonAutoDetect
@AutoValue
public abstract class IndexInfo {

    @JsonProperty("index_name")
    public abstract String indexName();

    @JsonProperty("primary_shards")
    public abstract IndexStats primaryShards();

    @JsonProperty("all_shards")
    public abstract IndexStats allShards();

    @JsonProperty("routing")
    public abstract List<ShardRouting> routing();

    @JsonProperty("is_reopened")
    public abstract boolean isReopened();

    @JsonCreator
    public static IndexInfo create(@JsonProperty("index_name") String indexName,
                                   @JsonProperty("primary_shards") IndexStats primaryShards,
                                   @JsonProperty("all_shards") IndexStats allShards,
                                   @JsonProperty("routing") List<ShardRouting> routing,
                                   @JsonProperty("is_reopened") boolean isReopened) {
        return new AutoValue_IndexInfo(indexName, primaryShards, allShards, routing, isReopened);
    }
}

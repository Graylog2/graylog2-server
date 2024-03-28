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
package org.graylog2.indexer.datanode;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.graylog2.indexer.migration.LogEntry;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.mongojack.Id;
import org.mongojack.ObjectId;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public record MigrationConfiguration(
        @ObjectId
        @Id
        @Nullable
        @JsonProperty(FIELD_ID)
        String id,
        @JsonProperty(FIELD_CREATED)
        DateTime created,
        @JsonProperty(FIELD_INDICES)
        List<IndexMigrationConfiguration> indices,

        @JsonProperty(FIELD_LOGS)
        List<LogEntry> logs
) {
    public static final String FIELD_ID = "id";
    public static final String FIELD_INDICES = "indices";
    public static final String FIELD_CREATED = "created";
    public static final String FIELD_LOGS = "logs";

    public static MigrationConfiguration forIndices(List<String> indices) {
        return new MigrationConfiguration(null, new DateTime(DateTimeZone.UTC), indices.stream().map(indexName -> new IndexMigrationConfiguration(indexName, null)).collect(Collectors.toList()), Collections.emptyList());
    }

    @JsonIgnore
    public Optional<IndexMigrationConfiguration> getConfigForIndexName(String indexName) {
        return indices.stream().filter(i -> Objects.equals(i.indexName(), indexName)).findFirst();
    }
}


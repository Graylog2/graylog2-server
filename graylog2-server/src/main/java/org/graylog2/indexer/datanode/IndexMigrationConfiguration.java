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

import com.fasterxml.jackson.annotation.JsonProperty;

import javax.annotation.Nullable;
import java.util.Optional;

public record IndexMigrationConfiguration(
        @JsonProperty(FIELD_INDEX_NAME) String indexName,
        @Nullable @JsonProperty(FIELD_TASK_ID) String nullableTaskID
) {
    public static final String FIELD_INDEX_NAME = "indexName";
    public static final String FIELD_TASK_ID = "taskId";

    public Optional<String> taskId() {
        return Optional.ofNullable(nullableTaskID);
    }
}

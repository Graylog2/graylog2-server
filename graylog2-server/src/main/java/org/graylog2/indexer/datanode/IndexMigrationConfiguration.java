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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.annotation.Nullable;
import java.util.Optional;

public class IndexMigrationConfiguration {
    public static final String FIELD_INDEX_NAME = "indexName";
    public static final String FIELD_TASK_ID = "taskId";
    @JsonProperty(FIELD_INDEX_NAME)
    private final String indexName;

    @Nullable
    @JsonProperty(FIELD_TASK_ID)
    private String taskId;

    @JsonCreator
    public IndexMigrationConfiguration(@JsonProperty(FIELD_INDEX_NAME) String indexName, @JsonProperty(FIELD_TASK_ID) @Nullable String taskId) {
        this.indexName = indexName;
        this.taskId = taskId;
    }

    public IndexMigrationConfiguration(String indexName) {
        this.indexName = indexName;
    }

    public String indexName() {
        return indexName;
    }

    public Optional<String> taskId() {
        return Optional.ofNullable(taskId);
    }

    public void taskId(String taskId) {
        this.taskId = taskId;
    }
}

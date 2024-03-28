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
package org.graylog2.indexer.migration;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.jsonwebtoken.lang.Collections;
import jakarta.validation.constraints.NotNull;
import org.apache.commons.collections4.queue.CircularFifoQueue;
import org.graylog2.indexer.datanode.RemoteReindexingMigrationAdapter.Status;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.UUID;

/**
 * Caution: this object will be heavily mutated from outside as the migration progresses.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RemoteReindexMigration {

    @JsonProperty("id")
    private final String id;
    private final List<RemoteReindexIndex> indices;

    private final List<LogEntry> logs;

    @JsonProperty("error")
    private String error;

    public RemoteReindexMigration(@NotNull String migrationID, List<RemoteReindexIndex> indices, List<LogEntry> logs) {
        this.id = migrationID;
        this.indices = indices;
        this.logs = logs;
    }

    public static RemoteReindexMigration nonExistent(String migrationID) {
        return new RemoteReindexMigration(migrationID, Collections.emptyList(), Collections.emptyList());
    }

    @JsonProperty("indices")
    public List<RemoteReindexIndex> indices() {
        return indices;
    }

    public String id() {
        return id;
    }

    @JsonProperty("status")
    public Status status() {
        if (indices.isEmpty()) {
            return Status.NOT_STARTED;
        }
        if (indices().stream().map(RemoteReindexIndex::status).anyMatch(i -> i == Status.RUNNING)) {
            return Status.RUNNING;
        }
        if (indices().stream().map(RemoteReindexIndex::status).anyMatch(i -> i == Status.ERROR)) {
            return Status.ERROR;
        }
        return Status.FINISHED;
    }

    /**
     * @return How much of the migration is done, in percent, int value between 0 a 100.
     */
    @JsonProperty("progress")
    public int progress() {
        final int countOfIndices = indices.size();

        if (indices.isEmpty()) {
            return 100; // avoid division by zero. No indices == migration is immediately done
        }

        final int done = (int) indices.stream().map(RemoteReindexIndex::status).filter(i -> i == Status.FINISHED || i == Status.ERROR).count();
        float percent = (100.0f / countOfIndices) * done;
        return (int) Math.ceil(percent);
    }

    public List<LogEntry> getLogs() {
        return Optional.ofNullable(logs).map(l -> l.stream().toList()).orElse(Collections.emptyList());
    }
}

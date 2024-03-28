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
    private List<RemoteReindexIndex> indices = new ArrayList<>();

    private final Queue<LogEntry> logs = new CircularFifoQueue<>(50);

    @JsonProperty("error")
    private String error;

    private RemoteReindexMigration(@NotNull String migrationID) {
        this.id = migrationID;
    }

    public RemoteReindexMigration() {
        this(UUID.randomUUID().toString());
    }

    public static RemoteReindexMigration nonExistent(String migrationID) {
        return new RemoteReindexMigration(migrationID);
    }

    public RemoteReindexMigration setIndices(List<RemoteReindexIndex> indices) {
        this.indices = indices;
        return this;
    }

    public RemoteReindexMigration error(String message) {
        this.error = message;
        return this;
    }

    @JsonProperty("indices")
    public List<RemoteReindexIndex> indices() {
        return indices;
    }

    public Optional<RemoteReindexIndex> indexByName(String name) {
        return indices.stream().filter(i -> Objects.equals(i.getName(), name)).findFirst();
    }

    public String id() {
        return id;
    }

    @JsonProperty("status")
    public Status status() {
        if (indices.isEmpty()) {
            return Status.NOT_STARTED;
        }
        if (indices().stream().map(RemoteReindexIndex::getStatus).anyMatch(i -> i == Status.RUNNING)) {
            return Status.RUNNING;
        }
        if (indices().stream().map(RemoteReindexIndex::getStatus).anyMatch(i -> i == Status.ERROR)) {
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

        final int done = (int) indices.stream().map(RemoteReindexIndex::getStatus).filter(i -> i == Status.FINISHED || i == Status.ERROR).count();
        float percent = (100.0f / countOfIndices) * done;
        return (int) Math.ceil(percent);
    }

    public void log(LogEntry log) {
        this.logs.offer(log);
    }

    public List<LogEntry> getLogs() {
        return logs.stream().toList();
    }
}

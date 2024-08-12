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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.jsonwebtoken.lang.Collections;
import jakarta.validation.constraints.NotNull;
import org.graylog2.indexer.datanode.RemoteReindexingMigrationAdapter.Status;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Caution: this object will be heavily mutated from outside as the migration progresses.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RemoteReindexMigration {

    @JsonProperty("id")
    private final String id;
    private final List<RemoteReindexIndex> indices;

    private final List<LogEntry> logs;

    @JsonProperty("error")
    private String error;

    @JsonCreator
    public RemoteReindexMigration(@JsonProperty("id") @NotNull String migrationID, @JsonProperty("indices") List<RemoteReindexIndex> indices, @JsonProperty("logs") List<LogEntry> logs) {
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
        if (indices.isEmpty() || indices.stream().allMatch(i -> i.status() == Status.NOT_STARTED)) {
            return Status.NOT_STARTED;
        } else if (indices.stream().allMatch(RemoteReindexIndex::isCompleted)) {
            // all are now completed, either finished or errored
            if (indices.stream().anyMatch(i -> i.status() == Status.ERROR)) {
                return Status.ERROR;
            } else {
                return Status.FINISHED;
            }
        } else {
            return Status.RUNNING;
        }
    }

    /**
     * @return How much of the migration is done, in percent, int value between 0 a 100.
     */
    @JsonProperty("progress")
    public int progress() {
        if (indices.isEmpty()) {
            return 100; // avoid division by zero. No indices == migration is immediately done
        }

        final BigDecimal sum = indices.stream()
                .filter(i -> i.progress() != null)
                .map(RemoteReindexMigration::indexProgress)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return sum.divide(BigDecimal.valueOf(indices.size()), 4, RoundingMode.HALF_UP).scaleByPowerOfTen(2).intValue();
    }

    /**
     * @return value between 0 and 1, representing how much percent of the index migration is completed
     */
    private static BigDecimal indexProgress(RemoteReindexIndex i) {
        if (i.isCompleted()) { // no matter if success or error, if the index task is completed, the progress is 100%
            return BigDecimal.ONE;
        } else {
            return i.progress().progress();
        }
    }

    public List<LogEntry> getLogs() {
        return Optional.ofNullable(logs).map(l -> l.stream().toList()).orElse(Collections.emptyList());
    }

    @JsonProperty("tasks_progress")
    public Map<String, Integer> getTasksProgress() {
        return indices.stream()
                .filter(i -> i.status() == Status.RUNNING)
                .filter(i -> i.progress() != null)
                .sorted(Comparator.comparing(RemoteReindexIndex::name))
                .collect(Collectors.toMap(RemoteReindexIndex::name, i -> i.progress().progressPercent(), (integer, integer2) -> integer, LinkedHashMap::new));
    }
}

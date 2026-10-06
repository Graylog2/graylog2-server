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
package org.graylog2.datanode.restart;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import jakarta.annotation.Nullable;
import org.graylog2.jackson.MongoInstantDeserializer;
import org.graylog2.jackson.MongoInstantSerializer;

import java.time.Instant;

public record RollingRestartNodeEntry(
        @JsonProperty(FIELD_HOSTNAME) String hostname,
        @JsonProperty(FIELD_DATANODE_ID) String datanodeId,
        @JsonProperty(FIELD_STATUS) RollingRestartNodeStatus status,
        @Nullable
        @JsonSerialize(using = MongoInstantSerializer.class)
        @JsonDeserialize(using = MongoInstantDeserializer.class)
        @JsonProperty(FIELD_STARTED_AT) Instant startedAt,
        @Nullable
        @JsonSerialize(using = MongoInstantSerializer.class)
        @JsonDeserialize(using = MongoInstantDeserializer.class)
        @JsonProperty(FIELD_FINISHED_AT) Instant finishedAt,
        @JsonProperty(FIELD_ATTEMPTS) int attempts,
        @Nullable @JsonProperty(FIELD_LAST_ERROR) String lastError
) {
    public static final String FIELD_HOSTNAME = "hostname";
    public static final String FIELD_DATANODE_ID = "datanode_id";
    public static final String FIELD_STATUS = "status";
    public static final String FIELD_STARTED_AT = "started_at";
    public static final String FIELD_FINISHED_AT = "finished_at";
    public static final String FIELD_ATTEMPTS = "attempts";
    public static final String FIELD_LAST_ERROR = "last_error";

    public static RollingRestartNodeEntry pending(String hostname, String datanodeId) {
        return new RollingRestartNodeEntry(hostname, datanodeId, RollingRestartNodeStatus.PENDING, null, null, 0, null);
    }

    public RollingRestartNodeEntry withStatus(RollingRestartNodeStatus newStatus) {
        return new RollingRestartNodeEntry(hostname, datanodeId, newStatus, startedAt, finishedAt, attempts, lastError);
    }

    public RollingRestartNodeEntry withStarted(Instant at) {
        return new RollingRestartNodeEntry(hostname, datanodeId, status, at, finishedAt, attempts, lastError);
    }

    public RollingRestartNodeEntry withFinished(Instant at) {
        return new RollingRestartNodeEntry(hostname, datanodeId, status, startedAt, at, attempts, lastError);
    }

    public RollingRestartNodeEntry withError(String error) {
        return new RollingRestartNodeEntry(hostname, datanodeId, status, startedAt, finishedAt, attempts + 1, error);
    }
}

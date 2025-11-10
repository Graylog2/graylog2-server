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

@JsonAutoDetect
@AutoValue
public abstract class ClusterHealth {
    @JsonProperty("status")
    public abstract String status();

    @JsonProperty("shards")
    public abstract ShardStatus shards();

    @JsonCreator
    public static ClusterHealth create(@JsonProperty("status") String status,
                                       @JsonProperty("shards") ShardStatus shards) {
        return new AutoValue_ClusterHealth(status, shards);
    }

    @JsonAutoDetect
    @AutoValue
    public static abstract class ShardStatus {
        @JsonProperty("active")
        public abstract int active();

        @JsonProperty("initializing")
        public abstract int initializing();

        @JsonProperty("relocating")
        public abstract int relocating();

        @JsonProperty("unassigned")
        public abstract int unassigned();

        @JsonCreator
        public static ShardStatus create(@JsonProperty("active") int active,
                                         @JsonProperty("initializing") int initializing,
                                         @JsonProperty("relocating") int relocating,
                                         @JsonProperty("unassigned") int unassigned) {
            return new AutoValue_ClusterHealth_ShardStatus(active, initializing, relocating, unassigned);
        }
    }
}

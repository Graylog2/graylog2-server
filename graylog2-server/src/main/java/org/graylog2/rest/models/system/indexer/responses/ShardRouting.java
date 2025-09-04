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
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;

import javax.annotation.Nullable;

@JsonAutoDetect
@JsonDeserialize(builder = ShardRouting.Builder.class)
@AutoValue
public abstract class ShardRouting {
    @JsonProperty("id")
    public abstract int id();

    @JsonProperty("state")
    public abstract String state();

    @JsonProperty("active")
    public abstract boolean active();

    @JsonProperty("primary")
    public abstract boolean primary();

    @JsonProperty("node_id")
    public abstract String nodeId();

    @JsonProperty("node_name")
    @Nullable
    public abstract String nodeName();

    @JsonProperty("node_hostname")
    @Nullable
    public abstract String nodeHostname();

    @JsonProperty("relocating_to")
    @Nullable
    public abstract String relocatingTo();

    public static ShardRouting create(int id,
                                      String state,
                                      boolean active,
                                      boolean primary,
                                      String nodeId,
                                      @Nullable String nodeName,
                                      @Nullable String nodeHostname,
                                      @Nullable String relocatingTo) {
        return Builder.create()
                .id(id)
                .state(state)
                .active(active)
                .primary(primary)
                .nodeId(nodeId)
                .nodeName(nodeName)
                .nodeHostname(nodeHostname)
                .relocatingTo(relocatingTo)
                .build();
    }

    public static Builder builder() {
        return Builder.create();
    }

    public ShardRouting withNodeDetails(String nodeName, String nodeHostname) {
        return toBuilder()
                .nodeName(nodeName)
                .nodeHostname(nodeHostname)
                .build();
    }

    abstract Builder toBuilder();

    @AutoValue.Builder
    public abstract static class Builder {
        @JsonCreator
        public static Builder create() {
            return new AutoValue_ShardRouting.Builder();
        }

        @JsonProperty("id")
        public abstract Builder id(int id);

        @JsonProperty("state")
        public abstract Builder state(String state);

        @JsonProperty("active")
        public abstract Builder active(boolean active);

        @JsonProperty("primary")
        public abstract Builder primary(boolean primary);

        @JsonProperty("node_id")
        public abstract Builder nodeId(String nodeId);

        @JsonProperty("node_name")
        public abstract Builder nodeName(@Nullable String nodeName);

        @JsonProperty("node_hostname")
        public abstract Builder nodeHostname(@Nullable String nodeHostname);

        @JsonProperty("relocating_to")
        public abstract Builder relocatingTo(@Nullable String relocatingTo);

        abstract ShardRouting build();
    }
}

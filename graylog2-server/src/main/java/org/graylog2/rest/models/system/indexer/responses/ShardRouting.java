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
import org.graylog.autovalue.WithBeanGetter;

import javax.annotation.Nullable;

@JsonAutoDetect
@AutoValue
@WithBeanGetter
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

    @JsonCreator
    public static ShardRouting create(@JsonProperty("id") int id,
                                      @JsonProperty("state") String state,
                                      @JsonProperty("active") boolean active,
                                      @JsonProperty("primary") boolean primary,
                                      @JsonProperty("node_id") String nodeId,
                                      @JsonProperty("node_name") @Nullable String nodeName,
                                      @JsonProperty("node_hostname") @Nullable String nodeHostname,
                                      @JsonProperty("relocating_to") @Nullable String relocatingTo) {
        return new AutoValue_ShardRouting(id, state, active, primary, nodeId, nodeName, nodeHostname, relocatingTo);
    }

    public static Builder builder() {
        return new AutoValue_ShardRouting.Builder();
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
        public abstract Builder id(int id);

        public abstract Builder state(String state);

        public abstract Builder active(boolean active);

        public abstract Builder primary(boolean primary);

        public abstract Builder nodeId(String nodeId);

        public abstract Builder nodeName(String nodeName);

        public abstract Builder nodeHostname(String nodeHostname);

        public abstract Builder relocatingTo(String relocatingTo);

        abstract ShardRouting build();
    }
}

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
package org.graylog.storage.elasticsearch7.errors;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;


@AutoValue
@JsonDeserialize(builder = FailedShard.Builder.class)
public abstract class FailedShard {
    public abstract int shard();
    public abstract String index();
    public abstract String node();
    public abstract Cause reason();

    @AutoValue.Builder
    public abstract static class Builder {
        @JsonProperty
        public abstract Builder shard(final int shard);
        @JsonProperty
        public abstract Builder index(final String index);
        @JsonProperty
        public abstract Builder node(final String node);
        @JsonProperty
        public abstract Builder reason(final Cause reason);
        @JsonProperty
        public abstract FailedShard build();

        @JsonCreator
        public static FailedShard.Builder builder() {
            return new AutoValue_FailedShard.Builder();
        }
    }
}

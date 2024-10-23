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
package org.graylog2.system.traffic;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import org.graylog2.database.MongoEntity;
import org.joda.time.DateTime;
import org.mongojack.Id;
import org.mongojack.ObjectId;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Map;

@AutoValue
@JsonDeserialize(builder = AutoValue_TrafficDto.Builder.class)
public abstract class TrafficDto implements MongoEntity {
    @Id
    @ObjectId
    @Nullable
    @Override
    @JsonProperty("_id")
    public abstract String id();

    @JsonProperty
    public abstract DateTime bucket();

    @JsonProperty
    public abstract Map<String, Long> input();

    @JsonProperty
    public abstract Map<String, Long> output();

    @JsonProperty
    public abstract Map<String, Long> decoded();

    public abstract Builder toBuilder();

    public static Builder builder() {
        return new AutoValue_TrafficDto.Builder().decoded(Collections.emptyMap());
    }

    @AutoValue.Builder
    public abstract static class Builder {

        @Id
        @ObjectId
        @JsonProperty
        public abstract Builder id(String id);

        @JsonProperty
        public abstract Builder bucket(DateTime bucket);

        @JsonProperty
        public abstract Builder input(Map<String, Long> inputTraffic);

        @JsonProperty
        public abstract Builder output(Map<String, Long> outputTraffic);

        @JsonProperty
        public abstract Builder decoded(Map<String, Long> decodedTraffic);

        public abstract TrafficDto build();
    }
}

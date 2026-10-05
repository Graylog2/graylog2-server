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
package org.graylog.collectors.db;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.auto.value.AutoValue;
import jakarta.annotation.Nullable;
import org.graylog2.jackson.MongoInstantDeserializer;
import org.graylog2.jackson.MongoInstantSerializer;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

@AutoValue
@JsonDeserialize(builder = ComponentHealthDTO.Builder.class)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public abstract class ComponentHealthDTO {
    public static final String HEALTHY_FIELD = "healthy";

    @JsonProperty(HEALTHY_FIELD)
    public abstract boolean healthy();

    @JsonProperty("start_time")
    @JsonSerialize(contentUsing = MongoInstantSerializer.class)
    public abstract Optional<Instant> startTime();

    @JsonProperty("last_error")
    public abstract Optional<String> lastError();

    @JsonProperty("status")
    public abstract Optional<String> status();

    @JsonProperty("status_time")
    @JsonSerialize(contentUsing = MongoInstantSerializer.class)
    public abstract Optional<Instant> statusTime();

    @JsonProperty("components")
    public abstract Map<String, ComponentHealthDTO> components();

    public static Builder builder() {
        return Builder.create();
    }

    @AutoValue.Builder
    public abstract static class Builder {

        @JsonCreator
        public static Builder create() {
            return new AutoValue_ComponentHealthDTO.Builder()
                    .components(Collections.emptyMap());
        }

        @JsonProperty(HEALTHY_FIELD)
        public abstract Builder healthy(boolean healthy);

        @JsonProperty("start_time")
        @JsonDeserialize(using = MongoInstantDeserializer.class)
        public abstract Builder startTime(@Nullable Instant startTime);

        @JsonProperty("last_error")
        public abstract Builder lastError(@Nullable String lastError);

        @JsonProperty("status")
        public abstract Builder status(@Nullable String status);

        @JsonProperty("status_time")
        @JsonDeserialize(using = MongoInstantDeserializer.class)
        public abstract Builder statusTime(@Nullable Instant statusTime);

        @JsonProperty("components")
        public abstract Builder components(Map<String, ComponentHealthDTO> components);

        public abstract ComponentHealthDTO build();
    }
}

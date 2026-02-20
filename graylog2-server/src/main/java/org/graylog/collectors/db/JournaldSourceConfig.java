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
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import jakarta.annotation.Nullable;
import org.graylog.collectors.config.OtlpReceiverConfig;

import java.util.Optional;

@AutoValue
@JsonTypeName(JournaldSourceConfig.TYPE_NAME)
@JsonDeserialize(builder = JournaldSourceConfig.Builder.class)
public abstract class JournaldSourceConfig implements SourceConfig {
    public static final String TYPE_NAME = "journald";

    @Override
    @JsonProperty(TYPE_FIELD)
    public abstract String type();

    @JsonProperty("priority")
    public abstract int priority();

    @Nullable
    @JsonProperty("match_pattern")
    public abstract String matchPattern();

    public static Builder builder() {
        return Builder.create();
    }

    @Override
    public void validate() {
        if (priority() < 0 || priority() > 7) {
            throw new IllegalArgumentException("JournaldSourceConfig priority must be between 0 and 7");
        }
    }

    @Override
    public Optional<OtlpReceiverConfig> toReceiverConfig(String id) {
        return Optional.empty();
    }

    @AutoValue.Builder
    public abstract static class Builder {
        @JsonCreator
        public static Builder create() {
            return new AutoValue_JournaldSourceConfig.Builder().type(TYPE_NAME).priority(6);
        }

        @JsonProperty(TYPE_FIELD)
        public abstract Builder type(String type);

        @JsonProperty("priority")
        public abstract Builder priority(int priority);

        @JsonProperty("match_pattern")
        public abstract Builder matchPattern(@Nullable String matchPattern);

        public abstract JournaldSourceConfig build();
    }
}

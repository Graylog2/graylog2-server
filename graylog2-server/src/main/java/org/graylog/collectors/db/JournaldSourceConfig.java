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
import org.graylog.collectors.config.receiver.CollectorReceiverConfig;
import org.graylog.collectors.config.receiver.JournaldReceiverConfig;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

@AutoValue
@JsonTypeName(JournaldSourceConfig.TYPE_NAME)
@JsonDeserialize(builder = JournaldSourceConfig.Builder.class)
public abstract class JournaldSourceConfig implements SourceConfig {
    public static final String TYPE_NAME = "journald";

    @Override
    @JsonProperty(TYPE_FIELD)
    public abstract String type();

    @JsonProperty("read_mode")
    public abstract String readMode();

    @JsonProperty("priority")
    public abstract String priority();

    @JsonProperty("match_pattern")
    public abstract Optional<String> matchPattern();

    public static Builder builder() {
        return Builder.create();
    }

    @Override
    public void validate() {
        try {
            JournaldReceiverConfig.Priority.valueOf(priority().toUpperCase(Locale.ROOT));
            JournaldReceiverConfig.StartAt.valueOf(readMode().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("JournaldSourceConfig: " + e.getMessage());
        }
    }

    @Override
    public Optional<CollectorReceiverConfig> toReceiverConfig(String id) {
        return Optional.of(JournaldReceiverConfig.builder(id)
                .startAt(JournaldReceiverConfig.StartAt.valueOf(readMode().toUpperCase(Locale.ROOT)))
                .priority(JournaldReceiverConfig.Priority.valueOf(priority().toUpperCase(Locale.ROOT)))
                .matches(matchPattern().map(List::of).orElse(null))
                .build());
    }

    @AutoValue.Builder
    public abstract static class Builder {
        @JsonCreator
        public static Builder create() {
            return new AutoValue_JournaldSourceConfig.Builder()
                    .type(TYPE_NAME)
                    .priority("INFO")
                    .readMode("end");
        }

        @JsonProperty(TYPE_FIELD)
        public abstract Builder type(String type);

        @JsonProperty("read_mode")
        public abstract Builder readMode(String readMode);

        @JsonProperty("priority")
        public abstract Builder priority(String priority);

        @JsonProperty("match_pattern")
        public abstract Builder matchPattern(@Nullable String matchPattern);

        public abstract JournaldSourceConfig build();
    }
}

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

import java.util.List;
import java.util.Optional;

@AutoValue
@JsonTypeName(WindowsEventLogSourceConfig.TYPE_NAME)
@JsonDeserialize(builder = WindowsEventLogSourceConfig.Builder.class)
public abstract class WindowsEventLogSourceConfig implements SourceConfig {
    public static final String TYPE_NAME = "windows_event_log";

    @Override
    @JsonProperty(TYPE_FIELD)
    public abstract String type();

    @JsonProperty("channels")
    public abstract List<String> channels();

    @JsonProperty("read_mode")
    public abstract String readMode();

    @JsonProperty("event_format")
    public abstract String eventFormat();

    @Nullable
    @JsonProperty("query")
    public abstract String query();

    public static Builder builder() {
        return Builder.create();
    }

    @Override
    public void validate() {
        if (channels() == null || channels().isEmpty()) {
            throw new IllegalArgumentException("WindowsEventLogSourceConfig requires at least one channel");
        }
        if (readMode() == null || readMode().isBlank()) {
            throw new IllegalArgumentException("WindowsEventLogSourceConfig requires a non-blank read_mode");
        }
        if (eventFormat() == null || eventFormat().isBlank()) {
            throw new IllegalArgumentException("WindowsEventLogSourceConfig requires a non-blank event_format");
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
            return new AutoValue_WindowsEventLogSourceConfig.Builder()
                    .type(TYPE_NAME)
                    .readMode("end")
                    .eventFormat("json");
        }

        @JsonProperty(TYPE_FIELD)
        public abstract Builder type(String type);

        @JsonProperty("channels")
        public abstract Builder channels(List<String> channels);

        @JsonProperty("read_mode")
        public abstract Builder readMode(String readMode);

        @JsonProperty("event_format")
        public abstract Builder eventFormat(String eventFormat);

        @JsonProperty("query")
        public abstract Builder query(@Nullable String query);

        public abstract WindowsEventLogSourceConfig build();
    }
}

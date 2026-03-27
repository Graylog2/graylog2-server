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
import org.graylog.collectors.config.receiver.CollectorReceiverConfig;
import org.graylog.collectors.config.receiver.WindowsEventLogReceiverConfig;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static org.apache.commons.lang3.StringUtils.isBlank;

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

    @JsonProperty("include_default_channels")
    public abstract boolean includeDefaultChannels();

    @JsonProperty("read_mode")
    public abstract String readMode();

    public static Builder builder() {
        return Builder.create();
    }

    @Override
    public void validate() {
        if (isBlank(readMode())) {
            throw new IllegalArgumentException("WindowsEventLogSourceConfig requires a non-blank read_mode");
        }
        try {
            WindowsEventLogReceiverConfig.StartAt.valueOf(readMode().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("WindowsEventLogSourceConfig: " + e.getMessage());
        }
    }

    @Override
    public Optional<CollectorReceiverConfig> toReceiverConfig(String id) {
        return Optional.of(WindowsEventLogReceiverConfig.builder(id)
                .channels(channels())
                .includeDefaultChannels(includeDefaultChannels())
                .startAt(WindowsEventLogReceiverConfig.StartAt.valueOf(readMode().toUpperCase(Locale.ROOT)))
                .build());
    }

    @AutoValue.Builder
    public abstract static class Builder {
        @JsonCreator
        public static Builder create() {
            return new AutoValue_WindowsEventLogSourceConfig.Builder()
                    .type(TYPE_NAME)
                    .channels(List.of())
                    .includeDefaultChannels(true)
                    .readMode("end");
        }

        @JsonProperty(TYPE_FIELD)
        public abstract Builder type(String type);

        @JsonProperty("channels")
        public abstract Builder channels(List<String> channels);

        @JsonProperty("include_default_channels")
        public abstract Builder includeDefaultChannels(boolean includeDefaultChannels);

        @JsonProperty("read_mode")
        public abstract Builder readMode(String readMode);

        public abstract WindowsEventLogSourceConfig build();
    }
}

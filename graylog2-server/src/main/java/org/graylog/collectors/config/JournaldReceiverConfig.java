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
package org.graylog.collectors.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import jakarta.annotation.Nullable;

import java.util.List;

/**
 * Otel collector journald receiver configuration.
 *
 * @see <a href="https://github.com/open-telemetry/opentelemetry-collector-contrib/tree/main/receiver/journaldreceiver">journald receiver</a>
 */
@AutoValue
@JsonInclude(JsonInclude.Include.NON_NULL)
public abstract class JournaldReceiverConfig {

    @Nullable
    @JsonProperty("priority")
    public abstract String priority();

    @Nullable
    @JsonProperty("matches")
    public abstract List<String> matches();

    @Nullable
    @JsonProperty("start_at")
    public abstract String startAt();

    public static Builder builder() {
        return new AutoValue_JournaldReceiverConfig.Builder();
    }

    @AutoValue.Builder
    public abstract static class Builder {

        public abstract Builder priority(@Nullable String priority);

        public abstract Builder matches(@Nullable List<String> matches);

        public abstract Builder startAt(@Nullable String startAt);

        public abstract JournaldReceiverConfig build();
    }
}

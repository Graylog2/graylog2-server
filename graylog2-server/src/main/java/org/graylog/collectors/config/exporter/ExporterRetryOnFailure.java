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
package org.graylog.collectors.config.exporter;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.auto.value.AutoValue;
import org.graylog.collectors.config.GoDurationSerializer;

import java.time.Duration;

/**
 * @see <a href="https://github.com/open-telemetry/opentelemetry-collector/blob/main/exporter/exporterhelper/README.md">exporterhelper</a>
 */
@AutoValue
@JsonInclude(JsonInclude.Include.NON_ABSENT)
public abstract class ExporterRetryOnFailure {
    @JsonProperty("enabled")
    public abstract boolean enabled();

    @JsonProperty("initial_interval")
    @JsonSerialize(using = GoDurationSerializer.class)
    public abstract Duration initialInterval();

    @JsonProperty("max_interval")
    @JsonSerialize(using = GoDurationSerializer.class)
    public abstract Duration maxInterval();

    @JsonProperty("max_elapsed_time")
    @JsonSerialize(using = GoDurationSerializer.class)
    public abstract Duration maxElapsedTime();

    @JsonProperty("multiplier")
    public abstract float multiplier();

    public static Builder builder() {
        return new AutoValue_ExporterRetryOnFailure.Builder()
                .enabled(true)
                .initialInterval(Duration.ofSeconds(5))
                .maxInterval(Duration.ofSeconds(30))
                // TODO: Is it okay to never stop retries in the OTLP exporters?
                .maxElapsedTime(Duration.ZERO) // Never stop retries
                .multiplier(1.5f);
    }

    public abstract Builder toBuilder();

    public static ExporterRetryOnFailure createDefault() {
        return builder().build();
    }

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder enabled(boolean enabled);

        public abstract Builder initialInterval(Duration initialInterval);

        public abstract Builder maxInterval(Duration maxInterval);

        public abstract Builder maxElapsedTime(Duration maxElapsedTime);

        public abstract Builder multiplier(float multiplier);

        public abstract ExporterRetryOnFailure build();
    }
}

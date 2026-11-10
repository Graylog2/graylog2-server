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
import com.google.common.base.Strings;
import jakarta.annotation.Nullable;
import org.graylog.collectors.CollectorOSType;
import org.graylog.collectors.config.receiver.CollectorReceiverConfig;
import org.graylog.collectors.config.receiver.MacOSUnifiedLoggingReceiverConfig;

import java.time.Duration;
import java.util.Optional;

@AutoValue
@JsonTypeName(MacOSUnifiedLoggingSourceConfig.TYPE_NAME)
@JsonDeserialize(builder = MacOSUnifiedLoggingSourceConfig.Builder.class)
public abstract class MacOSUnifiedLoggingSourceConfig implements SourceConfig {
    public static final String TYPE_NAME = "macos_unified_logging";

    @Override
    @JsonProperty(TYPE_FIELD)
    public abstract String type();

    @Nullable
    @JsonProperty("predicate")
    public abstract String predicate();

    @JsonProperty("max_poll_interval")
    public abstract Duration maxPollInterval();

    @JsonProperty("max_log_age")
    public abstract Duration maxLogAge();

    public static Builder builder() {
        return Builder.create();
    }

    @Override
    public void validate() {
        // empty predicates are valid
        if (maxPollInterval().isZero() || maxPollInterval().isNegative()) {
            throw new IllegalArgumentException("max_poll_interval must be positive");
        }
        if (maxLogAge().isNegative()) {
            throw new IllegalArgumentException("max_log_age must be zero or positive");
        }
    }

    @Override
    public Optional<CollectorReceiverConfig> toReceiverConfig(String id, CollectorOSType osType) {
        final var builder = MacOSUnifiedLoggingReceiverConfig.builder(id)
                .maxLogAge(maxLogAge())
                .maxPollInterval(maxPollInterval());
        if (predicate() != null) {
            builder.predicate(Strings.emptyToNull(predicate()));
        }
        return Optional.of(builder.build());
    }

    @AutoValue.Builder
    public abstract static class Builder {
        @JsonCreator
        public static Builder create() {
            return new AutoValue_MacOSUnifiedLoggingSourceConfig.Builder()
                    .type(TYPE_NAME);
        }

        @JsonProperty(TYPE_FIELD)
        public abstract Builder type(String type);

        @JsonProperty("predicate")
        public abstract Builder predicate(@Nullable String predicate);

        @JsonProperty("max_poll_interval")
        public abstract Builder maxPollInterval(Duration maxPollInterval);

        @JsonProperty("max_log_age")
        public abstract Builder maxLogAge(Duration maxLogAge);

        public abstract MacOSUnifiedLoggingSourceConfig build();
    }
}

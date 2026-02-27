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
import org.graylog.collectors.config.MacOSUnifiedLoggingReceiverConfig;
import org.graylog.collectors.config.OtlpReceiverConfig;

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

    public static Builder builder() {
        return Builder.create();
    }

    @Override
    public void validate() {
        // No required fields — predicate is optional, defaults are sensible
    }

    @Override
    public Optional<OtlpReceiverConfig> toReceiverConfig(String id) {
        final var builder = MacOSUnifiedLoggingReceiverConfig.builder(id)
                .predicate(predicate());
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

        public abstract MacOSUnifiedLoggingSourceConfig build();
    }
}

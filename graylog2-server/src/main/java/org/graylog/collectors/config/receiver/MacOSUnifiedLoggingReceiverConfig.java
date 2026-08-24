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
package org.graylog.collectors.config.receiver;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.auto.value.AutoValue;
import jakarta.annotation.Nullable;
import org.graylog.collectors.CollectorOSType;
import org.graylog.collectors.config.GoDurationSerializer;
import org.graylog.collectors.config.extension.FileStorageExtensionConfig;

import java.time.Duration;
import java.util.EnumSet;

import static org.graylog2.shared.utilities.StringUtils.f;

/**
 * OTel collector macOS Unified Logging Receiver configuration.
 * <p>
 * Graylog only models the live-collection subset of the upstream receiver's options. The upstream
 * {@code archive_path} (one-shot collection from a static {@code .logarchive}) and {@code end_time}
 * (a bounded, finite collection window) options are intentionally omitted: the collector is meant
 * for continuous live tailing, so those would only ever produce one-shot behavior.
 *
 * @see <a href="https://github.com/Graylog2/collector/tree/main/receiver/macosunifiedloggingreceiver">macOS Unified Logging Receiver</a>
 */
@AutoValue
@JsonInclude(JsonInclude.Include.NON_NULL)
public abstract class MacOSUnifiedLoggingReceiverConfig implements CollectorReceiverConfig {
    public static final String RECEIVER_TYPE = "macos_unified_logging";

    public String type() {
        return RECEIVER_TYPE;
    }

    @Override
    public EnumSet<CollectorOSType> osSupport() {
        return EnumSet.of(CollectorOSType.MACOS);
    }

    @Nullable
    @JsonProperty("predicate")
    public abstract String predicate();

    @JsonProperty("max_poll_interval")
    @JsonSerialize(using = GoDurationSerializer.class)
    public abstract Duration maxPollInterval();

    @JsonProperty("max_log_age")
    @JsonSerialize(using = GoDurationSerializer.class)
    public abstract Duration maxLogAge();

    @JsonProperty("format")
    public String format() {
        return "ndjson";
    }

    // Persists the live-mode forward cursor; required for cursor tracking and de-duplication.
    @JsonProperty("storage")
    public abstract String storage();


    public static Builder builder(String id) {
        return new AutoValue_MacOSUnifiedLoggingReceiverConfig.Builder()
                .name(f("macos_unified_logging/%s", id))
                .maxPollInterval(Duration.ofSeconds(30))
                .maxLogAge(Duration.ofHours(24))
                .storage(FileStorageExtensionConfig.defaultInstance().name());
    }

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder name(String name);

        public abstract Builder predicate(@Nullable String predicate);

        public abstract Builder maxPollInterval(Duration maxPollInterval);

        public abstract Builder maxLogAge(Duration maxLogAge);

        public abstract Builder storage(String storage);

        public abstract MacOSUnifiedLoggingReceiverConfig build();
    }
}

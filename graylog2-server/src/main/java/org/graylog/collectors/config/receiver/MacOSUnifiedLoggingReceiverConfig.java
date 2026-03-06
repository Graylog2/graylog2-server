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
import org.graylog.collectors.config.GoDurationSerializer;

import java.time.Duration;

import static org.graylog2.shared.utilities.StringUtils.f;

/**
 * Otel collector macOS Unified Logging Receiver configuration.
 *
 * @see <a href="https://github.com/open-telemetry/opentelemetry-collector-contrib/tree/main/receiver/macosunifiedloggingreceiver">macOS Unified Logging Receiver</a>
 */
@AutoValue
@JsonInclude(JsonInclude.Include.NON_NULL)
public abstract class MacOSUnifiedLoggingReceiverConfig implements CollectorReceiverConfig {
    public static final String RECEIVER_TYPE = "macosunifiedlogging";

    public String type() {
        return RECEIVER_TYPE;
    }

    @Nullable
    @JsonProperty("archive_path")
    public abstract String archivePath();

    @Nullable
    @JsonProperty("predicate")
    public abstract String predicate();

    @Nullable
    @JsonProperty("start_time")
    public abstract String startTime();

    @Nullable
    @JsonProperty("end_time")
    public abstract String endTime();

    @Nullable
    @JsonProperty("max_poll_interval")
    @JsonSerialize(using = GoDurationSerializer.class)
    public abstract Duration maxPollInterval();

    @Nullable
    @JsonProperty("max_log_age")
    @JsonSerialize(using = GoDurationSerializer.class)
    public abstract Duration maxLogAge();

    @JsonProperty("format")
    public String format() {
        return "ndjson";
    }

    // TODO: Expose this default via the source config REST API so the frontend can fetch it
    //  instead of duplicating it. This should be the single source of truth.
    // Default predicate: security-relevant subsystems, error severity and above.
    // Subsystems: authentication, authorization, firewall, permissions, disk,
    //             endpoint security, system policy, process lifecycle.
    static final String DEFAULT_PREDICATE = "subsystem IN {"
            + "'com.apple.opendirectoryd',"
            + "'com.apple.authorization',"
            + "'com.apple.loginwindow',"
            + "'com.apple.securityd',"
            + "'com.apple.TCC',"
            + "'com.apple.alf',"
            + "'com.apple.networkextension',"
            + "'com.apple.DiskManagement',"
            + "'com.apple.CoreStorage',"
            + "'com.apple.endpointsecurity',"
            + "'com.apple.syspolicyd',"
            + "'com.apple.launchd'"
            + "} AND messageType >= error";

    public static Builder builder(String id) {
        return new AutoValue_MacOSUnifiedLoggingReceiverConfig.Builder()
                .name(f("macosunifiedlogging/%s", id))
                .predicate(DEFAULT_PREDICATE)
                .maxPollInterval(Duration.ofSeconds(30))
                .maxLogAge(Duration.ofHours(24));
    }

    @AutoValue.Builder
    public abstract static class Builder {

        public abstract Builder name(String name);

        public abstract Builder archivePath(@Nullable String archivePath);

        public abstract Builder predicate(@Nullable String predicate);

        public abstract Builder startTime(@Nullable String startTime);

        public abstract Builder endTime(@Nullable String endTime);

        public abstract Builder maxPollInterval(@Nullable Duration maxPollInterval);

        public abstract Builder maxLogAge(@Nullable Duration maxLogAge);

        public abstract MacOSUnifiedLoggingReceiverConfig build();
    }
}

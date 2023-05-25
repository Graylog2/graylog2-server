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
package org.graylog2.configuration;

import com.github.joschi.jadconfig.Parameter;
import com.github.joschi.jadconfig.ValidationException;
import com.github.joschi.jadconfig.Validator;
import org.graylog2.configuration.converters.JavaDurationConverter;

import javax.annotation.Nullable;
import java.time.Duration;
import java.util.Map;

public class TelemetryConfiguration {

    public static final String TELEMETRY_ENABLED = "telemetry_enabled";
    public static final String TELEMETRY_CLUSTER_INFO_TTL = "telemetry_cluster_info_ttl";

    @Parameter(value = "telemetry_api_key", required = true)
    private String telemetryApiKey = "phc_t3lgBB66QsPW4HEfiGopO14um4XGNtBcefEKYWelWda";

    @Parameter(value = "telemetry_host")
    private String telemetryApiHost = "https://telemetry.graylog.cloud";

    @Parameter(value = TELEMETRY_ENABLED)
    private boolean telemetryEnabled = true;

    @Parameter(value = "telemetry_cluster_info_ttl", converter = JavaDurationConverter.class, validators = Minimum10MinuteValidator.class)
    private Duration telemetryClusterInfoTtl = Duration.ofMinutes(10);


    @Nullable
    public String getTelemetryApiKey() {
        return telemetryApiKey;
    }

    public String getTelemetryApiHost() {
        return telemetryApiHost;
    }

    public boolean isTelemetryEnabled() {
        return telemetryEnabled;
    }

    public Duration getTelemetryClusterInfoTtl() {
        return telemetryClusterInfoTtl;
    }

    public Map<String, ?> telemetryFrontendSettings() {
        return Map.of(
                "api_key", getTelemetryApiKey() != null ? getTelemetryApiKey() : "",
                "host", getTelemetryApiHost(),
                "enabled", isTelemetryEnabled()
        );
    }

    public static class Minimum10MinuteValidator implements Validator<Duration> {
        @Override
        public void validate(final String name, final Duration value) throws ValidationException {
            if (value != null && value.compareTo(Duration.ofMinutes(10)) < 0) {
                throw new ValidationException("Parameter " + name + " should be at least 10 minutes (found " + value + ")");
            }
        }
    }
}

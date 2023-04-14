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

import javax.annotation.Nullable;
import java.util.Map;

public class TelemetryConfiguration {

    public static final String TELEMETRY_ENABLED = "telemetry_enabled";

    @Parameter(value = "telemetry_api_key", required = true)
    private String telemetryApiKey = "phc_fmJsCXBb0sqPpUCAJ51C0sT933i8LHUT6Zqm4oCGuK7";

    @Parameter(value = "telemetry_host")
    private String telemetryApiHost = "https://telemetry.graylog.cloud";

    @Parameter(value = TELEMETRY_ENABLED)
    private boolean telemetryEnabled = true;


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

    public Map<String, ?> toMap() {
        return Map.of(
                "api_key", getTelemetryApiKey() != null ? getTelemetryApiKey() : "",
                "host", getTelemetryApiHost(),
                "enabled", isTelemetryEnabled()
        );
    }
}

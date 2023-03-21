package org.graylog2.configuration;

import com.github.joschi.jadconfig.Parameter;

import javax.annotation.Nullable;
import java.util.Map;

public class TelemetryConfiguration {

    @Parameter(value = "telemetry_api_key", required = true)
    private String telemetryApiKey;

    @Parameter(value = "telemetry_host")
    private String telemetryApiHost = "https://eu.posthog.com/";

    @Parameter(value = "telemetry_enabled")
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

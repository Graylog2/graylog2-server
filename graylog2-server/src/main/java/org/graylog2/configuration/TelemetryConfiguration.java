package org.graylog2.configuration;

import com.github.joschi.jadconfig.Parameter;

import javax.annotation.Nullable;
import java.util.Map;

public class TelemetryConfiguration {


    @Parameter(value = "api_key")
    private String telemetryApiKey;

    @Parameter(value = "host")
    private String telemetryApiHost = "https://app.posthog.com";

    @Parameter(value = "enabled")
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

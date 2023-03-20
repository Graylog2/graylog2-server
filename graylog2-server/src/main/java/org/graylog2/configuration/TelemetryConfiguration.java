package org.graylog2.configuration;

import com.github.joschi.jadconfig.Parameter;

import javax.annotation.Nullable;
import java.util.Map;

public class TelemetryConfiguration {


    @Parameter(value = "analytics_api_key")
    private String telemetryApiKey;

    @Parameter(value = "analytics_api_host")
    private String telemetryApiHost = "https://app.posthog.com";


    @Nullable
    public String getTelemetryApiKey() {
        return telemetryApiKey;
    }

    public String getTelemetryApiHost() {
        return telemetryApiHost;
    }

    public Map<String, ?> toMap() {
        return Map.of(
                "api_key", getTelemetryApiKey() != null ? getTelemetryApiKey() : "",
                "host", getTelemetryApiHost() != null ? getTelemetryApiHost() : ""
        );
    }
}

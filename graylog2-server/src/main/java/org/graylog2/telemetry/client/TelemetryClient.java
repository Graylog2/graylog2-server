package org.graylog2.telemetry.client;

import com.posthog.java.PostHog;
import jakarta.inject.Inject;
import org.graylog2.configuration.TelemetryConfiguration;

public class TelemetryClient {
    @Inject
    public TelemetryClient(TelemetryConfiguration telemetryConfiguration) {
        final var posthog = new PostHog.Builder(telemetryConfiguration.getTelemetryApiKey())
                .host(telemetryConfiguration.getTelemetryApiHost())
                .build();
    }
}

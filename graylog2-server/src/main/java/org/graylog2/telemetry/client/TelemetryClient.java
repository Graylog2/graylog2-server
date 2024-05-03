package org.graylog2.telemetry.client;

import com.posthog.java.PostHog;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.graylog2.configuration.TelemetryConfiguration;
import org.graylog2.telemetry.cluster.TelemetryClusterService;

import java.util.Map;

@Singleton
public class TelemetryClient {
    private final PostHog posthog;
    private final String clusterId;
    private final boolean isEnabled;

    @Inject
    public TelemetryClient(TelemetryConfiguration telemetryConfiguration, TelemetryClusterService telemetryClusterService) {
        this.isEnabled = telemetryConfiguration.isTelemetryEnabled();
        this.posthog = new PostHog.Builder(telemetryConfiguration.getTelemetryApiKey())
                .host(telemetryConfiguration.getTelemetryApiHost())
                .build();
        this.clusterId = telemetryClusterService.getClusterId();
    }

    public void capture(String eventType, Map<String, Object> event) {
        if (isEnabled) {
            this.posthog.capture(clusterId, eventType, event);
        }
    }

    public boolean isEnabled() {
        return isEnabled;
    }
}

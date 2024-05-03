package org.graylog2.telemetry.client;

import com.posthog.java.PostHog;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.graylog2.configuration.TelemetryConfiguration;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.plugin.cluster.ClusterId;

import java.util.Map;

@Singleton
public class TelemetryClient {
    private final PostHog posthog;
    private final ClusterId clusterId;
    private final boolean isEnabled;

    @Inject
    public TelemetryClient(TelemetryConfiguration telemetryConfiguration, ClusterConfigService clusterConfigService) {
        this.isEnabled = telemetryConfiguration.isTelemetryEnabled();
        this.posthog = new PostHog.Builder(telemetryConfiguration.getTelemetryApiKey())
                .host(telemetryConfiguration.getTelemetryApiHost())
                .build();
        this.clusterId = clusterConfigService.getOrDefault(ClusterId.class, ClusterId.create("UNKNOWN"));
    }

    public void capture(String eventType, Map<String, Object> event) {
        if (isEnabled) {
            this.posthog.capture(clusterId.clusterId(), eventType, event);
        }
    }
}

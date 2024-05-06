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

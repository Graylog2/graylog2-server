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

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import okhttp3.OkHttpClient;
import org.graylog2.configuration.TelemetryConfiguration;
import org.graylog2.telemetry.cluster.TelemetryClusterService;
import org.graylog2.telemetry.scheduler.TelemetryEvent;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;

import java.util.Map;

@Singleton
public class TelemetryClient {
    private final PosthogAPI posthog;
    private final String clusterId;
    private final boolean isEnabled;
    private final String apiKey;

    @Inject
    public TelemetryClient(TelemetryConfiguration telemetryConfiguration, TelemetryClusterService telemetryClusterService,
                           OkHttpClient okHttpClient, ObjectMapper objectMapper) {
        this.isEnabled = telemetryConfiguration.isTelemetryEnabled();
        this.apiKey = telemetryConfiguration.getTelemetryApiKey();
        this.posthog = new Retrofit.Builder()
                .baseUrl(telemetryConfiguration.getTelemetryApiHost())
                .addConverterFactory(JacksonConverterFactory.create(objectMapper))
                .client(okHttpClient)
                .build()
                .create(PosthogAPI.class);
        this.clusterId = telemetryClusterService.getClusterId();
    }

    public void capture(Map<String, TelemetryEvent> events) {
        if (isEnabled) {
            final var batch = events.entrySet()
                    .stream()
                    .map(entry -> PosthogAPI.Event.create(clusterId, entry.getKey(), entry.getValue().metrics()))
                    .toList();
            final var request = new PosthogAPI.BatchRequest(apiKey, batch);
            posthog.batchSend(request);
        }
    }

    public boolean isEnabled() {
        return isEnabled;
    }
}

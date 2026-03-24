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
package org.graylog.collectors;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoBuilder;
import jakarta.annotation.Nullable;

import java.time.Duration;

import static org.graylog2.shared.utilities.StringUtils.requireNonBlank;

public record CollectorsConfig(
        @JsonProperty("ca_cert_id") @Nullable String caCertId,
        @JsonProperty("signing_cert_id") @Nullable String signingCertId,
        @JsonProperty("token_signing_key") @Nullable TokenSigningKey tokenSigningKey,
        @JsonProperty("otlp_server_cert_id") @Nullable String otlpServerCertId,
        @JsonProperty("http") IngestEndpointConfig http,
        @JsonProperty("collector_offline_threshold") Duration collectorOfflineThreshold,
        @JsonProperty("collector_default_visibility_threshold") Duration collectorDefaultVisibilityThreshold,
        @JsonProperty("collector_expiration_threshold") Duration collectorExpirationThreshold,
        // TODO: Make certificate lifetime configurable in the UI - https://github.com/Graylog2/graylog2-server/issues/25407
        @JsonProperty("collector_cert_lifetime") Duration collectorCertLifetime,
        // TODO: Make heartbeat interval configurable in the UI - https://github.com/Graylog2/graylog2-server/issues/25408
        @JsonProperty("collector_heartbeat_interval") Duration collectorHeartbeatInterval
) {
    public static final int DEFAULT_HTTP_PORT = 14401;

    public static final Duration DEFAULT_OFFLINE_THRESHOLD = Duration.ofMinutes(5);
    public static final Duration DEFAULT_VISIBILITY_THRESHOLD = Duration.ofDays(1);
    public static final Duration DEFAULT_EXPIRATION_THRESHOLD = Duration.ofDays(7);
    private static final Duration DEFAULT_COLLECTOR_CERT_LIFETIME = Duration.ofDays(365);
    private static final Duration DEFAULT_COLLECTOR_HEARTBEAT_INTERVAL = Duration.ofSeconds(30);

    public static Builder createDefaultBuilder(String hostname) {
        requireNonBlank(hostname, "hostname can't be blank");

        return CollectorsConfig.builder()
                .http(new IngestEndpointConfig(true, hostname, DEFAULT_HTTP_PORT, null));
    }

    public static CollectorsConfig createDefault(String hostname) {
        return createDefaultBuilder(hostname).build();
    }

    public static Builder builder() {
        return new AutoBuilder_CollectorsConfig_Builder()
                .collectorOfflineThreshold(DEFAULT_OFFLINE_THRESHOLD)
                .collectorDefaultVisibilityThreshold(DEFAULT_VISIBILITY_THRESHOLD)
                .collectorExpirationThreshold(DEFAULT_EXPIRATION_THRESHOLD)
                .collectorCertLifetime(DEFAULT_COLLECTOR_CERT_LIFETIME)
                .collectorHeartbeatInterval(DEFAULT_COLLECTOR_HEARTBEAT_INTERVAL);
    }

    @AutoBuilder
    public interface Builder {
        Builder caCertId(String caCertId);

        Builder signingCertId(String signingCertId);

        Builder tokenSigningKey(TokenSigningKey tokenSigningKey);

        Builder otlpServerCertId(String otlpServerCertId);

        Builder http(IngestEndpointConfig http);

        Builder collectorOfflineThreshold(Duration collectorOfflineThreshold);

        Builder collectorDefaultVisibilityThreshold(Duration collectorDefaultVisibilityThreshold);

        Builder collectorExpirationThreshold(Duration collectorExpirationThreshold);

        Builder collectorCertLifetime(Duration collectorCertLifetime);

        Builder collectorHeartbeatInterval(Duration collectorHeartbeatInterval);

        CollectorsConfig build();
    }
}

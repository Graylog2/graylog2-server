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
import jakarta.annotation.Nullable;

import java.time.Duration;

public record CollectorsConfig(
        @JsonProperty("opamp_ca_id") @Nullable String opampCaId,
        @JsonProperty("token_signing_cert_id") @Nullable String tokenSigningCertId,
        @JsonProperty("otlp_server_cert_id") @Nullable String otlpServerCertId,
        @JsonProperty("http") IngestEndpointConfig http,
        @JsonProperty("grpc") IngestEndpointConfig grpc,
        @JsonProperty("collector_offline_threshold") Duration collectorOfflineThreshold,
        @JsonProperty("collector_default_visibility_threshold") Duration collectorDefaultVisibilityThreshold,
        @JsonProperty("collector_expiration_threshold") Duration collectorExpirationThreshold
) {
    public static final Duration DEFAULT_OFFLINE_THRESHOLD = Duration.ofMinutes(5);
    public static final Duration DEFAULT_VISIBILITY_THRESHOLD = Duration.ofDays(1);
    public static final Duration DEFAULT_EXPIRATION_THRESHOLD = Duration.ofDays(7);

    public static final CollectorsConfig DEFAULT = new CollectorsConfig(
            null, null, null,
            new IngestEndpointConfig(false, "localhost", 14401, null),
            new IngestEndpointConfig(false, "localhost", 14402, null),
            DEFAULT_OFFLINE_THRESHOLD,
            DEFAULT_VISIBILITY_THRESHOLD,
            DEFAULT_EXPIRATION_THRESHOLD);
}

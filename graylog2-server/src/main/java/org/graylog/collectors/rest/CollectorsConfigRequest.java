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
package org.graylog.collectors.rest;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;
import org.graylog.collectors.IngestEndpointConfig;

import java.time.Duration;

import static java.util.Objects.requireNonNull;

public record CollectorsConfigRequest(
        @JsonProperty("http") @NotNull IngestEndpointRequest http,
        @JsonProperty("collector_offline_threshold") @Nullable Duration collectorOfflineThreshold,
        @JsonProperty("collector_default_visibility_threshold") @Nullable Duration collectorDefaultVisibilityThreshold,
        @JsonProperty("collector_expiration_threshold") @Nullable Duration collectorExpirationThreshold,
        @JsonProperty("create_input") @Nullable Boolean createInput
) {
    public CollectorsConfigRequest {
        requireNonNull(http, "http must not be null");
    }

    public record IngestEndpointRequest(
            @JsonProperty("hostname") String hostname,
            @JsonProperty("port") int port
    ) {
        public IngestEndpointConfig toConfig() {
            return new IngestEndpointConfig(hostname(), port());
        }
    }
}

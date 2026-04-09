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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.github.joschi.jadconfig.util.Size;
import com.google.auto.value.AutoValue;
import com.google.common.primitives.Ints;
import jakarta.annotation.Nullable;

import java.time.Duration;

import static org.graylog2.shared.utilities.StringUtils.requireNonBlank;

@AutoValue
@JsonDeserialize(builder = CollectorsConfig.Builder.class)
public abstract class CollectorsConfig {
    public static final int DEFAULT_HTTP_PORT = 14401;

    public static final Duration DEFAULT_OFFLINE_THRESHOLD = Duration.ofMinutes(5);
    public static final Duration DEFAULT_VISIBILITY_THRESHOLD = Duration.ofDays(1);
    public static final Duration DEFAULT_EXPIRATION_THRESHOLD = Duration.ofDays(7);
    private static final Duration DEFAULT_COLLECTOR_CERT_LIFETIME = Duration.ofDays(365);
    private static final Duration DEFAULT_COLLECTOR_HEARTBEAT_INTERVAL = Duration.ofSeconds(30);
    private static final int DEFAULT_OPAMP_MAX_REQUEST_BODY_SIZE_BYTES = Ints.saturatedCast(Size.megabytes(10).toBytes());

    public static final String FIELD_CA_CERT_ID = "ca_cert_id";
    public static final String FIELD_SIGNING_CERT_ID = "signing_cert_id";
    public static final String FIELD_TOKEN_SIGNING_KEY = "token_signing_key";
    public static final String FIELD_OTLP_SERVER_CERT_ID = "otlp_server_cert_id";
    public static final String FIELD_HTTP = "http";
    public static final String FIELD_COLLECTOR_OFFLINE_THRESHOLD = "collector_offline_threshold";
    public static final String FIELD_COLLECTOR_DEFAULT_VISIBILITY_THRESHOLD = "collector_default_visibility_threshold";
    public static final String FIELD_COLLECTOR_EXPIRATION_THRESHOLD = "collector_expiration_threshold";
    public static final String FIELD_COLLECTOR_CERT_LIFETIME = "collector_cert_lifetime";
    public static final String FIELD_COLLECTOR_HEARTBEAT_INTERVAL = "collector_heartbeat_interval";
    public static final String FIELD_OPAMP_MAX_REQUEST_BODY_SIZE_BYTES = "opamp_max_request_body_size_bytes";

    @JsonProperty(FIELD_CA_CERT_ID)
    @Nullable
    public abstract String caCertId();

    @JsonProperty(FIELD_SIGNING_CERT_ID)
    @Nullable
    public abstract String signingCertId();

    @JsonProperty(FIELD_TOKEN_SIGNING_KEY)
    @Nullable
    public abstract TokenSigningKey tokenSigningKey();

    @JsonProperty(FIELD_OTLP_SERVER_CERT_ID)
    @Nullable
    public abstract String otlpServerCertId();

    @JsonProperty(FIELD_HTTP)
    public abstract IngestEndpointConfig http();

    @JsonProperty(FIELD_COLLECTOR_OFFLINE_THRESHOLD)
    public abstract Duration collectorOfflineThreshold();

    @JsonProperty(FIELD_COLLECTOR_DEFAULT_VISIBILITY_THRESHOLD)
    public abstract Duration collectorDefaultVisibilityThreshold();

    @JsonProperty(FIELD_COLLECTOR_EXPIRATION_THRESHOLD)
    public abstract Duration collectorExpirationThreshold();

    // TODO: Make certificate lifetime configurable in the UI - https://github.com/Graylog2/graylog2-server/issues/25407
    // TODO: Collector cert lifetime can't be higher than the signing cert's lifetime. Validate!
    @JsonProperty(FIELD_COLLECTOR_CERT_LIFETIME)
    public abstract Duration collectorCertLifetime();

    // TODO: Make heartbeat interval configurable in the UI - https://github.com/Graylog2/graylog2-server/issues/25408
    @JsonProperty(FIELD_COLLECTOR_HEARTBEAT_INTERVAL)
    public abstract Duration collectorHeartbeatInterval();

    @JsonProperty(FIELD_OPAMP_MAX_REQUEST_BODY_SIZE_BYTES)
    public abstract int opampMaxRequestBodySizeBytes();

    public static Builder createDefaultBuilder(String hostname) {
        requireNonBlank(hostname, "hostname can't be blank");

        return CollectorsConfig.builder()
                .http(new IngestEndpointConfig(true, hostname, DEFAULT_HTTP_PORT, null));
    }

    public static CollectorsConfig createDefault(String hostname) {
        return createDefaultBuilder(hostname).build();
    }

    public static Builder builder() {
        return Builder.create();
    }

    @AutoValue.Builder
    public static abstract class Builder {
        @JsonCreator
        public static Builder create() {
            return new AutoValue_CollectorsConfig.Builder()
                    .collectorOfflineThreshold(DEFAULT_OFFLINE_THRESHOLD)
                    .collectorDefaultVisibilityThreshold(DEFAULT_VISIBILITY_THRESHOLD)
                    .collectorExpirationThreshold(DEFAULT_EXPIRATION_THRESHOLD)
                    .collectorCertLifetime(DEFAULT_COLLECTOR_CERT_LIFETIME)
                    .collectorHeartbeatInterval(DEFAULT_COLLECTOR_HEARTBEAT_INTERVAL)
                    .opampMaxRequestBodySizeBytes(DEFAULT_OPAMP_MAX_REQUEST_BODY_SIZE_BYTES);
        }

        @JsonProperty(FIELD_CA_CERT_ID)
        public abstract Builder caCertId(String caCertId);

        @JsonProperty(FIELD_SIGNING_CERT_ID)
        public abstract Builder signingCertId(String signingCertId);

        @JsonProperty(FIELD_TOKEN_SIGNING_KEY)
        public abstract Builder tokenSigningKey(TokenSigningKey tokenSigningKey);

        @JsonProperty(FIELD_OTLP_SERVER_CERT_ID)
        public abstract Builder otlpServerCertId(String otlpServerCertId);

        @JsonProperty(FIELD_HTTP)
        public abstract Builder http(IngestEndpointConfig http);

        @JsonProperty(FIELD_COLLECTOR_OFFLINE_THRESHOLD)
        public abstract Builder collectorOfflineThreshold(Duration collectorOfflineThreshold);

        @JsonProperty(FIELD_COLLECTOR_DEFAULT_VISIBILITY_THRESHOLD)
        public abstract Builder collectorDefaultVisibilityThreshold(Duration collectorDefaultVisibilityThreshold);

        @JsonProperty(FIELD_COLLECTOR_EXPIRATION_THRESHOLD)
        public abstract Builder collectorExpirationThreshold(Duration collectorExpirationThreshold);

        @JsonProperty(FIELD_COLLECTOR_CERT_LIFETIME)
        public abstract Builder collectorCertLifetime(Duration collectorCertLifetime);

        @JsonProperty(FIELD_COLLECTOR_HEARTBEAT_INTERVAL)
        public abstract Builder collectorHeartbeatInterval(Duration collectorHeartbeatInterval);

        @JsonProperty(FIELD_OPAMP_MAX_REQUEST_BODY_SIZE_BYTES)
        public abstract Builder opampMaxRequestBodySizeBytes(int opampMaxRequestBodySizeBytes);

        public abstract CollectorsConfig build();
    }
}

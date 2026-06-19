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
package org.graylog.collectors.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import jakarta.annotation.Nullable;

import java.util.Optional;

/**
 * OpenTelemetry Collector TLS configuration settings. The settings can be used for multiple receivers and exporters.
 *
 * @see <a href="https://github.com/open-telemetry/opentelemetry-collector/blob/main/config/configtls/README.md">Documentation</a>
 */
@AutoValue
@JsonInclude(JsonInclude.Include.NON_ABSENT)
public abstract class TLSConfigurationSettings {
    /**
     * The Collector supervisor sets these environment variables in the Collector environment on start.
     * MUST NOT be changed!
     */
    private static final String ENV_GLC_INTERNAL_TLS_CLIENT_KEY_PATH = "${env:GLC_INTERNAL_TLS_CLIENT_KEY_PATH}";
    private static final String ENV_GLC_INTERNAL_TLS_CLIENT_CERT_PATH = "${env:GLC_INTERNAL_TLS_CLIENT_CERT_PATH}";
    private static final String DEFAULT_TLS_MIN_VERSION = "1.3";

    /**
     * Whether to enable client transport security for the exporter's HTTPs or gRPC connection. (i.e., disable TLS if true)
     */
    @JsonProperty("insecure")
    public abstract boolean insecure();

    @JsonProperty("insecure_skip_verify")
    public abstract boolean insecureSkipVerify();

    @JsonProperty("ca_pem")
    public abstract Optional<String> caPem();

    @JsonProperty("cert_file")
    public String certFile() {
        return ENV_GLC_INTERNAL_TLS_CLIENT_CERT_PATH;
    }

    @JsonProperty("key_file")
    public String keyFile() {
        return ENV_GLC_INTERNAL_TLS_CLIENT_KEY_PATH;
    }

    @JsonProperty("min_version")
    public abstract String minVersion();

    @JsonProperty("max_version")
    public abstract Optional<String> maxVersion();

    @JsonProperty("server_name_override")
    public abstract String serverNameOverride();

    public static Builder builder() {
        return new AutoValue_TLSConfigurationSettings.Builder()
                .minVersion(DEFAULT_TLS_MIN_VERSION)
                .insecureSkipVerify(false)
                .insecure(false);
    }

    public static TLSConfigurationSettings withCACert(String serverName, String caCert) {
        return builder().caPem(caCert).serverNameOverride(serverName).build();
    }

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder insecure(boolean insecure);

        public abstract Builder insecureSkipVerify(boolean insecureSkipVerify);

        public abstract Builder caPem(@Nullable String caPem);

        public abstract Builder minVersion(String minVersion);

        public abstract Builder maxVersion(@Nullable String maxVersion);

        public abstract Builder serverNameOverride(String serverNameOverride);

        public abstract TLSConfigurationSettings build();
    }
}

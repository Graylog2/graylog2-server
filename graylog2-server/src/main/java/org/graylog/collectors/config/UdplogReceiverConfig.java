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

/**
 * Otel collector udplog receiver configuration.
 *
 * @see <a href="https://github.com/open-telemetry/opentelemetry-collector-contrib/tree/main/receiver/udplogreceiver">udplog receiver</a>
 */
@AutoValue
@JsonInclude(JsonInclude.Include.NON_NULL)
public abstract class UdplogReceiverConfig {

    @JsonProperty("listen_address")
    public abstract String listenAddress();

    @Nullable
    @JsonProperty("multiline")
    public abstract OtelMultilineConfig multiline();

    public static Builder builder() {
        return new AutoValue_UdplogReceiverConfig.Builder();
    }

    @AutoValue.Builder
    public abstract static class Builder {

        public abstract Builder listenAddress(String listenAddress);

        public abstract Builder multiline(@Nullable OtelMultilineConfig multiline);

        public abstract UdplogReceiverConfig build();
    }
}

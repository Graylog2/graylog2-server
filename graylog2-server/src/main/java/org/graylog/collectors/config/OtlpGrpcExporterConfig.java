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

/**
 * Otel collector otlp (gRPC) exporter configuration.
 *
 * @see <a href="https://github.com/open-telemetry/opentelemetry-collector/tree/main/exporter/otlpexporter">otlp exporter</a>
 */
@AutoValue
@JsonInclude(JsonInclude.Include.NON_NULL)
public abstract class OtlpGrpcExporterConfig implements OtlpExporterConfig {

    public String getName() {
        return "otlp_grpc";
    }

    @JsonProperty("endpoint")
    public abstract String endpoint();

    public static Builder builder() {
        return new AutoValue_OtlpGrpcExporterConfig.Builder();
    }

    @AutoValue.Builder
    public abstract static class Builder {

        public abstract Builder endpoint(String endpoint);

        public abstract OtlpGrpcExporterConfig build();
    }
}

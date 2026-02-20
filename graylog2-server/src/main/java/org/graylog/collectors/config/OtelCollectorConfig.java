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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;

import java.util.List;
import java.util.Map;

/**
 * Representation of a collector config.
 *
 * This is used to programmatically assemble the otel-collector config.
 * The top-level keys match the otel-collector YAML structure: receivers, exporters, service.
 */
@AutoValue
public abstract class OtelCollectorConfig {

    @JsonProperty("receivers")
    public abstract Map<String, OtlpReceiverConfig> receivers();

    @JsonProperty("exporters")
    public abstract Map<String, OtlpExporterConfig> exporters();

    @JsonProperty("service")
    public abstract OtelServiceConfig service();

    public static Builder builder() {
        return new AutoValue_OtelCollectorConfig.Builder();
    }

    @AutoValue.Builder
    public abstract static class Builder {

        public abstract Builder receivers(Map<String, OtlpReceiverConfig> receivers);

        public abstract Builder exporters(Map<String, OtlpExporterConfig> exporters);

        public abstract Builder service(OtelServiceConfig service);

        public abstract OtelCollectorConfig build();
    }
}

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
import org.graylog.collectors.config.exporter.OtlpExporterConfig;
import org.graylog.collectors.config.processor.CollectorProcessorConfig;
import org.graylog.collectors.config.receiver.CollectorReceiverConfig;

import java.util.Map;

/**
 * Representation of a collector config.
 *
 * This is used to programmatically assemble the otel-collector config.
 * The top-level keys match the otel-collector YAML structure: receivers, exporters, service.
 */
@AutoValue
public abstract class CollectorConfig {

    // TODO: Add file_storage extension for persisting file offsets, and exporter sending queue
    // TODO: Add resourcedetection processor to add resource attributes like OS name, version, etc.

    @JsonProperty("receivers")
    public abstract Map<String, CollectorReceiverConfig> receivers();

    @JsonProperty("exporters")
    public abstract Map<String, OtlpExporterConfig> exporters();

    @JsonProperty("processors")
    public abstract Map<String, CollectorProcessorConfig> processors();

    @JsonProperty("service")
    public abstract CollectorServiceConfig service();

    public static Builder builder() {
        return new AutoValue_CollectorConfig.Builder();
    }

    @AutoValue.Builder
    public abstract static class Builder {

        public abstract Builder receivers(Map<String, CollectorReceiverConfig> receivers);

        public abstract Builder exporters(Map<String, OtlpExporterConfig> exporters);

        public abstract Builder processors(Map<String, CollectorProcessorConfig> processors);

        public abstract Builder service(CollectorServiceConfig service);

        public abstract CollectorConfig build();
    }
}

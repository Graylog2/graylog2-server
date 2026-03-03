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
package org.graylog.collectors.config.exporter;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.auto.value.AutoValue;

import java.time.Duration;

/**
 * OTel collector OTLP HTTP exporter configuration.
 *
 * @see <a href="https://github.com/open-telemetry/opentelemetry-collector/tree/main/exporter/otlphttpexporter">OTLP HTTP exporter</a>
 */
@AutoValue
@JsonInclude(JsonInclude.Include.NON_NULL)
public abstract class OtlpHttpExporterConfig implements OtlpExporterConfig {

    public String getName() {
        return "otlp_http";
    }

    public static Builder builder() {
        return new AutoValue_OtlpHttpExporterConfig.Builder()
                .timeout(Duration.ofSeconds(5))
                .retryOnFailure(ExporterRetryOnFailure.createDefault())
                .sendingQueue(ExporterSendingQueue.createDefault());
    }

    @AutoValue.Builder
    public abstract static class Builder implements OtlpExporterConfig.Builder<OtlpHttpExporterConfig, Builder> {
    }
}

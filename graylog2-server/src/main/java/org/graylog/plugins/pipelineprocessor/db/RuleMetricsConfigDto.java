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
package org.graylog.plugins.pipelineprocessor.db;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;

@AutoValue
@JsonDeserialize(builder = RuleMetricsConfigDto.Builder.class)
public abstract class RuleMetricsConfigDto {
    private static final String FIELD_METRICS_ENABLED = "metrics_enabled";

    @JsonProperty(FIELD_METRICS_ENABLED)
    public abstract boolean metricsEnabled();

    public static Builder builder() {
        return Builder.create();
    }

    public static RuleMetricsConfigDto createDefault() {
        return builder().build();
    }

    @AutoValue.Builder
    public abstract static class Builder {
        @JsonCreator
        public static Builder create() {
            return new AutoValue_RuleMetricsConfigDto.Builder().metricsEnabled(false);
        }

        @JsonProperty(FIELD_METRICS_ENABLED)
        public abstract Builder metricsEnabled(boolean metricsEnabled);

        public abstract RuleMetricsConfigDto build();
    }
}

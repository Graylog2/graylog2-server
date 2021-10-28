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
package org.graylog.metrics.prometheus.mapping;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import java.util.List;
import java.util.Map;

@AutoValue
public abstract class PrometheusMappingConfig {
    @JsonProperty("metric_mappings")
    public abstract List<MetricMapping> metricMappings();

    @JsonCreator
    public static PrometheusMappingConfig create(@JsonProperty("metric_mappings") List<MetricMapping> metricMappings) {
        return new AutoValue_PrometheusMappingConfig(metricMappings);
    }

    @AutoValue
    @JsonDeserialize(builder = PrometheusMappingConfig.MetricMapping.Builder.class)
    public static abstract class MetricMapping {
        @JsonProperty("metric_name")
        public abstract String metricName();

        @JsonProperty("match_pattern")
        public abstract String matchPattern();

        @JsonProperty("wildcard_extract_labels")
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        public abstract ImmutableList<String> wildcardExtractLabels();

        @JsonProperty("additional_labels")
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        public abstract ImmutableMap<String, String> additionalLabels();

        public static Builder builder() {
            return Builder.create();
        }

        @AutoValue.Builder
        public static abstract class Builder {
            @JsonCreator
            public static Builder create() {
                return new AutoValue_PrometheusMappingConfig_MetricMapping.Builder()
                        .wildcardExtractLabels(ImmutableList.of())
                        .additionalLabels(ImmutableMap.of());
            }

            @JsonProperty("metric_name")
            public abstract Builder metricName(String metricName);

            @JsonProperty("match_pattern")
            public abstract Builder matchPattern(String matchPattern);

            @JsonProperty("wildcard_extract_labels")
            public abstract Builder wildcardExtractLabels(List<String> wildcardExtractLabels);

            @JsonProperty("additional_labels")
            public abstract Builder additionalLabels(Map<String, String> additionalLabels);

            public abstract MetricMapping build();
        }
    }
}

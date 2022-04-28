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
import com.google.inject.assistedinject.Assisted;
import io.prometheus.client.dropwizard.samplebuilder.MapperConfig;
import org.graylog2.plugin.system.NodeId;

import javax.inject.Inject;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MetricMatchMapping implements MetricMapping {
    public static final String TYPE = "metric_match";

    private final NodeId nodeId;
    private final Config config;

    @Inject
    public MetricMatchMapping(NodeId nodeId, @Assisted MetricMapping.Config config) {
        this.nodeId = nodeId;
        this.config = (Config) config;
    }

    public interface Factory extends MetricMapping.Factory<MetricMatchMapping> {
        @Override
        MetricMatchMapping create(MetricMapping.Config config);
    }

    @Override
    public Set<MapperConfig> toMapperConfigs() {
        final Map<String, String> labels = new HashMap<>();

        // Add nodeId to every metric.
        labels.put("node", nodeId.toString());
        labels.putAll(config.additionalLabels());

        for (int i = 0; i < config.wildcardExtractLabels().size(); i++) {
            labels.put(config.wildcardExtractLabels().get(i), "${" + i + "}");
        }

        return Collections.singleton(
                new MapperConfig(config.matchPattern(), "gl_" + config.metricName(), labels));
    }

    @AutoValue
    @JsonDeserialize(builder = Config.Builder.class)
    public static abstract class Config implements MetricMapping.Config {

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
        public static abstract class Builder implements MetricMapping.Config.Builder<Builder> {
            @JsonCreator
            public static Builder create() {
                return new AutoValue_MetricMatchMapping_Config.Builder()
                        .type(TYPE)
                        .wildcardExtractLabels(ImmutableList.of())
                        .additionalLabels(ImmutableMap.of());
            }

            @JsonProperty("match_pattern")
            public abstract Builder matchPattern(String matchPattern);

            @JsonProperty("wildcard_extract_labels")
            public abstract Builder wildcardExtractLabels(List<String> wildcardExtractLabels);

            @JsonProperty("additional_labels")
            public abstract Builder additionalLabels(Map<String, String> additionalLabels);

            public abstract Config build();
        }
    }
}

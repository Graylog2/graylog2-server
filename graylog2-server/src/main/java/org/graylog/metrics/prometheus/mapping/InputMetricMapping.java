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
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableMap;
import com.google.inject.assistedinject.Assisted;
import io.prometheus.client.dropwizard.samplebuilder.MapperConfig;
import org.graylog2.inputs.InputService;
import org.graylog2.plugin.system.NodeId;

import javax.inject.Inject;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class InputMetricMapping implements MetricMapping {
    public static final String TYPE = "input_metric";

    private final InputService inputService;
    private final NodeId nodeId;
    private final Config config;

    @Inject
    public InputMetricMapping(InputService inputService, NodeId nodeId, @Assisted MetricMapping.Config config) {
        this.inputService = inputService;
        this.nodeId = nodeId;
        this.config = (Config) config;
    }

    public interface Factory extends MetricMapping.Factory<InputMetricMapping> {
        @Override
        InputMetricMapping create(MetricMapping.Config config);
    }

    @Override
    public Set<MapperConfig> toMapperConfigs() {
        return inputService.all()
                .stream()
                .map(input -> {
                    final String match = input.getType() + "." + input.getId() + "." + config.inputMetricName();
                    final Map<String, String> labels = ImmutableMap.of(
                            "node", nodeId.toString(),
                            "input_id", input.getId(),
                            "input_type", input.getType()
                    );
                    return new MapperConfig(match, "gl_" + config.metricName(), labels);
                })
                .collect(Collectors.toSet());
    }

    @AutoValue
    @JsonDeserialize(builder = Config.Builder.class)
    public static abstract class Config implements MetricMapping.Config {

        @JsonProperty("input_metric_name")
        public abstract String inputMetricName();

        public static Builder builder() {
            return Builder.create();
        }

        @AutoValue.Builder
        public static abstract class Builder implements MetricMapping.Config.Builder<Builder> {
            @JsonCreator
            public static Builder create() {
                return new AutoValue_InputMetricMapping_Config.Builder()
                        .type(TYPE);
            }

            @JsonProperty("input_metric_name")
            public abstract Builder inputMetricName(String inputMetricName);

            public abstract Config build();
        }
    }
}

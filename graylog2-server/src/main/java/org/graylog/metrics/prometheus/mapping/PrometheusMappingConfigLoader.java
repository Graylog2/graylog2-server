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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.prometheus.client.dropwizard.samplebuilder.MapperConfig;
import org.graylog2.plugin.system.NodeId;

import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class PrometheusMappingConfigLoader {
    private static final ObjectMapper YAML_MAPPER = new ObjectMapper(new YAMLFactory());

    private final NodeId nodeId;

    @Inject
    public PrometheusMappingConfigLoader(NodeId nodeId) {
        this.nodeId = nodeId;
    }


    public Set<MapperConfig> load(InputStream inputStream) throws IOException {
        final PrometheusMappingConfig config = YAML_MAPPER.readValue(inputStream, PrometheusMappingConfig.class);

        return config.metricMappings().stream()
                .map(this::mapMetric)
                .collect(Collectors.toSet());
    }

    private MapperConfig mapMetric(PrometheusMappingConfig.MetricMapping mapping) {
        final Map<String, String> labels = new HashMap<>();

        // Add nodeId to every metric.
        // TODO: Can Prometheus do this with some global label?
        labels.put("node", nodeId.toString());
        labels.putAll(mapping.additionalLabels());

        for (int i = 0; i < mapping.wildcardExtractLabels().size(); i++) {
            labels.put(mapping.wildcardExtractLabels().get(i), "${" + i + "}");
        }

        return new MapperConfig(mapping.matchPattern(), "gl_" + mapping.metricName(), labels);
    }
}

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
import org.graylog2.jackson.DeserializationProblemHandlerModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PrometheusMappingConfigLoader {
    private static final Logger log = LoggerFactory.getLogger(PrometheusMappingConfigLoader.class);

    private final ObjectMapper ymlMapper;
    private final Map<String, MetricMapping.Factory<? extends MetricMapping>> metricMappingFactories;

    @Inject
    public PrometheusMappingConfigLoader(
            Map<String, MetricMapping.Factory<? extends MetricMapping>> metricMappingFactories) {
        this.metricMappingFactories = metricMappingFactories;

        ymlMapper = new ObjectMapper(new YAMLFactory()).registerModule(new DeserializationProblemHandlerModule());
    }

    public Set<MapperConfig> load(InputStream inputStream) throws IOException {
        final PrometheusMappingConfig config = ymlMapper.readValue(inputStream, PrometheusMappingConfig.class);

        return config.metricMappingConfigs()
                .stream()
                .flatMap(this::mapMetric)
                .collect(Collectors.toSet());
    }

    @Nullable
    private Stream<MapperConfig> mapMetric(MetricMapping.Config config) {
        final MetricMapping.Factory<? extends MetricMapping> factory = metricMappingFactories.get(config.type());
        if (factory == null) {
            log.error("Missing handler to process mapping for metric <{}> of type <{}>. Skipping mapping.",
                    config.metricName(), config.type());
            return Stream.empty();
        }
        return factory.create(config).toMapperConfigs().stream();
    }
}

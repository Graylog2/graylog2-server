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
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.prometheus.client.dropwizard.samplebuilder.MapperConfig;
import org.graylog2.plugin.system.NodeId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.Serializable;
import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PrometheusMappingConfigLoaderTest {
    @Mock
    NodeId nodeId;

    PrometheusMappingConfigLoader configLoader;
    ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        when(nodeId.toString()).thenReturn("abc-123");

        this.configLoader = new PrometheusMappingConfigLoader(
                ImmutableMap.of(MetricMatchMapping.TYPE, config -> new MetricMatchMapping(nodeId, config)));
        this.objectMapper = new ObjectMapper(new YAMLFactory());
    }

    @Test
    void loadBytes() throws Exception {
        final Map<String, ImmutableList<Serializable>> config = Collections.singletonMap("metric_mappings", ImmutableList.of(
                ImmutableMap.of(
                        "metric_name", "test1",
                        "match_pattern", "foo.*.bar",
                        "wildcard_extract_labels", ImmutableList.of("first"),
                        "additional_labels", ImmutableMap.of("another", "label")
                ),
                ImmutableMap.of(
                        "metric_name", "test2",
                        "match_pattern", "hello.world",
                        "additional_labels", ImmutableMap.of("another", "label")
                ),
                ImmutableMap.of(
                        "metric_name", "test3",
                        "match_pattern", "one.*.three",
                        "wildcard_extract_labels", ImmutableList.of("two")
                )
        ));

        assertThat(configLoader.load(new ByteArrayInputStream(objectMapper.writeValueAsBytes(config)))).containsExactlyInAnyOrder(
                new MapperConfig(
                        "foo.*.bar",
                        "gl_test1",
                        ImmutableMap.of(
                                "node", "abc-123",
                                "another", "label",
                                "first", "${0}"
                        )
                ),
                new MapperConfig(
                        "hello.world",
                        "gl_test2",
                        ImmutableMap.of(
                                "node", "abc-123",
                                "another", "label"
                        )
                ),
                new MapperConfig(
                        "one.*.three",
                        "gl_test3",
                        ImmutableMap.of(
                                "node", "abc-123",
                                "two", "${0}"
                        )
                )
        );
    }
}

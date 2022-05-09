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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.prometheus.client.dropwizard.samplebuilder.MapperConfig;
import org.graylog2.plugin.system.NodeId;
import org.graylog2.shared.inputs.MessageInputFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.graylog.metrics.prometheus.mapping.PrometheusMappingFilesHandler.CORE_MAPPING_RESOURCE;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PrometheusMappingFilesHandlerTest {
    @Mock
    NodeId nodeId;

    @Mock
    MessageInputFactory messageInputFactory;

    PrometheusMappingConfigLoader configLoader;

    @BeforeEach
    void setUp() {
        this.configLoader = new PrometheusMappingConfigLoader(
                ImmutableMap.of(
                        MetricMatchMapping.TYPE, config -> new MetricMatchMapping(nodeId, config),
                        InputMetricMapping.TYPE, config -> new InputMetricMapping(messageInputFactory, nodeId, config))
        );
    }

    @Nested
    @DisplayName("with resource only")
    class WithResourceOnly {
        PrometheusMappingFilesHandler handler;

        @BeforeEach
        void setUp() {
            this.handler = new PrometheusMappingFilesHandler(
                    null,
                    null,
                    CORE_MAPPING_RESOURCE,
                    configLoader
            );
        }

        @Test
        void filesHaveChanged() {
            assertThat(handler.filesHaveChanged()).isFalse();
        }

        @Test
        void getMapperConfigs() {
            when(nodeId.toString()).thenReturn("abc-123");

            final List<MapperConfig> mapperConfigs = handler.getMapperConfigs();

            assertThat(mapperConfigs).isNotEmpty();

            // Just check a sample from our built-in metrics to verify that the resource loading works
            assertThat(mapperConfigs).contains(new MapperConfig(
                    "org.apache.logging.log4j.core.Appender.*",
                    "gl_log_appenders",
                    ImmutableMap.of(
                            "node", "abc-123",
                            "status", "${0}"
                    )
            ));
        }
    }

    @Nested
    @DisplayName("with external core mapping")
    class WithExternalCoreMapping {
        PrometheusMappingFilesHandler handler;
        Path coreMapping;

        @BeforeEach
        void setUp(@TempDir Path tempDir) throws Exception {
            this.coreMapping = tempDir.resolve("core-mapping.yml");

            // Write an initial config
            Files.write(coreMapping, ImmutableList.of(
                    "---",
                    "metric_mappings:",
                    "  - metric_name: \"test\"",
                    "    match_pattern: \"test.pattern\"",
                    "    additional_labels:",
                    "      hello: \"world\""
            ));

            this.handler = new PrometheusMappingFilesHandler(
                    coreMapping,
                    null,
                    CORE_MAPPING_RESOURCE,
                    configLoader
            );
        }

        @Test
        void filesHaveChanged() throws Exception {
            // Initially it shouldn't have been marked as changed
            assertThat(handler.filesHaveChanged()).isFalse();

            Files.write(coreMapping, Collections.singletonList("\n"), StandardOpenOption.APPEND);

            // After writing to it, it should have changed
            assertThat(handler.filesHaveChanged()).isTrue();
        }

        @Test
        void getMapperConfigs() throws Exception {
            when(nodeId.toString()).thenReturn("abc-123");

            final List<MapperConfig> mapperConfigs = handler.getMapperConfigs();

            assertThat(mapperConfigs).hasSize(1);

            assertThat(mapperConfigs).containsExactlyInAnyOrder(new MapperConfig(
                    "test.pattern",
                    "gl_test",
                    ImmutableMap.of(
                            "node", "abc-123",
                            "hello", "world"
                    )
            ));

            // Add another metric
            Files.write(coreMapping, ImmutableList.of(
                    "  - metric_name: \"test2\"",
                    "    match_pattern: \"test2.pattern\"",
                    "    additional_labels:",
                    "      hello: \"world2\""
            ), StandardOpenOption.APPEND);

            final List<MapperConfig> mapperConfigs2 = handler.getMapperConfigs();

            assertThat(mapperConfigs2).hasSize(2);

            assertThat(mapperConfigs2).containsExactlyInAnyOrder(
                    new MapperConfig(
                            "test.pattern",
                            "gl_test",
                            ImmutableMap.of(
                                    "node", "abc-123",
                                    "hello", "world"
                            )
                    ),
                    new MapperConfig(
                            "test2.pattern",
                            "gl_test2",
                            ImmutableMap.of(
                                    "node", "abc-123",
                                    "hello", "world2"
                            )
                    )
            );
        }
    }

    @Nested
    @DisplayName("with external custom mapping")
    class WithExternalCustomMapping {
        PrometheusMappingFilesHandler handler;
        Path customMapping;

        @BeforeEach
        void setUp(@TempDir Path tempDir) throws Exception {
            this.customMapping = tempDir.resolve("custom-mapping.yml");

            // Write an initial config
            Files.write(customMapping, ImmutableList.of(
                    "---",
                    "metric_mappings:",
                    "  - metric_name: \"test\"",
                    "    match_pattern: \"test.pattern\"",
                    "    additional_labels:",
                    "      hello: \"world\""
            ));

            this.handler = new PrometheusMappingFilesHandler(
                    null,
                    customMapping,
                    CORE_MAPPING_RESOURCE,
                    configLoader
            );
        }

        @Test
        void filesHaveChanged() throws Exception {
            // Initially it shouldn't have been marked as changed
            assertThat(handler.filesHaveChanged()).isFalse();

            Files.write(customMapping, Collections.singletonList("\n"), StandardOpenOption.APPEND);

            // After writing to it, it should have changed
            assertThat(handler.filesHaveChanged()).isTrue();
        }

        @Test
        void getMapperConfigs() throws Exception {
            when(nodeId.toString()).thenReturn("abc-123");

            final List<MapperConfig> mapperConfigs = handler.getMapperConfigs();

            assertThat(mapperConfigs.size()).isGreaterThan(1);

            assertThat(mapperConfigs).contains(new MapperConfig(
                    "test.pattern",
                    "gl_test",
                    ImmutableMap.of(
                            "node", "abc-123",
                            "hello", "world"
                    )
            ));

            // Add another metric
            Files.write(customMapping, ImmutableList.of(
                    "  - metric_name: \"test2\"",
                    "    match_pattern: \"test2.pattern\"",
                    "    additional_labels:",
                    "      hello: \"world2\""
            ), StandardOpenOption.APPEND);

            final List<MapperConfig> mapperConfigs2 = handler.getMapperConfigs();

            assertThat(mapperConfigs2.size()).isGreaterThan(2);

            assertThat(mapperConfigs2).contains(
                    new MapperConfig(
                            "org.apache.logging.log4j.core.Appender.*",
                            "gl_log_appenders",
                            ImmutableMap.of(
                                    "node", "abc-123",
                                    "status", "${0}"
                            )
                    ),
                    new MapperConfig(
                            "test.pattern",
                            "gl_test",
                            ImmutableMap.of(
                                    "node", "abc-123",
                                    "hello", "world"
                            )
                    ),
                    new MapperConfig(
                            "test2.pattern",
                            "gl_test2",
                            ImmutableMap.of(
                                    "node", "abc-123",
                                    "hello", "world2"
                            )
                    )
            );
        }
    }
}

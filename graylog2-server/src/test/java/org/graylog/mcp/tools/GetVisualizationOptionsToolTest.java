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
package org.graylog.mcp.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.graylog.mcp.server.SchemaGeneratorProvider;
import org.graylog.plugins.views.visualizations.CoreVisualizations;
import org.graylog.plugins.views.visualizations.VisualizationConfigs;
import org.graylog.plugins.views.visualizations.VisualizationDescriptor;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class GetVisualizationOptionsToolTest {
    private final ObjectMapper objectMapper = new ObjectMapperProvider().get();
    private final GetVisualizationOptionsTool tool = new GetVisualizationOptionsTool(
            new VisualizationConfigs(CoreVisualizations.all().stream()
                    .collect(Collectors.toMap(VisualizationDescriptor::name, Function.identity())), objectMapper),
            objectMapper, mock(ClusterConfigService.class), new SchemaGeneratorProvider(Set.of()));
    private final PermissionHelper permissionHelper = mock(PermissionHelper.class);

    @Test
    void listsTheVisualizationsWithTheNamesOfTheirSettings() {
        final String list = tool.apply(permissionHelper, Map.of());

        assertThat(list).contains("- numeric (Single Number): Single number: the value of one metric, without "
                        + "group-bys. Settings: color_preference, trend, trend_preference")
                .contains("- pie (Pie Chart): ")
                .contains("Settings: none");
    }

    @Test
    void describesTheSettingsOfAVisualization() throws Exception {
        final JsonNode options = objectMapper.readTree(tool.apply(permissionHelper, Map.of("visualization", "numeric")));

        assertThat(options.path("title").asText()).isEqualTo("Single Number");
        assertThat(options.at("/settings/properties/trend/description").asText()).startsWith("Shows below the value");
        assertThat(objectMapper.readTree(tool.apply(permissionHelper, Map.of("visualization", "pie"))).has("settings"))
                .isFalse();
        assertThatThrownBy(() -> tool.apply(permissionHelper, Map.of("visualization", "donut")))
                .hasMessageContaining("Unknown visualization <donut>");
    }

    @Test
    void takesAnOptionalVisualization() {
        assertThat(tool.inputSchema()).extractingByKey("properties").asString().contains("visualization");
    }
}

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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import org.graylog.mcp.server.SchemaGeneratorProvider;
import org.graylog.mcp.server.Tool;
import org.graylog.plugins.views.visualizations.VisualizationConfigs;
import org.graylog2.plugin.cluster.ClusterConfigService;

import java.io.UncheckedIOException;
import java.util.Set;
import java.util.stream.Collectors;

import static org.graylog2.shared.utilities.StringUtils.f;

/** Lists the visualizations of aggregation widgets, or describes the settings of one. */
public class GetVisualizationOptionsTool extends Tool<GetVisualizationOptionsTool.Parameters, String> {
    public static final String NAME = "get_visualization_options";

    private final VisualizationConfigs visualizationConfigs;
    private final ObjectMapper objectMapper;

    @Inject
    public GetVisualizationOptionsTool(VisualizationConfigs visualizationConfigs,
                                       ObjectMapper objectMapper,
                                       ClusterConfigService clusterConfigService,
                                       SchemaGeneratorProvider schemaGeneratorProvider) {
        super(
                new TypeReference<>() {},
                new TypeReference<>() {},
                NAME,
                "Get visualization options",
                "Lists the visualizations of aggregation widgets, e.g. bar charts or single numbers, with their "
                        + "settings. With a visualization, returns the JSON Schema of its settings (visualization_config): each "
                        + "setting's type, allowed values, meaning and default.",
                objectMapper,
                clusterConfigService,
                schemaGeneratorProvider
        );
        this.visualizationConfigs = visualizationConfigs;
        this.objectMapper = objectMapper;
    }

    @Override
    public Set<String> checkedPermissions() {
        return Set.of();
    }

    @Override
    public String apply(PermissionHelper permissionHelper, Parameters parameters) {
        final String visualization = parameters.visualization();
        if (visualization == null || visualization.isBlank()) {
            return visualizationConfigs.list().stream()
                    .map(descriptor -> f("- %s (%s): %s Settings: %s", descriptor.name(), descriptor.title(),
                            descriptor.description(), descriptor.hasSettings()
                                    ? String.join(", ", visualizationConfigs.settingNames(descriptor.name()))
                                    : "none"))
                    .collect(Collectors.joining("\n"));
        }
        try {
            return objectMapper.writeValueAsString(visualizationConfigs.options(visualization.trim()));
        } catch (JsonProcessingException e) {
            throw new UncheckedIOException(e);
        }
    }

    @AutoValue
    @JsonDeserialize(builder = Parameters.Builder.class)
    public abstract static class Parameters {
        @JsonProperty("visualization")
        @JsonPropertyDescription("The visualization whose settings to describe, e.g. numeric; leave it empty to list "
                + "the visualizations.")
        @Nullable
        public abstract String visualization();

        @AutoValue.Builder
        public abstract static class Builder {
            @JsonCreator
            public static Builder create() {
                return new AutoValue_GetVisualizationOptionsTool_Parameters.Builder();
            }

            @JsonProperty("visualization")
            public abstract Builder visualization(@Nullable String visualization);

            public abstract Parameters build();
        }
    }
}

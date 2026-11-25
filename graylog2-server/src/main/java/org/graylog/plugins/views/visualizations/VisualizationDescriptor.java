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
package org.graylog.plugins.views.visualizations;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonTypeName;
import jakarta.annotation.Nullable;
import org.graylog.plugins.views.search.views.widgets.aggregation.VisualizationConfigDTO;

import static java.util.Objects.requireNonNull;

/**
 * A visualization of aggregation widgets, e.g. a bar chart, as plugins register it with
 * {@link org.graylog2.plugin.PluginModule#addVisualization(VisualizationDescriptor)}.
 *
 * @param name        the visualization's type, as aggregation configs store it, e.g. {@code bar}
 * @param title       the name users see, e.g. {@code Bar Chart}
 * @param description what it shows and which group-bys and metrics it needs
 * @param configClass the class of its settings, which documents them; null if it has none
 */
public record VisualizationDescriptor(String name, String title, String description,
                                      @Nullable Class<? extends VisualizationConfigDTO> configClass) {
    public VisualizationDescriptor {
        if (requireNonNull(name).isBlank() || requireNonNull(title).isBlank()
                || requireNonNull(description).isBlank()) {
            throw new IllegalArgumentException("A visualization needs a name, a title and a description");
        }
    }

    /** A visualization with settings; its name and description come from the config class's annotations. */
    public static VisualizationDescriptor of(Class<? extends VisualizationConfigDTO> configClass, String title) {
        final JsonTypeName typeName = configClass.getAnnotation(JsonTypeName.class);
        final JsonClassDescription description = configClass.getAnnotation(JsonClassDescription.class);
        if (typeName == null || description == null) {
            throw new IllegalArgumentException(configClass.getName()
                    + " needs @JsonTypeName and @JsonClassDescription to be registered as a visualization");
        }
        return new VisualizationDescriptor(typeName.value(), title, description.value(), configClass);
    }

    public static VisualizationDescriptor withoutSettings(String name, String title, String description) {
        return new VisualizationDescriptor(name, title, description, null);
    }

    public boolean hasSettings() {
        return configClass != null;
    }
}

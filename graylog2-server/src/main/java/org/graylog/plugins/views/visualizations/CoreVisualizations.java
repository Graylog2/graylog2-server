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

import org.graylog.plugins.views.search.views.widgets.aggregation.AreaVisualizationConfigDTO;
import org.graylog.plugins.views.search.views.widgets.aggregation.BarVisualizationConfigDTO;
import org.graylog.plugins.views.search.views.widgets.aggregation.DataTableVisualizationConfigDTO;
import org.graylog.plugins.views.search.views.widgets.aggregation.HeatmapVisualizationConfigDTO;
import org.graylog.plugins.views.search.views.widgets.aggregation.LineVisualizationConfigDTO;
import org.graylog.plugins.views.search.views.widgets.aggregation.NetworkVisualizationConfigDTO;
import org.graylog.plugins.views.search.views.widgets.aggregation.NumberVisualizationConfigDTO;
import org.graylog.plugins.views.search.views.widgets.aggregation.ScatterVisualizationConfigDTO;
import org.graylog.plugins.views.search.views.widgets.aggregation.WorldMapVisualizationConfigDTO;

import java.util.List;

/** The visualizations the web interface ships with. */
public final class CoreVisualizations {
    private CoreVisualizations() {
    }

    public static List<VisualizationDescriptor> all() {
        return List.of(
                VisualizationDescriptor.of(AreaVisualizationConfigDTO.class, "Area Chart"),
                VisualizationDescriptor.of(BarVisualizationConfigDTO.class, "Bar Chart"),
                VisualizationDescriptor.of(DataTableVisualizationConfigDTO.class, "Data Table"),
                VisualizationDescriptor.of(HeatmapVisualizationConfigDTO.class, "Heatmap"),
                VisualizationDescriptor.of(LineVisualizationConfigDTO.class, "Line Chart"),
                VisualizationDescriptor.of(NetworkVisualizationConfigDTO.class, "Network Graph"),
                VisualizationDescriptor.of(NumberVisualizationConfigDTO.class, "Single Number"),
                VisualizationDescriptor.withoutSettings("pie", "Pie Chart",
                        "Pie chart: the share of each value of the group-bys in one metric."),
                VisualizationDescriptor.withoutSettings("sankey", "Sankey",
                        "Sankey diagram: how values flow between two or more group-bys, without timestamp, as links "
                                + "as wide as one metric."),
                VisualizationDescriptor.of(ScatterVisualizationConfigDTO.class, "Scatter Plot"),
                VisualizationDescriptor.of(WorldMapVisualizationConfigDTO.class, "World Map"));
    }
}

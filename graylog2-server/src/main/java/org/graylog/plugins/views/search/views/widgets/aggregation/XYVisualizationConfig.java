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
package org.graylog.plugins.views.search.views.widgets.aggregation;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.HashMap;
import java.util.Optional;

public interface XYVisualizationConfig {
    String FIELD_AXIS_TYPE = "axis_type";
    String FIELD_AXIS_CONFIG = "axis_config";
    String AXIS_TYPE_DESCRIPTION = "The scale of the y-axis: linear, or logarithmic for values that span several "
            + "orders of magnitude.";
    String AXIS_CONFIG_DESCRIPTION = "Labels and colors of the axes, by axis: xaxis for the x-axis, number for the "
            + "y-axis of metrics without a unit, and percent, time and size for the y-axes of metrics on fields with "
            + "that unit. Give each axis a title, a color, or both.";
    AxisType DEFAULT_AXIS_TYPE = AxisType.LINEAR;

    enum AxisType {
        LINEAR("linear"),
        LOGARITHMIC("logarithmic");

        private final String value;

        @JsonValue
        public String value() {
            return this.value;
        }

        @JsonCreator
        AxisType(String value) {
            this.value = value;
        }
    }

    AxisType axisType();

    enum UnitType {
        number,
        percent,
        time,
        size,
        xaxis;
    }

    record SingleAxisConfig(@JsonProperty @JsonPropertyDescription("The label of the axis.")
                            Optional<String> title,
                            @JsonProperty @JsonPropertyDescription("The color of the axis, e.g. #e6194b.")
                            Optional<String> color) {}

    class AxisConfig extends HashMap<UnitType, SingleAxisConfig> {}

    Optional<AxisConfig> axisConfig();
}

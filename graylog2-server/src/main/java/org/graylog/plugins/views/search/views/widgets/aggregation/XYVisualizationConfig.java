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
import com.fasterxml.jackson.annotation.JsonValue;

public interface XYVisualizationConfig {
    String FIELD_AXIS_TYPE = "axis_type";
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
}

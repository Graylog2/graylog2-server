package org.graylog.plugins.views.search.views.widgets.aggregation;

public interface XYVisualizationConfig {
    String FIELD_AXIS_TYPE = "axis_type";
    AxisType DEFAULT_AXIS_TYPE = AxisType.LINEAR;

    enum AxisType {
        LINEAR,
        LOGARITHMIC
    }

    AxisType axisType();
}

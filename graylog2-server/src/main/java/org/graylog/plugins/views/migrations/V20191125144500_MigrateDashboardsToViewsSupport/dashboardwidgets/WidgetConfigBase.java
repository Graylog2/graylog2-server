package org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.dashboardwidgets;

import org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.viewwidgets.Interval;
import org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.viewwidgets.Pivot;
import org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.viewwidgets.Series;
import org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.viewwidgets.SortConfig;
import org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.viewwidgets.TimeHistogramConfig;
import org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.viewwidgets.TimeUnitInterval;
import org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.viewwidgets.ValueConfig;

abstract class WidgetConfigBase implements WidgetConfig {
    static String TIMESTAMP_FIELD = "timestamp";

    Pivot valuesPivotForField(String field, int limit) {
        return Pivot.valuesBuilder()
                .field(field)
                .config(ValueConfig.ofLimit(limit))
                .build();
    }

    Pivot timestampPivot(String interval) {
        return Pivot.timeBuilder()
                .field(TIMESTAMP_FIELD)
                .config(TimeHistogramConfig.builder().interval(timestampInterval(interval)).build())
                .build();
    }

    Series countSeries() {
        return Series.buildFromString("count()").build();
    }

    SortConfig.Direction sortDirection(String sortOrder) {
        switch (sortOrder) {
            case "asc": return SortConfig.Direction.Ascending;
            case "desc": return SortConfig.Direction.Descending;
        }
        throw new RuntimeException("Unable to parse sort order: "  + sortOrder);
    }

    Interval timestampInterval(String interval) {
        switch (interval) {
            case "minute": return TimeUnitInterval.create(TimeUnitInterval.IntervalUnit.MINUTES, 1);
            case "hour": return TimeUnitInterval.create(TimeUnitInterval.IntervalUnit.HOURS, 1);
        }
        throw new RuntimeException("Unable to map interval: " + interval);
    }

    String mapStatsFunction(String function) {
        switch (function) {
            case "total": return "sum";
            case "mean": return "avg";
            case "std_deviation": return "stddev";
            case "cardinality": return "card";
            case "count":
            case "variance":
            case "min":
            case "max":
            case "sum":
                return function;
        }
        throw new RuntimeException("Unable to map statistical function: " + function);
    }

    String mapRendererToVisualization(String renderer) {
        switch (renderer) {
            case "bar":
            case "line":
                return renderer;
            case "area":
                // TODO: Do something about
                throw new RuntimeException("Area chart is unsupported");
            case "scatterplot": return "scatter";
        }
        throw new RuntimeException("Unable to map renderer to visualization: " + renderer);
    }
}

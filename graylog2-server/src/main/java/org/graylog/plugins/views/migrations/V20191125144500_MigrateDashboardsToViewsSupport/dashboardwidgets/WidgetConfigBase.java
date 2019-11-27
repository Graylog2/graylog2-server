package org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.dashboardwidgets;

import org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.ElasticsearchQueryString;
import org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.ViewWidget;
import org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.viewwidgets.Interval;
import org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.viewwidgets.Pivot;
import org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.viewwidgets.Series;
import org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.viewwidgets.SortConfig;
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

    Series countSeries() {
        return Series.createFromString("count()").build();
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
            case "minute": return TimeUnitInterval.builder().unit(TimeUnitInterval.IntervalUnit.MINUTES).value(1).build();
        }
        throw new RuntimeException("Unable to map interval: " + interval);
    }

    ViewWidget.Builder createViewWidget() {
        return ViewWidget.builder()
                .query(ElasticsearchQueryString.create(query()))
                .timerange(timerange());
    }
}

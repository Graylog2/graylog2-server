package org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.dashboardwidgets;

import org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.ElasticsearchQueryString;
import org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.ViewWidget;
import org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.viewwidgets.Interval;
import org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.viewwidgets.TimeUnitInterval;

abstract class WidgetConfigBase implements WidgetConfig {
    static String TIMESTAMP_FIELD = "timestamp";

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

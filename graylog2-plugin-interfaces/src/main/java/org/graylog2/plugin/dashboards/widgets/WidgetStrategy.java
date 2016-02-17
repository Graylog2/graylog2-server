package org.graylog2.plugin.dashboards.widgets;

import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;

import java.util.Map;

public interface WidgetStrategy {
    interface Factory<T> {
        T create(Map<String, Object> config,
                 TimeRange timeRange,
                 String widgetId);
    }

    ComputationResult compute();
}

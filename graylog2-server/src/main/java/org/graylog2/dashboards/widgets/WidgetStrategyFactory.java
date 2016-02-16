package org.graylog2.dashboards.widgets;

import org.graylog2.indexer.searches.Searches;
import org.graylog2.plugin.dashboards.widgets.WidgetStrategy;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;

import javax.inject.Inject;
import java.util.Map;

public class WidgetStrategyFactory {
    private final Map<String, WidgetStrategy.Factory<? extends WidgetStrategy>> widgetStrategyFactories;
    private final Searches searches;

    @Inject
    public WidgetStrategyFactory(Map<String, WidgetStrategy.Factory<? extends WidgetStrategy>> widgetStrategyFactories,
                                 Searches searches) {
        this.widgetStrategyFactories = widgetStrategyFactories;
        this.searches = searches;
    }

    public WidgetStrategy getWidgetForType(final String typeName, Map<String, Object> config, TimeRange timeRange, String widgetId) throws InvalidWidgetConfigurationException {
        final String query = (String)config.get("query");
        final DashboardWidget.Type type = DashboardWidget.Type.valueOf(typeName);
        switch (type) {
            case SEARCH_RESULT_COUNT:
                return new SearchResultCountWidget(searches,
                        config,
                        query,
                        timeRange);
            case STREAM_SEARCH_RESULT_COUNT:
                return new StreamSearchResultCountWidget(searches,
                        config,
                        query,
                        timeRange);
            case FIELD_CHART:
                return new FieldChartWidget(searches,
                        config,
                        query,
                        timeRange,
                        widgetId);
            case QUICKVALUES:
                return new QuickvaluesWidget(searches,
                        config,
                        query,
                        timeRange);
            case SEARCH_RESULT_CHART:
                return new SearchResultChartWidget(searches,
                        config,
                        query,
                        timeRange);
            case STATS_COUNT:
                return new StatisticalCountWidget(searches,
                        config,
                        query,
                        timeRange);
            case STACKED_CHART:
                return new StackedChartWidget(searches,
                        config,
                        timeRange,
                        widgetId);
            default:
                return widgetStrategyFactories.get("type").create(config, query, timeRange);
        }
    }
}

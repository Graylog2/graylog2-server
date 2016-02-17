package org.graylog2.dashboards.widgets;

import org.graylog2.plugin.dashboards.widgets.WidgetStrategy;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;

import javax.inject.Inject;
import java.util.Map;

public class WidgetStrategyFactory {
    private final Map<String, WidgetStrategy.Factory<? extends WidgetStrategy>> widgetStrategyFactories;

    @Inject
    public WidgetStrategyFactory(Map<String, WidgetStrategy.Factory<? extends WidgetStrategy>> widgetStrategyFactories) {
        this.widgetStrategyFactories = widgetStrategyFactories;
    }

    public WidgetStrategy getWidgetForType(final String typeName, Map<String, Object> config, TimeRange timeRange, String widgetId) throws InvalidWidgetConfigurationException {
        final WidgetStrategy.Factory<? extends WidgetStrategy> factory = widgetStrategyFactories.get(typeName);

        if (factory == null) {
            throw new IllegalArgumentException("Widget type <" + typeName + "> not found!");
        }
        return factory.create(config, timeRange, widgetId);
    }
}

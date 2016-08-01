/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.dashboards.widgets;

import org.graylog2.plugin.dashboards.widgets.WidgetStrategy;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;

import javax.inject.Inject;
import java.util.Locale;
import java.util.Map;

public class WidgetStrategyFactory {
    private final Map<String, WidgetStrategy.Factory<? extends WidgetStrategy>> widgetStrategyFactories;

    @Inject
    public WidgetStrategyFactory(Map<String, WidgetStrategy.Factory<? extends WidgetStrategy>> widgetStrategyFactories) {
        this.widgetStrategyFactories = widgetStrategyFactories;
    }

    public WidgetStrategy getWidgetForType(final String typeName, Map<String, Object> config, TimeRange timeRange, String widgetId) throws InvalidWidgetConfigurationException {
        if (widgetStrategyFactories.containsKey(typeName)) {
            final WidgetStrategy.Factory<? extends WidgetStrategy> factory = widgetStrategyFactories.get(typeName);
            return factory.create(config, timeRange, widgetId);
        }

        final String upperCaseTypeName = typeName.toUpperCase(Locale.ENGLISH);
        if (widgetStrategyFactories.containsKey(upperCaseTypeName)) {
            final WidgetStrategy.Factory<? extends WidgetStrategy> factory = widgetStrategyFactories.get(upperCaseTypeName);
            return factory.create(config, timeRange, widgetId);
        }

        throw new IllegalArgumentException("Widget type <" + typeName + "> not found!");
    }
}

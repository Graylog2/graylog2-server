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
package org.graylog2.bindings;

import org.graylog2.dashboards.widgets.strategies.FieldChartWidgetStrategy;
import org.graylog2.dashboards.widgets.strategies.QuickvaluesWidgetStrategy;
import org.graylog2.dashboards.widgets.strategies.SearchResultChartWidgetStrategy;
import org.graylog2.dashboards.widgets.strategies.SearchResultCountWidgetStrategy;
import org.graylog2.dashboards.widgets.strategies.StackedChartWidgetStrategy;
import org.graylog2.dashboards.widgets.strategies.StatisticalCountWidgetStrategy;
import org.graylog2.dashboards.widgets.strategies.StreamSearchResultCountWidgetStrategy;
import org.graylog2.plugin.inject.Graylog2Module;

public class WidgetStrategyBindings extends Graylog2Module {
    @Override
    protected void configure() {
        installWidgetStrategyWithAlias(widgetStrategyBinder(), "FIELD_CHART", FieldChartWidgetStrategy.class, FieldChartWidgetStrategy.Factory.class);
        installWidgetStrategyWithAlias(widgetStrategyBinder(), "QUICKVALUES", QuickvaluesWidgetStrategy.class, QuickvaluesWidgetStrategy.Factory.class);
        installWidgetStrategyWithAlias(widgetStrategyBinder(), "SEARCH_RESULT_CHART", SearchResultChartWidgetStrategy.class, SearchResultChartWidgetStrategy.Factory.class);
        installWidgetStrategyWithAlias(widgetStrategyBinder(), "SEARCH_RESULT_COUNT", SearchResultCountWidgetStrategy.class, SearchResultCountWidgetStrategy.Factory.class);
        installWidgetStrategyWithAlias(widgetStrategyBinder(), "STACKED_CHART", StackedChartWidgetStrategy.class, StackedChartWidgetStrategy.Factory.class);
        installWidgetStrategyWithAlias(widgetStrategyBinder(), "STATS_COUNT", StatisticalCountWidgetStrategy.class, StatisticalCountWidgetStrategy.Factory.class);
        installWidgetStrategyWithAlias(widgetStrategyBinder(), "STREAM_SEARCH_RESULT_COUNT", StreamSearchResultCountWidgetStrategy.class, StreamSearchResultCountWidgetStrategy.Factory.class);
    }
}

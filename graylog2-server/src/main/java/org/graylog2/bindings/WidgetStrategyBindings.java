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

import org.graylog2.dashboards.widgets.DashboardWidget;
import org.graylog2.dashboards.widgets.FieldChartWidget;
import org.graylog2.dashboards.widgets.QuickvaluesWidget;
import org.graylog2.dashboards.widgets.SearchResultChartWidget;
import org.graylog2.dashboards.widgets.SearchResultCountWidget;
import org.graylog2.dashboards.widgets.StackedChartWidget;
import org.graylog2.dashboards.widgets.StatisticalCountWidget;
import org.graylog2.dashboards.widgets.StreamSearchResultCountWidget;
import org.graylog2.plugin.inject.Graylog2Module;

public class WidgetStrategyBindings extends Graylog2Module {
    @Override
    protected void configure() {
        installWidgetStrategyWithAlias(widgetStrategyBinder(), DashboardWidget.Type.FIELD_CHART.toString(), FieldChartWidget.class, FieldChartWidget.Factory.class);
        installWidgetStrategyWithAlias(widgetStrategyBinder(), DashboardWidget.Type.QUICKVALUES.toString(), QuickvaluesWidget.class, QuickvaluesWidget.Factory.class);
        installWidgetStrategyWithAlias(widgetStrategyBinder(), DashboardWidget.Type.SEARCH_RESULT_CHART.toString(), SearchResultChartWidget.class, SearchResultChartWidget.Factory.class);
        installWidgetStrategyWithAlias(widgetStrategyBinder(), DashboardWidget.Type.SEARCH_RESULT_COUNT.toString(), SearchResultCountWidget.class, SearchResultCountWidget.Factory.class);
        installWidgetStrategyWithAlias(widgetStrategyBinder(), DashboardWidget.Type.STACKED_CHART.toString(), StackedChartWidget.class, StackedChartWidget.Factory.class);
        installWidgetStrategyWithAlias(widgetStrategyBinder(), DashboardWidget.Type.STATS_COUNT.toString(), StatisticalCountWidget.class, StatisticalCountWidget.Factory.class);
        installWidgetStrategy(widgetStrategyBinder(), StreamSearchResultCountWidget.class, StreamSearchResultCountWidget.Factory.class);
    }
}

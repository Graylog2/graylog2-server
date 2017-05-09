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
package org.graylog2.dashboards.widgets.strategies;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.graylog2.indexer.results.HistogramResult;
import org.graylog2.indexer.searches.Searches;
import org.graylog2.plugin.dashboards.widgets.ComputationResult;
import org.graylog2.plugin.dashboards.widgets.WidgetStrategy;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;

import java.util.Map;

import static com.google.common.base.Strings.isNullOrEmpty;

public class SearchResultChartWidgetStrategy extends ChartWidgetStrategy {
    public interface Factory extends WidgetStrategy.Factory<SearchResultChartWidgetStrategy> {
        @Override
        SearchResultChartWidgetStrategy create(Map<String, Object> config, TimeRange timeRange, String widgetId);
    }

    private final String query;
    private final Searches searches;
    private final TimeRange timeRange;

    @AssistedInject
    public SearchResultChartWidgetStrategy(Searches searches, @Assisted Map<String, Object> config, @Assisted TimeRange timeRange, @Assisted String widgetId) {        super(config);
        this.searches = searches;
        this.timeRange = timeRange;
        this.query = getNonEmptyQuery((String)config.get("query"));
    }

    // We need to ensure query is not empty, or the histogram calculation will fail
    private String getNonEmptyQuery(String query) {
        if (isNullOrEmpty(query)) {
            return "*";
        }
        return query;
    }

    public String getQuery() {
        return query;
    }

    @Override
    public ComputationResult compute() {
        String filter = null;
        if (!isNullOrEmpty(streamId)) {
            filter = "streams:" + streamId;
        }

        final HistogramResult histogram = searches.histogram(query, interval, filter, this.timeRange);
        return new ComputationResult(histogram.getResults(), histogram.tookMs(), histogram.getHistogramBoundaries());
    }
}

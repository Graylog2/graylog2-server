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

import com.google.common.collect.ImmutableMap;
import org.graylog2.indexer.results.HistogramResult;
import org.graylog2.indexer.searches.Searches;
import org.graylog2.plugin.dashboards.widgets.ComputationResult;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;

import java.util.Map;

import static com.google.common.base.Strings.isNullOrEmpty;

public class SearchResultChartWidget extends ChartWidget {

    private final String query;
    private final Searches searches;
    private final TimeRange timeRange;

    public SearchResultChartWidget(Searches searches, Map<String, Object> config, String query, TimeRange timeRange) {
        super(config);
        this.searches = searches;
        this.timeRange = timeRange;
        this.query = getNonEmptyQuery(query);
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
    public Map<String, Object> getPersistedConfig() {
        final ImmutableMap.Builder<String, Object> persistedConfig = ImmutableMap.<String, Object>builder()
                .putAll(super.getPersistedConfig())
                .put("query", query);

        return persistedConfig.build();
    }

    @Override
    public ComputationResult compute() {
        String filter = null;
        if (!isNullOrEmpty(streamId)) {
            filter = "streams:" + streamId;
        }

        HistogramResult histogram = searches.histogram(query, interval, filter, this.timeRange);
        return new ComputationResult(histogram.getResults(), histogram.took().millis(), histogram.getHistogramBoundaries());
    }
}

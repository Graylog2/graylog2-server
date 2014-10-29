/**
 * This file is part of Graylog2.
 *
 * Graylog2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog2.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.dashboards.widgets;

import com.codahale.metrics.MetricRegistry;
import com.google.common.collect.ImmutableMap;
import org.graylog2.indexer.IndexHelper;
import org.graylog2.indexer.results.HistogramResult;
import org.graylog2.indexer.searches.Searches;
import org.graylog2.indexer.searches.timeranges.TimeRange;

import javax.annotation.Nullable;
import java.util.Map;

import static com.google.common.base.Strings.isNullOrEmpty;

public class SearchResultChartWidget extends DashboardWidget {

    private final String query;
    private final TimeRange timeRange;
    private final Searches.DateHistogramInterval interval;
    @Nullable
    private final String streamId;
    private final Searches searches;

    public SearchResultChartWidget(MetricRegistry metricRegistry, Searches searches, String id, String description, int cacheTime, Map<String, Object> config, String query, TimeRange timeRange, String creatorUserId) {
        super(metricRegistry, Type.SEARCH_RESULT_CHART, id, description, cacheTime, config, creatorUserId);
        this.searches = searches;

        this.query = getNonEmptyQuery(query);
        this.timeRange = timeRange;

        this.streamId = (String) config.get("stream_id");

        if (config.containsKey("interval")) {
            this.interval = Searches.DateHistogramInterval.valueOf(((String) config.get("interval")).toUpperCase());
        } else {
            this.interval = Searches.DateHistogramInterval.MINUTE;
        }
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

    public TimeRange getTimeRange() {
        return timeRange;
    }

    @Override
    public Map<String, Object> getPersistedConfig() {
        final ImmutableMap.Builder<String, Object> persistedConfig = ImmutableMap.<String, Object>builder()
                .put("query", query)
                .put("interval", interval.toString().toLowerCase())
                .put("timerange", timeRange.getPersistedConfig());

        if (!isNullOrEmpty(streamId)) {
            persistedConfig.put("stream_id", streamId);
        }

        return persistedConfig.build();
    }

    @Override
    protected ComputationResult compute() {
        String filter = null;
        if (!isNullOrEmpty(streamId)) {
            filter = "streams:" + streamId;
        }

        try {
            HistogramResult histogram = searches.histogram(query, interval, filter, timeRange);
            return new ComputationResult(histogram.getResults(), histogram.took().millis(), histogram.getHistogramBoundaries());
        } catch (IndexHelper.InvalidRangeFormatException e) {
            throw new RuntimeException("Invalid timerange format.", e);
        }
    }
}

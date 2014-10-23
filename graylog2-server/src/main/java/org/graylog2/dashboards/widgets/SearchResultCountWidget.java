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
import org.graylog2.indexer.results.CountResult;
import org.graylog2.indexer.searches.Searches;
import org.graylog2.indexer.searches.timeranges.TimeRange;

import java.util.Map;

public class SearchResultCountWidget extends DashboardWidget {

    private final Searches searches;
    private final String query;
    private final TimeRange timeRange;

    public SearchResultCountWidget(MetricRegistry metricRegistry, Searches searches, String id, String description, int cacheTime, Map<String, Object> config, String query, TimeRange timeRange, String creatorUserId) {
        super(metricRegistry, Type.SEARCH_RESULT_COUNT, id, description, cacheTime, config, creatorUserId);
        this.searches = searches;

        this.query = query;
        this.timeRange = timeRange;
    }

    public String getQuery() {
        return query;
    }

    public TimeRange getTimeRange() {
        return timeRange;
    }

    @Override
    public Map<String, Object> getPersistedConfig() {
        return ImmutableMap.<String, Object>builder()
                .put("query", query)
                .put("timerange", timeRange.getPersistedConfig())
                .build();
    }

    @Override
    protected ComputationResult compute() {
        try {
            CountResult cr = searches.count(query, timeRange);
            return new ComputationResult(cr.getCount(), cr.getTookMs());
        } catch (IndexHelper.InvalidRangeFormatException e) {
            throw new RuntimeException("Invalid timerange format.", e);
        }
    }
}

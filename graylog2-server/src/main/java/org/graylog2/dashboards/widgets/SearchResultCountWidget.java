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
import com.google.common.collect.Maps;
import org.graylog2.indexer.IndexHelper;
import org.graylog2.indexer.results.CountResult;
import org.graylog2.indexer.searches.Searches;
import org.graylog2.indexer.searches.timeranges.AbsoluteRange;
import org.graylog2.indexer.searches.timeranges.RelativeRange;
import org.graylog2.indexer.searches.timeranges.TimeRange;
import org.joda.time.DateTime;
import org.joda.time.Seconds;

import java.util.Map;

public class SearchResultCountWidget extends DashboardWidget {

    protected final Searches searches;
    protected final String query;
    protected final TimeRange timeRange;
    protected final Boolean trend;
    protected final Boolean lowerIsBetter;

    public SearchResultCountWidget(MetricRegistry metricRegistry, Searches searches, String id, String description, int cacheTime, Map<String, Object> config, String query, TimeRange timeRange, String creatorUserId) {
        this(metricRegistry, Type.SEARCH_RESULT_COUNT, searches, id, description, cacheTime, config, query, timeRange, creatorUserId);
    }

    protected SearchResultCountWidget(MetricRegistry metricRegistry, Type type, Searches searches, String id, String description, int cacheTime, Map<String, Object> config, String query, TimeRange timeRange, String creatorUserId) {
        super(metricRegistry, type, id, description, cacheTime, config, creatorUserId);
        this.searches = searches;

        this.query = query;
        this.timeRange = timeRange;
        this.trend = config.get("trend") != null && Boolean.parseBoolean(String.valueOf(config.get("trend")));
        this.lowerIsBetter = config.get("lower_is_better") != null && Boolean.parseBoolean(String.valueOf(config.get("lower_is_better")));
    }

    protected Searches getSearches() {
        return searches;
    }

    public TimeRange getTimeRange() {
        return timeRange;
    }

    @Override
    public Map<String, Object> getPersistedConfig() {
        return ImmutableMap.<String, Object>builder()
                .put("query", query)
                .put("timerange", timeRange.getPersistedConfig())
                .put("trend", trend)
                .put("lower_is_better", lowerIsBetter)
                .build();
    }

    @Override
    protected ComputationResult compute() {
        return computeInternal(null);
    }

    protected ComputationResult computeInternal(String filter) {
        try {
            CountResult cr = searches.count(query, timeRange, filter);
            if (trend && timeRange instanceof RelativeRange) {
                DateTime toPrevious = timeRange.getFrom();
                DateTime fromPrevious = toPrevious.minus(Seconds.seconds(((RelativeRange) timeRange).getRange()));
                TimeRange previousTimeRange = new AbsoluteRange(fromPrevious, toPrevious);
                CountResult previousCr = searches.count(query, previousTimeRange);

                Map<String, Object> results = Maps.newHashMap();
                results.put("now", cr.getCount());
                results.put("previous", previousCr.getCount());
                long tookMs = cr.getTookMs() + previousCr.getTookMs();

                return new ComputationResult(results, tookMs);
            } else {
                return new ComputationResult(cr.getCount(), cr.getTookMs());
            }
        } catch (IndexHelper.InvalidRangeFormatException e) {
            throw new RuntimeException("Invalid timerange format.", e);
        }
    }
}

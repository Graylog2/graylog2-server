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

import com.codahale.metrics.MetricRegistry;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
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
    protected final Boolean trend;
    protected final Boolean lowerIsBetter;

    public SearchResultCountWidget(MetricRegistry metricRegistry, Searches searches, String id, String description, WidgetCacheTime cacheTime, Map<String, Object> config, String query, TimeRange timeRange, String creatorUserId) {
        this(metricRegistry, Type.SEARCH_RESULT_COUNT, searches, id, description, cacheTime, config, query, timeRange, creatorUserId);
    }

    protected SearchResultCountWidget(MetricRegistry metricRegistry, Type type, Searches searches, String id, String description, WidgetCacheTime cacheTime, Map<String, Object> config, String query, TimeRange timeRange, String creatorUserId) {
        super(metricRegistry, type, id, timeRange, description, cacheTime, config, creatorUserId);
        this.searches = searches;

        this.query = query;
        this.trend = config.get("trend") != null && Boolean.parseBoolean(String.valueOf(config.get("trend")));
        this.lowerIsBetter = config.get("lower_is_better") != null && Boolean.parseBoolean(String.valueOf(config.get("lower_is_better")));
    }

    protected Searches getSearches() {
        return searches;
    }

    @Override
    public Map<String, Object> getPersistedConfig() {
        return ImmutableMap.<String, Object>builder()
                .putAll(super.getPersistedConfig())
                .put("query", query)
                .put("trend", trend)
                .put("lower_is_better", lowerIsBetter)
                .build();
    }

    @Override
    protected ComputationResult compute() {
        return computeInternal(null);
    }

    protected ComputationResult computeInternal(String filter) {
        final TimeRange timeRange = this.getTimeRange();
        CountResult cr = searches.count(query, timeRange, filter);
        if (trend && timeRange instanceof RelativeRange) {
            DateTime toPrevious = timeRange.getFrom();
            DateTime fromPrevious = toPrevious.minus(Seconds.seconds(((RelativeRange) timeRange).getRange()));
            TimeRange previousTimeRange = AbsoluteRange.create(fromPrevious, toPrevious);
            CountResult previousCr = searches.count(query, previousTimeRange);

            Map<String, Object> results = Maps.newHashMap();
            results.put("now", cr.count());
            results.put("previous", previousCr.count());
            long tookMs = cr.tookMs() + previousCr.tookMs();

            return new ComputationResult(results, tookMs);
        } else {
            return new ComputationResult(cr.count(), cr.tookMs());
        }
    }
}

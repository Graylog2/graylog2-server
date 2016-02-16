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
import com.google.common.collect.Maps;
import org.graylog2.indexer.results.CountResult;
import org.graylog2.indexer.searches.Searches;
import org.graylog2.plugin.dashboards.widgets.ComputationResult;
import org.graylog2.plugin.dashboards.widgets.WidgetStrategy;
import org.graylog2.plugin.indexer.searches.timeranges.AbsoluteRange;
import org.graylog2.plugin.indexer.searches.timeranges.RelativeRange;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;
import org.joda.time.DateTime;
import org.joda.time.Seconds;

import java.util.Map;

public class SearchResultCountWidget implements WidgetStrategy {

    protected final Searches searches;
    protected final String query;
    protected final TimeRange timeRange;
    protected final Boolean trend;
    protected final Boolean lowerIsBetter;

    protected SearchResultCountWidget(Searches searches, Map<String, Object> config, String query, TimeRange timeRange) {
        this.searches = searches;

        this.query = query;
        this.timeRange = timeRange;
        this.trend = config.get("trend") != null && Boolean.parseBoolean(String.valueOf(config.get("trend")));
        this.lowerIsBetter = config.get("lower_is_better") != null && Boolean.parseBoolean(String.valueOf(config.get("lower_is_better")));
    }

    protected Searches getSearches() {
        return searches;
    }

    public Map<String, Object> getPersistedConfig() {
        return ImmutableMap.<String, Object>builder()
                .putAll(ImmutableMap.of("timerange", this.timeRange.getPersistedConfig()))
                .put("query", query)
                .put("trend", trend)
                .put("lower_is_better", lowerIsBetter)
                .build();
    }

    @Override
    public ComputationResult compute() {
        return computeInternal(null);
    }

    protected ComputationResult computeInternal(String filter) {
        final TimeRange timeRange = this.timeRange;
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

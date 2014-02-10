/**
 * Copyright 2014 Lennart Koopmann <lennart@torch.sh>
 *
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
 *
 */
package org.graylog2.dashboards.widgets;

import org.graylog2.Core;
import org.graylog2.indexer.IndexHelper;
import org.graylog2.indexer.Indexer;
import org.graylog2.indexer.results.HistogramResult;
import org.graylog2.indexer.searches.timeranges.TimeRange;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class SearchResultChartWidget extends DashboardWidget {

    private final Core core;
    private final String query;
    private final TimeRange timeRange;
    private final Indexer.DateHistogramInterval interval;
    private final String streamId;

    public SearchResultChartWidget(Core core, String id, String description, int cacheTime, Map<String, Object> config, String query, TimeRange timeRange, String creatorUserId) {
        super(core, Type.SEARCH_RESULT_CHART, id, description, cacheTime, config, creatorUserId);

        this.query = query;
        this.timeRange = timeRange;
        this.core = core;

        if (config.containsKey("stream_id")) {
            this.streamId = (String) config.get("stream_id");
        } else {
            this.streamId = null;
        }

        if (config.containsKey("interval")) {
            this.interval = Indexer.DateHistogramInterval.valueOf(((String) config.get("interval")).toUpperCase());
        } else {
            this.interval = Indexer.DateHistogramInterval.MINUTE;
        }
    }

    public String getQuery() {
        return query;
    }

    public TimeRange getTimeRange() {
        return timeRange;
    }

    @Override
    public Map<String, Object> getPersistedConfig() {
        return new HashMap<String, Object>() {{
            put("query", query);
            put("stream_id", streamId);
            put("interval", interval.toString().toLowerCase());
            put("timerange", timeRange.getPersistedConfig());
        }};
    }

    @Override
    protected ComputationResult compute() {
        String filter = null;
        if (streamId != null && !streamId.isEmpty()) {
            filter = "streams:" + streamId;
        }

        try {
            HistogramResult histogram = core.getIndexer().searches().histogram(query, interval, filter, timeRange);
            return new ComputationResult(histogram.getResults(), histogram.took().millis());
        } catch (IndexHelper.InvalidRangeFormatException e) {
            throw new RuntimeException("Invalid timerange format.", e);
        }
    }

}

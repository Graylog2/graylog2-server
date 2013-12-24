/**
 * Copyright 2013 Lennart Koopmann <lennart@torch.sh>
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
import org.graylog2.indexer.results.CountResult;
import org.graylog2.indexer.searches.timeranges.TimeRange;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class StreamSearchResultCountWidget extends DashboardWidget {

    private final Core core;
    private final String query;
    private final TimeRange timeRange;
    private final String streamId;

    public StreamSearchResultCountWidget(Core core, String id, String description, int cacheTime, Map<String, Object> config, String query, TimeRange timeRange, String creatorUserId) {
        super(core, DashboardWidget.Type.STREAM_SEARCH_RESULT_COUNT, id, description, cacheTime, config, creatorUserId);

        this.query = query;
        this.timeRange = timeRange;
        this.core = core;
        this.streamId = (String) config.get("stream_id");
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
            put("timerange", timeRange.getPersistedConfig());
            put("stream_id", streamId);
        }};
    }

    @Override
    protected ComputationResult compute() {
        try {
            CountResult cr = core.getIndexer().searches().count(query, timeRange, "streams:" + streamId);
            return new ComputationResult(cr.getCount(), cr.getTookMs());
        } catch (IndexHelper.InvalidRangeFormatException e) {
            throw new RuntimeException("Invalid timerange format.", e);
        }
    }

}

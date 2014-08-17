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
import org.graylog2.indexer.IndexHelper;
import org.graylog2.indexer.Indexer;
import org.graylog2.indexer.results.CountResult;
import org.graylog2.indexer.searches.timeranges.TimeRange;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class StreamSearchResultCountWidget extends DashboardWidget {

    private final Indexer indexer;
    private final String query;
    private final TimeRange timeRange;
    private final String streamId;

    public StreamSearchResultCountWidget(MetricRegistry metricRegistry, Indexer indexer, String id, String description, int cacheTime, Map<String, Object> config, String query, TimeRange timeRange, String creatorUserId) {
        super(metricRegistry, DashboardWidget.Type.STREAM_SEARCH_RESULT_COUNT, id, description, cacheTime, config, creatorUserId);
        this.indexer = indexer;

        this.query = query;
        this.timeRange = timeRange;
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
            CountResult cr = indexer.searches().count(query, timeRange, "streams:" + streamId);
            return new ComputationResult(cr.getCount(), cr.getTookMs());
        } catch (IndexHelper.InvalidRangeFormatException e) {
            throw new RuntimeException("Invalid timerange format.", e);
        }
    }

}

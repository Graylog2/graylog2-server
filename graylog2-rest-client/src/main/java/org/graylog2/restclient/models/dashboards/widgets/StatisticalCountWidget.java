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
package org.graylog2.restclient.models.dashboards.widgets;

import com.google.common.collect.Maps;
import org.graylog2.restclient.lib.timeranges.TimeRange;
import org.graylog2.restclient.models.dashboards.Dashboard;

import java.util.Map;

import static com.google.common.base.Strings.isNullOrEmpty;

public class StatisticalCountWidget extends SearchResultCountWidget {
    private final String field;
    private final String statsFunction;
    private final String streamId;


    public StatisticalCountWidget(Dashboard dashboard, String query, TimeRange timerange, String description, boolean trend, boolean lowerIsBetter, String field, String statsFunction, String streamId) {
        this(dashboard, null, description, 0, query, timerange, trend, lowerIsBetter, field, statsFunction, streamId, null);
    }

    public StatisticalCountWidget(Dashboard dashboard, String query, TimeRange timerange, String description, String field, String statsFunction, String streamId) {
        this(dashboard, null, description, 0, query, timerange, false, false, field, statsFunction, streamId, null);
    }

    public StatisticalCountWidget(Dashboard dashboard, String id, String description, int cacheTime, String query, TimeRange timerange, boolean trend, boolean lowerIsBetter, String field, String statsFunction, String streamId, String creatorUserId) {
        super(Type.STATS_COUNT, dashboard, id, description, cacheTime, query, timerange, trend, lowerIsBetter, creatorUserId);

        if (statsFunction == null || statsFunction.isEmpty()) {
            throw new RuntimeException("Missing statsFunction for widget [" + id + "] on dashboard [" + dashboard.getId() + "].");
        }

        if (field == null || field.isEmpty()) {
            throw new RuntimeException("Missing field for widget [" + id + "] on dashboard [" + dashboard.getId() + "].");
        }

        this.field = field;
        this.statsFunction = statsFunction;
        this.streamId = streamId;
    }

    @Override
    public Map<String, Object> getConfig() {
        Map<String, Object> config = Maps.newHashMap();
        config.putAll(super.getConfig());
        config.put("field", field);
        config.put("stats_function", statsFunction);
        if (!isNullOrEmpty(streamId)) {
            config.put("stream_id", streamId);
        }

        return config;
    }

    public String getField() {
        return field;
    }

    public String getStatsFunction() {
        return statsFunction;
    }
}

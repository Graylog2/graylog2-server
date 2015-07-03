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
import org.graylog2.restclient.lib.timeranges.RelativeRange;
import org.graylog2.restclient.lib.timeranges.TimeRange;
import org.graylog2.restclient.models.dashboards.Dashboard;

import java.util.Map;

public class SearchResultChartWidget extends ChartWidget {

    public SearchResultChartWidget(Dashboard dashboard, String query, TimeRange timerange, String description, String streamId, String interval) {
        this(dashboard, null, description, streamId, 0, query, timerange, null, interval);
    }

    public SearchResultChartWidget(Dashboard dashboard, String id, String description, String streamId, int cacheTime, String query, TimeRange timerange, String creatorUserId, String interval) {
        super(Type.SEARCH_RESULT_CHART, id, description, cacheTime, dashboard, creatorUserId, query, timerange, streamId, interval);
    }

    @Override
    public Map<String, Object> getConfig() {
        Map<String, Object> config = Maps.newHashMap();
        config.putAll(super.getConfig());
        config.putAll(getTimerange().getQueryParams());
        config.put("query", getQuery());

        return config;
    }

    @Override
    public int getWidth() {
        int storedWidth = super.getWidth();
        return storedWidth == 0 ? DEFAULT_WIDTH : storedWidth;
    }

    @Override
    public int getHeight() {
        int storedHeight = super.getHeight();
        return storedHeight == 0 ? DEFAULT_HEIGHT : storedHeight;
    }

    @Override
    public boolean hasFixedTimeAxis() {
        TimeRange timeRange = getTimerange();
        return ((timeRange.getType() != TimeRange.Type.RELATIVE) || !(((RelativeRange)timeRange).isEmptyRange()));
    }
}
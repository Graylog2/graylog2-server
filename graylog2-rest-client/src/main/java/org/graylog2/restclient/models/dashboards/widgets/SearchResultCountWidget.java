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

public class SearchResultCountWidget extends DashboardWidget {

    private static final int WIDTH = 1;
    private static final int HEIGHT = 1;

    private final Boolean trend;
    private final Boolean lowerIsBetter;

    public SearchResultCountWidget(Dashboard dashboard, String query, TimeRange timerange, String description, boolean trend, boolean lowerIsBetter) {
        this(dashboard, null, description, 0, query, timerange, trend, lowerIsBetter, null);
    }

    public SearchResultCountWidget(Dashboard dashboard, String query, TimeRange timerange, String description) {
        this(dashboard, query, timerange, description, false, false);
    }

    public SearchResultCountWidget(Dashboard dashboard, String id, String description, int cacheTime, String query, TimeRange timerange, boolean trend, boolean lowerIsBetter, String creatorUserId) {
        this(Type.SEARCH_RESULT_COUNT, dashboard, id, description, cacheTime, query, timerange, trend, lowerIsBetter, creatorUserId);
    }

    protected SearchResultCountWidget(Type type, Dashboard dashboard, String id, String description, int cacheTime, String query, TimeRange timerange, boolean trend, boolean lowerIsBetter, String creatorUserId) {
        super(type, id, description, cacheTime, dashboard, creatorUserId, query, timerange);

        this.trend = trend;
        this.lowerIsBetter = lowerIsBetter;
    }

    @Override
    public Map<String, Object> getConfig() {
        Map<String, Object> config = Maps.newHashMap();
        config.putAll(getTimerange().getQueryParams());
        config.put("query", getQuery());
        config.put("trend", trend);
        config.put("lower_is_better", lowerIsBetter);

        return config;
    }

    @Override
    public int getWidth() {
        return WIDTH;
    }

    @Override
    public int getHeight() {
        return HEIGHT;
    }

    @Override
    public String getStreamId() {
        return null;
    }

    @Override
    public boolean hasFixedTimeAxis() {
        return false;
    }

    public boolean getTrend() { return trend; }

    public boolean getLowerIsBetter() { return lowerIsBetter; }
}

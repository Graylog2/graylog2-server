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

public class QuickvaluesWidget extends DashboardWidget {

    private static final int DEFAULT_WIDTH = 1;
    private static final int DEFAULT_HEIGHT = 2;

    private final String field;
    private final String streamId;

    private final Boolean showPieChart;

    public QuickvaluesWidget(Dashboard dashboard, String query, TimeRange timerange, String field, String description, String streamId) {
        this(dashboard, null, description, streamId, 0, query, timerange, field, false, null);
    }

    public QuickvaluesWidget(Dashboard dashboard, String id, String description, String streamId, int cacheTime, String query, TimeRange timerange, String field, boolean showPieChart, String creatorUserId) {
        super(Type.QUICKVALUES, id, description, cacheTime, dashboard, creatorUserId, query, timerange);

        this.field = field;
        this.showPieChart = showPieChart;

        if (streamId != null && !streamId.isEmpty()) {
            this.streamId = streamId;
        } else {
            this.streamId = null;
        }
    }

    @Override
    public Map<String, Object> getConfig() {
        Map<String, Object> config = Maps.newHashMap();
        config.putAll(getTimerange().getQueryParams());
        config.put("query", getQuery());
        config.put("stream_id", streamId);
        config.put("field", field);
        config.put("show_pie_chart", showPieChart);

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
    public String getStreamId() {
        return streamId;
    }

    @Override
    public boolean hasFixedTimeAxis() {
        return false;
    }
}

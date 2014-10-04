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
package org.graylog2.restclient.models.dashboards.widgets;

import com.google.common.collect.Maps;
import org.graylog2.restclient.lib.timeranges.RelativeRange;
import org.graylog2.restclient.lib.timeranges.TimeRange;
import org.graylog2.restclient.models.dashboards.Dashboard;
import play.mvc.Call;

import java.util.Map;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class FieldChartWidget extends DashboardWidget {

    private static final int WIDTH = 2;
    private static final int HEIGHT = 1;

    private final String streamId;
    private final Map<String, Object> config;

    public FieldChartWidget(Dashboard dashboard, String query, TimeRange timerange, String description, String streamId, Map<String, Object> config) {
        this(dashboard, null, description, 0, query, timerange, streamId, config, null);
    }

    public FieldChartWidget(Dashboard dashboard, String id, String description, int cacheTime, String query, TimeRange timerange, String streamId, Map<String, Object> config, String creatorUserId) {
        super(Type.FIELD_CHART, id, description, cacheTime, dashboard, creatorUserId, query, timerange);

        this.config = config;

        if (streamId != null && !streamId.isEmpty()) {
            this.streamId = streamId;
        } else {
            this.streamId = null;
        }
    }

    @Override
    public Map<String, Object> getConfig() {
        Map<String, Object> c = Maps.newHashMap();
        c.putAll(getTimerange().getQueryParams());
        c.put("query", getQuery());
        c.put("stream_id", streamId);

        c.put("field", config.get("field"));
        c.put("valuetype", config.get("valuetype"));
        c.put("renderer", config.get("renderer"));
        c.put("interpolation", config.get("interpolation"));
        c.put("interval", config.get("interval"));

        return c;
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
        return streamId;
    }

    @Override
    public boolean hasFixedTimeAxis() {
        TimeRange timeRange = getTimerange();
        return ((timeRange.getType() != TimeRange.Type.RELATIVE) || !(((RelativeRange)timeRange).isEmptyRange()));
    }
}

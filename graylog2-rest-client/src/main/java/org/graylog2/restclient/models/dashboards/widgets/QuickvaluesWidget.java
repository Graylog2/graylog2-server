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
import org.graylog2.restclient.lib.timeranges.TimeRange;
import org.graylog2.restclient.models.dashboards.Dashboard;
import play.mvc.Call;

import java.util.Map;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class QuickvaluesWidget extends DashboardWidget {

    private static final int WIDTH = 1;
    private static final int HEIGHT = 2;

    private final String field;
    private final String streamId;

    public QuickvaluesWidget(Dashboard dashboard, String query, TimeRange timerange, String field, String description, String streamId) {
        this(dashboard, null, description, streamId, 0, query, timerange, field, null);
    }

    public QuickvaluesWidget(Dashboard dashboard, String id, String description, String streamId, int cacheTime, String query, TimeRange timerange, String field, String creatorUserId) {
        super(Type.QUICKVALUES, id, description, cacheTime, dashboard, creatorUserId, query, timerange);

        this.field = field;

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
        return streamId;
    }

    @Override
    public boolean hasFixedTimeAxis() {
        return false;
    }
}

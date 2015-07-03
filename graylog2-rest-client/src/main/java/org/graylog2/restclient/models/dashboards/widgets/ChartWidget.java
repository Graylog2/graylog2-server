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

import com.google.common.collect.ImmutableMap;
import org.graylog2.restclient.lib.timeranges.TimeRange;
import org.graylog2.restclient.models.dashboards.Dashboard;

import java.util.Map;

import static com.google.common.base.Strings.emptyToNull;

public abstract class ChartWidget extends DashboardWidget {
    protected static final int DEFAULT_WIDTH = 2;
    protected static final int DEFAULT_HEIGHT = 1;

    private final String streamId;
    private final String interval;

    protected ChartWidget(Type type, String id, String description, int cacheTime, Dashboard dashboard, String query, TimeRange timerange, String streamId, String interval) {
        this(type, id, description, cacheTime, dashboard, null, query, timerange, streamId, interval);
    }

    protected ChartWidget(Type type, String id, String description, int cacheTime, Dashboard dashboard, String creatorUserId, String query, TimeRange timerange, String streamId, String interval) {
        super(type, id, description, cacheTime, dashboard, creatorUserId, query, timerange);

        this.interval = interval;
        this.streamId = emptyToNull(streamId);
    }

    @Override
    public Map<String, Object> getConfig() {
        final ImmutableMap.Builder<String, Object> configBuilder = ImmutableMap.<String, Object>builder()
                .put("interval", interval);

        if (streamId != null) {
            configBuilder.put("stream_id", streamId);
        }

        return configBuilder.build();
    }

    @Override
    public String getStreamId() {
        return streamId;
    }
}

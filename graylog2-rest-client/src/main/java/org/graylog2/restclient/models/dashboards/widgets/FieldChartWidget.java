/**
 * The MIT License
 * Copyright (c) 2012 TORCH GmbH
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
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

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

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.graylog2.restclient.lib.timeranges.RelativeRange;
import org.graylog2.restclient.lib.timeranges.TimeRange;
import org.graylog2.restclient.models.dashboards.Dashboard;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class StackedChartWidget extends ChartWidget {

    private final String renderer;
    private final String interpolation;
    private List<Series> chartSeries;

    public StackedChartWidget(Dashboard dashboard, TimeRange timerange, String description, String streamId, String renderer, String interpolation, String interval) {
        this(dashboard, null, description, 0, timerange, streamId, renderer, interpolation, interval, null, null);
    }

    public StackedChartWidget(Dashboard dashboard, TimeRange timerange, String description, String streamId, String renderer, String interpolation, String interval, List<Map<String, Object>> series) {
        this(dashboard, null, description, 0, timerange, streamId, renderer, interpolation, interval, series, null);
    }

    public StackedChartWidget(Dashboard dashboard, String id, String description, int cacheTime, TimeRange timerange, String streamId, String renderer, String interpolation, String interval, List<Map<String, Object>> series, String creatorUserId) {
        super(Type.STACKED_CHART, id, description, cacheTime, dashboard, creatorUserId, null, timerange, streamId, interval);

        this.renderer = renderer;
        this.interpolation = interpolation;

        if (series != null) {
            this.chartSeries = new ArrayList<>(series.size());
            for (Map<String, Object> aSeries : series) {
                this.chartSeries.add(Series.fromMap(aSeries));
            }
        } else {
            this.chartSeries = Lists.newArrayList();
        }
    }

    public void addSeries(Map<String, Object> fields) {
        this.chartSeries.add(Series.fromMap(fields));
    }

    @Override
    public Map<String, Object> getConfig() {
        Map<String, Object> config = Maps.newHashMap();
        config.putAll(super.getConfig());
        config.putAll(getTimerange().getQueryParams());
        config.put("renderer", renderer);
        config.put("interpolation", interpolation);

        List<Map<String, String>> series = new ArrayList<>(chartSeries.size());
        for (Series aSeries : this.chartSeries) {
            series.add(aSeries.toMap());
        }

        config.put("series", series);

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

    private static class Series {
        final String field;
        final String query;
        final String statisticalFunction;

        static Series fromMap(Map<String, Object> fields) {
            return new Series((String) fields.get("query"), (String) fields.get("field"), (String) fields.get("statistical_function"));
        }

        Series(String query, String field, String statisticalFunction) {
            this.query = query;
            this.field = field;
            this.statisticalFunction = statisticalFunction;
        }

        Map<String, String> toMap() {
            Map<String, String> series = Maps.newHashMap();
            series.put("field", field);
            series.put("query", query);
            series.put("statistical_function", statisticalFunction);

            return series;
        }
    }
}

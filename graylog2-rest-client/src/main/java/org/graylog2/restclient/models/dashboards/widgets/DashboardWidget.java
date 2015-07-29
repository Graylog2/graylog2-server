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

import org.graylog2.rest.models.dashboards.requests.AddWidgetRequest;
import org.graylog2.restclient.lib.APIException;
import org.graylog2.restclient.lib.ApiClient;
import org.graylog2.restclient.lib.timeranges.InvalidRangeParametersException;
import org.graylog2.restclient.lib.timeranges.TimeRange;
import org.graylog2.restclient.models.api.requests.dashboards.WidgetUpdateRequest;
import org.graylog2.restclient.models.api.responses.dashboards.DashboardWidgetResponse;
import org.graylog2.restclient.models.api.responses.dashboards.DashboardWidgetValueResponse;
import org.graylog2.restclient.models.dashboards.Dashboard;
import org.graylog2.restroutes.generated.routes;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public abstract class DashboardWidget {

    public enum Type {
        SEARCH_RESULT_COUNT,
        STREAM_SEARCH_RESULT_COUNT,
        FIELD_CHART,
        QUICKVALUES,
        SEARCH_RESULT_CHART,
        STATS_COUNT,
        STACKED_CHART
    }

    private final Type type;
    private final String id;
    private final String description;
    private final Dashboard dashboard;
    private final int cacheTime;
    private final String creatorUserId;

    private final String query;
    private final TimeRange timerange;

    private int col = 1;
    private int row = 1;

    private int height = 0;
    private int width = 0;

    protected DashboardWidget(Type type, String id, String description, int cacheTime, Dashboard dashboard, String query, TimeRange timeRange) {
        this(type, id, description, cacheTime, dashboard, null, query, timeRange);
    }

    protected DashboardWidget(Type type, String id, String description, int cacheTime, Dashboard dashboard, String creatorUserId, String query, TimeRange timeRange) {
        this.type = type;
        this.id = id;
        this.description = description;
        this.dashboard = dashboard;
        this.cacheTime = cacheTime;
        this.creatorUserId = creatorUserId;
        this.query = query;
        this.timerange = timeRange;
    }

    public Type getType() {
        return type;
    }

    public String getId() {
        return id;
    }

    public String getDescription() {
        return (description == null || description.isEmpty() ? "Description" : description);
    }

    public Dashboard getDashboard() {
        return dashboard;
    }

    public int getCacheTime() {
        return cacheTime;
    }

    public void setCol(int col) {
        this.col = col;
    }

    public void setRow(int row) {
        this.row = row;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public DashboardWidgetValueResponse getValue(ApiClient api) throws APIException, IOException {
        return api.path(routes.DashboardsResource().widgetValue(dashboard.getId(), id),  DashboardWidgetValueResponse.class)
                    .onlyMasterNode()
                    .execute();
    }

    public void updateWidget(ApiClient api, AddWidgetRequest addWidgetRequest) throws APIException, IOException {
        api.path(routes.DashboardsResource().updateWidget(dashboard.getId(), id))
                .body(addWidgetRequest)
                .onlyMasterNode()
                .execute();
    };

    public void updateDescription(ApiClient api, String newDescription) throws APIException, IOException {
        WidgetUpdateRequest wur = new WidgetUpdateRequest();
        wur.description = newDescription;

        api.path(routes.DashboardsResource().updateDescription(dashboard.getId(), id))
                .body(wur)
                .onlyMasterNode()
                .execute();
    }

    public void updateCacheTime(ApiClient api, int cacheTime) throws APIException, IOException {
        WidgetUpdateRequest wur = new WidgetUpdateRequest();
        wur.cacheTime = cacheTime;

        api.path(routes.DashboardsResource().updateCacheTime(dashboard.getId(), id))
                .body(wur)
                .onlyMasterNode()
                .execute();
    }

    public static DashboardWidget factory(Dashboard dashboard, DashboardWidgetResponse w) throws NoSuchWidgetTypeException, InvalidRangeParametersException {
        Type type;
        try {
            type = Type.valueOf(w.type.toUpperCase(Locale.ENGLISH));
        } catch(IllegalArgumentException e) {
            throw new NoSuchWidgetTypeException();
        }

        DashboardWidget widget;

        switch (type) {
            case SEARCH_RESULT_COUNT:
                widget = new SearchResultCountWidget(
                        dashboard,
                        w.id,
                        w.description,
                        w.cacheTime,
                        (String) w.config.get("query"),
                        TimeRange.factory((Map<String, Object>) w.config.get("timerange")),
                        w.config.get("trend") != null && Boolean.parseBoolean(String.valueOf(w.config.get("trend"))),
                        w.config.get("lower_is_better") != null && Boolean.parseBoolean(String.valueOf(w.config.get("lower_is_better"))),
                        w.creatorUserId
                );
                break;
            case STREAM_SEARCH_RESULT_COUNT:
                widget = new StreamSearchResultCountWidget(
                        dashboard,
                        w.id,
                        w.description,
                        w.cacheTime,
                        (String) w.config.get("query"),
                        TimeRange.factory((Map<String, Object>) w.config.get("timerange")),
                        w.config.get("trend") != null && Boolean.parseBoolean(String.valueOf(w.config.get("trend"))),
                        w.config.get("lower_is_better") != null && Boolean.parseBoolean(String.valueOf(w.config.get("lower_is_better"))),
                        (String) w.config.get("stream_id"),
                        w.creatorUserId
                );
                break;
            case FIELD_CHART:
                widget = new FieldChartWidget(
                        dashboard,
                        w.id,
                        w.description,
                        w.cacheTime,
                        (String) w.config.get("query"),
                        TimeRange.factory((Map<String, Object>) w.config.get("timerange")),
                        (String) w.config.get("stream_id"),
                        w.config,
                        w.creatorUserId
                );
                break;
            case QUICKVALUES:
                widget = new QuickvaluesWidget(
                        dashboard,
                        w.id,
                        w.description,
                        (String) w.config.get("stream_id"),
                        w.cacheTime,
                        (String) w.config.get("query"),
                        TimeRange.factory((Map<String, Object>) w.config.get("timerange")),
                        (String) w.config.get("field"),
                        w.config.get("show_pie_chart") != null && Boolean.parseBoolean(String.valueOf(w.config.get("show_pie_chart"))),
                        !w.config.containsKey("show_data_table") || Boolean.parseBoolean(String.valueOf(w.config.get("show_data_table"))),
                        w.creatorUserId
                );
                break;
            case SEARCH_RESULT_CHART:
                widget = new SearchResultChartWidget(
                        dashboard,
                        w.id,
                        w.description,
                        (String) w.config.get("stream_id"),
                        w.cacheTime,
                        (String) w.config.get("query"),
                        TimeRange.factory((Map<String, Object>) w.config.get("timerange")),
                        w.creatorUserId,
                        (String) w.config.get("interval")
                );
                break;
            case STATS_COUNT:
                widget = new StatisticalCountWidget(
                        dashboard,
                        w.id,
                        w.description,
                        w.cacheTime,
                        (String) w.config.get("query"),
                        TimeRange.factory((Map<String, Object>) w.config.get("timerange")),
                        w.config.get("trend") != null && Boolean.parseBoolean(String.valueOf(w.config.get("trend"))),
                        w.config.get("lower_is_better") != null && Boolean.parseBoolean(String.valueOf(w.config.get("lower_is_better"))),
                        (String) w.config.get("field"),
                        (String) w.config.get("stats_function"),
                        (w.config.containsKey("stream_id") ? (String) w.config.get("stream_id") : null),
                        w.creatorUserId
                );
                break;
            case STACKED_CHART:
                widget = new StackedChartWidget(
                        dashboard,
                        w.id,
                        w.description,
                        w.cacheTime,
                        TimeRange.factory((Map<String, Object>) w.config.get("timerange")),
                        (String) w.config.get("stream_id"),
                        (String) w.config.get("renderer"),
                        (String) w.config.get("interpolation"),
                        (String) w.config.get("interval"),
                        (List<Map<String, Object>>) w.config.get("series"),
                        w.creatorUserId
                );
                break;
            default:
                throw new NoSuchWidgetTypeException();
        }
        // Read and set positions. Defaults to 0, which is then rescued by the JS dashboard library.
        if (dashboard.getPositions().containsKey(w.id)) {
            widget.setCol(dashboard.getPositions().get(w.id).col);
            widget.setRow(dashboard.getPositions().get(w.id).row);
            widget.setHeight(dashboard.getPositions().get(w.id).height);
            widget.setWidth(dashboard.getPositions().get(w.id).width);
        }

        return widget;
    }

    public abstract Map<String, Object> getConfig();
    public int getWidth() {
        return width;
    };
    public int getHeight() {
        return height;
    };
    public abstract String getStreamId();

    /* Indicate if the representation should contain the whole searched time range */
    public abstract boolean hasFixedTimeAxis();

    public String getCreatorUserId() {
        return creatorUserId;
    }

    public int getCol() {
        return col;
    }

    public int getRow() {
        return row;
    }

    public String getQuery() {
        return query;
    }

    public TimeRange getTimerange() {
        return timerange;
    }

    public static class NoSuchWidgetTypeException extends Throwable {
    }
}

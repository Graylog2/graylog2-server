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

import org.graylog2.restclient.lib.APIException;
import org.graylog2.restclient.lib.ApiClient;
import org.graylog2.restclient.lib.DateTools;
import org.graylog2.restclient.lib.timeranges.InvalidRangeParametersException;
import org.graylog2.restclient.lib.timeranges.TimeRange;
import org.graylog2.restclient.models.api.requests.dashboards.WidgetUpdateRequest;
import org.graylog2.restclient.models.api.responses.dashboards.DashboardWidgetResponse;
import org.graylog2.restclient.models.api.responses.dashboards.DashboardWidgetValueResponse;
import org.graylog2.restclient.models.dashboards.Dashboard;
import org.graylog2.restroutes.generated.routes;
import org.joda.time.DateTime;
import play.mvc.Call;

import java.io.IOException;
import java.util.Map;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public abstract class DashboardWidget {

    public enum Type {
        SEARCH_RESULT_COUNT,
        STREAM_SEARCH_RESULT_COUNT,
        FIELD_CHART,
        QUICKVALUES,
        SEARCH_RESULT_CHART
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

    public DashboardWidgetValueResponse getValue(ApiClient api) throws APIException, IOException {
        return api.path(routes.DashboardsResource().widgetValue(dashboard.getId(), id),  DashboardWidgetValueResponse.class)
                    .onlyMasterNode()
                    .execute();
    }

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
            type = Type.valueOf(w.type.toUpperCase());
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
                        (w.config.containsKey("stream_id") ? (String) w.config.get("stream_id") : null),
                        w.config,
                        w.creatorUserId
                );
                break;
            case QUICKVALUES:
                widget = new QuickvaluesWidget(
                        dashboard,
                        w.id,
                        w.description,
                        (w.config.containsKey("stream_id") ? (String) w.config.get("stream_id") : null),
                        w.cacheTime,
                        (String) w.config.get("query"),
                        TimeRange.factory((Map<String, Object>) w.config.get("timerange")),
                        (String) w.config.get("field"),
                        w.creatorUserId
                );
                break;
            case SEARCH_RESULT_CHART:
                widget = new SearchResultChartWidget(
                        dashboard,
                        w.id,
                        w.description,
                        (w.config.containsKey("stream_id") ? (String) w.config.get("stream_id") : null),
                        w.cacheTime,
                        (String) w.config.get("query"),
                        TimeRange.factory((Map<String, Object>) w.config.get("timerange")),
                        w.creatorUserId,
                        (String) w.config.get("interval")
                );
                break;
            default:
                throw new NoSuchWidgetTypeException();
        }
        // Read and set positions. Defaults to 1, which is then rescued by the JS dashboard library.
        if (dashboard.getPositions().containsKey(w.id)) {
            widget.setCol(dashboard.getPositions().get(w.id).col);
            widget.setRow(dashboard.getPositions().get(w.id).row);
        }

        return widget;
    }

    public abstract Map<String, Object> getConfig();
    public abstract int getWidth();
    public abstract int getHeight();
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

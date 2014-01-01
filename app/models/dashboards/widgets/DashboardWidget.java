/**
 * Copyright 2013 Lennart Koopmann <lennart@torch.sh>
 *
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
 *
 */
package models.dashboards.widgets;

import controllers.routes;
import lib.APIException;
import lib.ApiClient;
import lib.DateTools;
import lib.timeranges.InvalidRangeParametersException;
import lib.timeranges.TimeRange;
import models.Stream;
import models.api.requests.dashboards.WidgetUpdateRequest;
import models.api.responses.dashboards.DashboardWidgetResponse;
import models.api.responses.dashboards.DashboardWidgetValueResponse;
import models.dashboards.Dashboard;
import org.joda.time.DateTime;
import play.Logger;
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
        FIELD_CHART
    }

    private final Type type;
    private final String id;
    private final String description;
    private final Dashboard dashboard;
    private final int cacheTime;
    private final String creatorUserId;

    private int col = 1;
    private int row = 1;

    protected DashboardWidget(Type type, String id, String description, int cacheTime, Dashboard dashboard) {
        this(type, id, description, cacheTime, dashboard, null);
    }

    protected DashboardWidget(Type type, String id, String description, int cacheTime, Dashboard dashboard, String creatorUserId) {
        this.type = type;
        this.id = id;
        this.description = description;
        this.dashboard = dashboard;
        this.cacheTime = cacheTime;
        this.creatorUserId = creatorUserId;
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
        return api.get(DashboardWidgetValueResponse.class)
                    .path("/dashboards/{0}/widgets/{1}/value", dashboard.getId(), id)
                    .onlyMasterNode()
                    .execute();
    }

    public void updateDescription(ApiClient api, String newDescription) throws APIException, IOException {
        WidgetUpdateRequest wur = new WidgetUpdateRequest();
        wur.description = newDescription;

        api.put().path("/dashboards/{0}/widgets/{1}/description", dashboard.getId(), id)
                .body(wur)
                .onlyMasterNode()
                .execute();
    }

    public void updateCacheTime(ApiClient api, int cacheTime) throws APIException, IOException {
        WidgetUpdateRequest wur = new WidgetUpdateRequest();
        wur.cacheTime = cacheTime;

        api.put().path("/dashboards/{0}/widgets/{1}/cachetime", dashboard.getId(), id)
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
    public abstract Call replayRoute();
    public abstract int getWidth();
    public abstract int getHeight();

    protected Call prepareNonStreamBoundReplayRoute(String query, TimeRange timerange) {
        return routes.SearchController.index(
                (query == null) ? "" : query,
                timerange.getType().name().toLowerCase(),
                timerange.getQueryParams().containsKey("range") ? Integer.valueOf(timerange.nullSafeParam("range")) : 0,
                timerange.getQueryParams().containsKey("from") ? new DateTime(timerange.nullSafeParam("from")).toString(DateTools.SHORT_DATE_FORMAT) : "",
                timerange.getQueryParams().containsKey("to") ? new DateTime(timerange.nullSafeParam("to")).toString(DateTools.SHORT_DATE_FORMAT) : "",
                timerange.nullSafeParam("keyword"),
                "minute",
                0,
                ""
        );
    }

    protected Call prepareStreamBoundReplayRoute(String streamId, String query, TimeRange timerange) {
        return routes.StreamSearchController.index(
                streamId,
                (query == null) ? "" : query,
                timerange.getType().name().toLowerCase(),
                timerange.getQueryParams().containsKey("range") ? Integer.valueOf(timerange.nullSafeParam("range")) : 0,
                timerange.getQueryParams().containsKey("from") ? new DateTime(timerange.nullSafeParam("from")).toString(DateTools.SHORT_DATE_FORMAT) : "",
                timerange.getQueryParams().containsKey("to") ? new DateTime(timerange.nullSafeParam("to")).toString(DateTools.SHORT_DATE_FORMAT) : "",
                timerange.nullSafeParam("keyword"),
                "minute",
                0,
                ""
        );
    }

    public String getCreatorUserId() {
        return creatorUserId;
    }

    public int getCol() {
        return col;
    }

    public int getRow() {
        return row;
    }

    public static class NoSuchWidgetTypeException extends Throwable {
    }
}

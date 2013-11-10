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

import lib.APIException;
import lib.ApiClient;
import lib.timeranges.InvalidRangeParametersException;
import lib.timeranges.TimeRange;
import models.api.requests.dashboards.WidgetUpdateRequest;
import models.api.responses.dashboards.DashboardWidgetResponse;
import models.api.responses.dashboards.DashboardWidgetValueResponse;
import models.dashboards.Dashboard;

import java.io.IOException;
import java.util.Map;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public abstract class DashboardWidget {

    public enum Type {
        SEARCH_RESULT_COUNT
    }

    private final Type type;
    private final String id;
    private final String description;
    private final Dashboard dashboard;
    private final int cacheTime;

    protected DashboardWidget(Type type, String id, String description, int cacheTime, Dashboard dashboard) {
        this.type = type;
        this.id = id;
        this.description = description;
        this.dashboard = dashboard;
        this.cacheTime = cacheTime;
    }

    public Type getType() {
        return type;
    }

    public String getId() {
        return id;
    }

    public String getDescription() {
        return (description == null ? "Description" : description);
    }

    public Dashboard getDashboard() {
        return dashboard;
    }

    public int getCacheTime() {
        return cacheTime;
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

        switch (type) {
            case SEARCH_RESULT_COUNT:
                return new SearchResultCountWidget(
                        dashboard,
                        w.id,
                        w.description,
                        w.cacheTime,
                        (String) w.config.get("query"),
                        TimeRange.factory((Map<String, Object>) w.config.get("timerange"))
                );
            default:
                throw new NoSuchWidgetTypeException();
        }
    }

    public abstract Map<String, Object> getConfig();

    public static class NoSuchWidgetTypeException extends Throwable {
    }
}

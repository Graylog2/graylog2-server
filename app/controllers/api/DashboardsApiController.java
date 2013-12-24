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
package controllers.api;

import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.inject.Inject;
import controllers.AuthenticatedController;
import lib.APIException;
import lib.ApiClient;
import lib.timeranges.InvalidRangeParametersException;
import lib.timeranges.TimeRange;
import models.dashboards.Dashboard;
import models.dashboards.DashboardService;
import models.NodeService;
import models.dashboards.widgets.DashboardWidget;
import models.dashboards.widgets.SearchResultCountWidget;
import models.dashboards.widgets.StreamSearchResultCountWidget;
import play.Logger;
import play.mvc.Result;

import java.io.IOException;
import java.util.Map;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class DashboardsApiController extends AuthenticatedController {

    @Inject
    private NodeService nodeService;

    @Inject
    private DashboardService dashboardService;

    public Result index() {
        try {
            Map<String, Object> result = Maps.newHashMap();
            for (Dashboard d : dashboardService.getAll()) {
                Map<String, String> dashboard = Maps.newHashMap();

                dashboard.put("title", d.getTitle());
                dashboard.put("description", d.getDescription());
                dashboard.put("created_by", d.getCreatorUser().getName());

                result.put(d.getId(), dashboard);
            }

            return ok(new Gson().toJson(result)).as("application/json");
        } catch (APIException e) {
            String message = "Could not get dashboards. We expected HTTP 200, but got a HTTP " + e.getHttpCode() + ".";
            return status(504, views.html.errors.error.render(message, e, request()));
        } catch (IOException e) {
            return status(504, views.html.errors.error.render(ApiClient.ERROR_MSG_IO, e, request()));
        }
    }

    public Result widgetValue(String dashboardId, String widgetId) {
        try {
            Dashboard dashboard = dashboardService.get(dashboardId);
            DashboardWidget widget = dashboard.getWidget(widgetId);

            Map<String, Object> result = Maps.newHashMap();
            result.put("result", widget.getValue(api()).result);
            result.put("took_ms", widget.getValue(api()).tookMs);
            result.put("calculated_at", widget.getValue(api()).calculatedAt);

            return ok(new Gson().toJson(result)).as("application/json");
        } catch (APIException e) {
            String message = "Could not get dashboard. We expected HTTP 200, but got a HTTP " + e.getHttpCode() + ".";
            return status(504, views.html.errors.error.render(message, e, request()));
        } catch (IOException e) {
            return status(504, views.html.errors.error.render(ApiClient.ERROR_MSG_IO, e, request()));
        }
    }

    public Result addWidget(String dashboardId) {
        try {
            Map<String, String> params = flattenFormUrlEncoded(request().body().asFormUrlEncoded());
            String query = params.get("query");
            String rangeType = params.get("rangeType");

            Dashboard dashboard = dashboardService.get(dashboardId);

            // Determine timerange type.
            TimeRange timerange;
            try {
                int relative = 0;
                if (params.get("relative") != null) {
                    relative = Integer.parseInt(params.get("relative"));
                }

                timerange = TimeRange.factory(rangeType, relative, params.get("from"), params.get("to"), params.get("keyword"));
            } catch(InvalidRangeParametersException e2) {
                return status(400, views.html.errors.error.render("Invalid range parameters provided.", e2, request()));
            } catch(IllegalArgumentException e1) {
                return status(400, views.html.errors.error.render("Invalid range type provided.", e1, request()));
            }

            DashboardWidget widget;
            try {
                switch (DashboardWidget.Type.valueOf(params.get("widgetType"))) {
                    case SEARCH_RESULT_COUNT:
                        widget = new SearchResultCountWidget(dashboard, query, timerange);
                        break;
                    case STREAM_SEARCH_RESULT_COUNT:
                        widget = new StreamSearchResultCountWidget(dashboard, query, timerange, params.get("streamId"));
                        break;
                    default:
                        throw new IllegalArgumentException();
                }
            } catch (IllegalArgumentException e) {
                Logger.error("No such widget type: " + params.get("widgetType"));
                return badRequest();
            }

            dashboard.addWidget(widget, currentUser());

            return created();
        } catch (APIException e) {
            String message = "Could not add widget. We got a HTTP " + e.getHttpCode() + ".";
            return status(504, views.html.errors.error.render(message, e, request()));
        } catch (IOException e) {
            return status(504, views.html.errors.error.render(ApiClient.ERROR_MSG_IO, e, request()));
        }
    }

    public Result removeWidget(String dashboardId, String widgetId) {
        try {
            Dashboard dashboard = dashboardService.get(dashboardId);
            dashboard.removeWidget(widgetId);

            return noContent();
        } catch (APIException e) {
            String message = "Could not get dashboard. We expected HTTP 200, but got a HTTP " + e.getHttpCode() + ".";
            return status(504, views.html.errors.error.render(message, e, request()));
        } catch (IOException e) {
            return status(504, views.html.errors.error.render(ApiClient.ERROR_MSG_IO, e, request()));
        }
    }

    public Result updateWidgetDescription(String dashboardId, String widgetId) {
        String newDescription = flattenFormUrlEncoded(request().body().asFormUrlEncoded()).get("description");

        if (newDescription == null || newDescription.trim().isEmpty()) {
            return badRequest();
        }

        try {
            Dashboard dashboard = dashboardService.get(dashboardId);
            DashboardWidget widget = dashboard.getWidget(widgetId);

            widget.updateDescription(api(), newDescription.trim());

            return ok().as("application/json");
        } catch (APIException e) {
            String message = "Could not get widget. We expected HTTP 200, but got a HTTP " + e.getHttpCode() + ".";
            return status(504, views.html.errors.error.render(message, e, request()));
        } catch (IOException e) {
            return status(504, views.html.errors.error.render(ApiClient.ERROR_MSG_IO, e, request()));
        }
    }

    public Result updateWidgetCacheTime(String dashboardId, String widgetId) {
        String newCacheTimeS = flattenFormUrlEncoded(request().body().asFormUrlEncoded()).get("cacheTime");

        if (newCacheTimeS == null) {
            return badRequest();
        }

        int newCacheTime;
        try {
            newCacheTime = Integer.parseInt(newCacheTimeS);
        } catch (NumberFormatException e) {
            return badRequest();
        }

        try {
            Dashboard dashboard = dashboardService.get(dashboardId);
            DashboardWidget widget = dashboard.getWidget(widgetId);

            widget.updateCacheTime(api(), newCacheTime);

            return ok().as("application/json");
        } catch (APIException e) {
            String message = "Could not get widget. We expected HTTP 200, but got a HTTP " + e.getHttpCode() + ".";
            return status(504, views.html.errors.error.render(message, e, request()));
        } catch (IOException e) {
            return status(504, views.html.errors.error.render(ApiClient.ERROR_MSG_IO, e, request()));
        }
    }

}

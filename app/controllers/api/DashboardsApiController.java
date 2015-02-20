/**
 * Copyright 2012-2015 TORCH GmbH, 2015 Graylog, Inc.
 *
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
 *
 */
package controllers.api;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import controllers.AuthenticatedController;
import lib.security.RestPermissions;
import org.graylog2.restclient.lib.APIException;
import org.graylog2.restclient.lib.ApiClient;
import org.graylog2.restclient.lib.timeranges.InvalidRangeParametersException;
import org.graylog2.restclient.lib.timeranges.TimeRange;
import org.graylog2.restclient.models.User;
import org.graylog2.restclient.models.api.requests.dashboards.UserSetWidgetPositionsRequest;
import org.graylog2.restclient.models.api.responses.dashboards.DashboardWidgetValueResponse;
import org.graylog2.restclient.models.dashboards.Dashboard;
import org.graylog2.restclient.models.dashboards.DashboardService;
import org.graylog2.restclient.models.dashboards.widgets.DashboardWidget;
import org.graylog2.restclient.models.dashboards.widgets.FieldChartWidget;
import org.graylog2.restclient.models.dashboards.widgets.QuickvaluesWidget;
import org.graylog2.restclient.models.dashboards.widgets.SearchResultChartWidget;
import org.graylog2.restclient.models.dashboards.widgets.SearchResultCountWidget;
import org.graylog2.restclient.models.dashboards.widgets.StatisticalCountWidget;
import org.graylog2.restclient.models.dashboards.widgets.StreamSearchResultCountWidget;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Days;
import org.joda.time.Duration;
import org.joda.time.Hours;
import org.joda.time.Minutes;
import org.joda.time.MutableDateTime;
import org.joda.time.Weeks;
import play.Logger;
import play.libs.Json;
import play.mvc.BodyParser;
import play.mvc.Http;
import play.mvc.Result;
import views.helpers.Permissions;

import javax.inject.Inject;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DashboardsApiController extends AuthenticatedController {
    private final DashboardService dashboardService;

    @Inject
    public DashboardsApiController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    public Result index() {
        try {
            Map<String, Object> result = Maps.newHashMap();
            for (Dashboard d : dashboardService.getAll()) {
                Map<String, String> dashboard = Maps.newHashMap();

                dashboard.put("title", d.getTitle());
                dashboard.put("description", d.getDescription());
                dashboard.put("created_by", (d.getCreatorUser() == null) ? null : d.getCreatorUser().getName());

                result.put(d.getId(), dashboard);
            }

            return ok(Json.toJson(result));
        } catch (APIException e) {
            String message = "Could not get dashboards. We expected HTTP 200, but got a HTTP " + e.getHttpCode() + ".";
            return status(504, views.html.errors.error.render(message, e, request()));
        } catch (IOException e) {
            return status(504, views.html.errors.error.render(ApiClient.ERROR_MSG_IO, e, request()));
        }
    }

    public Result listWritable() {
        try {
            Map<String, Object> result = Maps.newHashMap();
            for (Dashboard d : getAllWritable(currentUser())) {
                Map<String, String> dashboard = Maps.newHashMap();

                dashboard.put("title", d.getTitle());
                dashboard.put("description", d.getDescription());
                dashboard.put("created_by", (d.getCreatorUser() == null) ? null : d.getCreatorUser().getName());

                result.put(d.getId(), dashboard);
            }

            return ok(Json.toJson(result));
        } catch (APIException e) {
            String message = "Could not get dashboards. We expected HTTP 200, but got a HTTP " + e.getHttpCode() + ".";
            return status(504, views.html.errors.error.render(message, e, request()));
        } catch (IOException e) {
            return status(504, views.html.errors.error.render(ApiClient.ERROR_MSG_IO, e, request()));
        }
    }

    private Iterable<? extends Dashboard> getAllWritable(User user) throws IOException, APIException {
        List<Dashboard> writable = Lists.newArrayList();

        for (Dashboard dashboard : dashboardService.getAll()) {
            if (Permissions.isPermitted(user, RestPermissions.DASHBOARDS_EDIT, dashboard.getId())) {
                writable.add(dashboard);
            }
        }

        return writable;
    }

    @BodyParser.Of(BodyParser.Json.class)
    public Result setWidgetPositions(String dashboardId) {

        try {
            Dashboard dashboard = dashboardService.get(dashboardId);
            UserSetWidgetPositionsRequest positions = Json.fromJson(request().body().asJson(), UserSetWidgetPositionsRequest.class);
            dashboard.setWidgetPositions(positions.positions);
        } catch (APIException e) {
            String message = "Could not update positions. We expected HTTP 200, but got a HTTP " + e.getHttpCode() + ".";
            return status(504, views.html.errors.error.render(message, e, request()));
        } catch (IOException e) {
            return status(504, views.html.errors.error.render(ApiClient.ERROR_MSG_IO, e, request()));
        }

        return ok();
    }

    public Result widget(String dashboardId, String widgetId) {
        try {
            Dashboard dashboard = dashboardService.get(dashboardId);
            DashboardWidget widget = dashboard.getWidget(widgetId);

            Map<String, Object> result = Maps.newHashMap();
            result.put("type", widget.getType());
            result.put("id", widget.getId());
            result.put("dashboard_id", widget.getDashboard().getId());
            result.put("description", widget.getDescription());
            result.put("cache_time", widget.getCacheTime());
            result.put("creator_user_id", widget.getCreatorUserId());
            result.putAll(widget.getConfig());

            return ok(Json.toJson(result));
        } catch (APIException e) {
            String message = "Could not get dashboard. We expected HTTP 200, but got a HTTP " + e.getHttpCode() + ".";
            return status(504, views.html.errors.error.render(message, e, request()));
        } catch (IOException e) {
            return status(504, views.html.errors.error.render(ApiClient.ERROR_MSG_IO, e, request()));
        }
    }

    public Result widgetValue(String dashboardId, String widgetId, int resolution) {
        try {
            Dashboard dashboard = dashboardService.get(dashboardId);
            DashboardWidget widget = dashboard.getWidget(widgetId);
            DashboardWidgetValueResponse widgetValue = widget.getValue(api());

            Object resultValue;
            if (widget instanceof SearchResultChartWidget) {
                resultValue = formatWidgetValueResults(resolution, widget, widgetValue);
            } else {
                resultValue = widgetValue.result;
            }

            Map<String, Object> result = Maps.newHashMap();
            result.put("result", resultValue);
            result.put("took_ms", widgetValue.tookMs);
            result.put("calculated_at", widgetValue.calculatedAt);
            result.put("time_range", widgetValue.computationTimeRange);

            return ok(Json.toJson(result));
        } catch (APIException e) {
            String message = "Could not get dashboard. We expected HTTP 200, but got a HTTP " + e.getHttpCode() + ".";
            return status(504, views.html.errors.error.render(message, e, request()));
        } catch (IOException e) {
            return status(504, views.html.errors.error.render(ApiClient.ERROR_MSG_IO, e, request()));
        }
    }

    protected Map<String, Long> formatWidgetValueResults(final int maxDataPoints,
                                                               final DashboardWidget widget,
                                                               final DashboardWidgetValueResponse widgetValue) {
        final Map<String, Object> widgetConfig = widget.getConfig();
        final String interval = widgetConfig.containsKey("interval") ? (String) widgetConfig.get("interval") : "minute";
        final boolean allQuery = widgetConfig.get("range_type").equals("relative") && widgetConfig.get("range").equals("0");

        return formatWidgetValueResults(maxDataPoints,
                widgetValue.result,
                interval,
                widgetValue.computationTimeRange,
                allQuery);
    }

    // TODO: Extract common parts of this and the similar method on SearchApiController
    protected Map<String, Long> formatWidgetValueResults(final int maxDataPoints,
                                                         final Object resultValue,
                                                         final String interval,
                                                         final Map<String, Object> timeRange,
                                                         final boolean allQuery) {
        final Map<String, Long> points = Maps.newHashMap();

        if (resultValue instanceof Map) {
            final Map<?, ?> resultMap = (Map) resultValue;

            DateTime from;
            if (allQuery) {
                String firstTimestamp = (String) resultMap.entrySet().iterator().next().getKey();
                from = new DateTime(Long.parseLong(firstTimestamp) * 1000, DateTimeZone.UTC);
            } else {
                from = DateTime.parse((String) timeRange.get("from")).withZone(DateTimeZone.UTC);
            }
            final DateTime to = DateTime.parse((String) timeRange.get("to"));
            final MutableDateTime currentTime = new MutableDateTime(from);

            final Duration step = estimateIntervalStep(interval);
            final int dataPoints = (int) ((to.getMillis() - from.getMillis()) / step.getMillis());

            // using the absolute value guarantees, that there will always be enough values for the given resolution
            final int factor = (maxDataPoints != -1 && dataPoints > maxDataPoints) ? dataPoints / maxDataPoints : 1;

            int index = 0;
            floorToBeginningOfInterval(interval, currentTime);
            while (currentTime.isBefore(to) || currentTime.isEqual(to)) {
                if (index % factor == 0) {
                    String timestamp = Long.toString(currentTime.getMillis() / 1000);
                    Object value = resultMap.get(timestamp);
                    Long result = value == null ? 0L : Long.parseLong(String.valueOf(value));
                    points.put(timestamp, result);
                }
                index++;
                nextStep(interval, currentTime);
            }
        }
        return points;
    }

    private void nextStep(String interval, MutableDateTime currentTime) {
        switch (interval) {
            case "minute":
                currentTime.addMinutes(1);
                break;
            case "hour":
                currentTime.addHours(1);
                break;
            case "day":
                currentTime.addDays(1);
                break;
            case "week":
                currentTime.addWeeks(1);
                break;
            case "month":
                currentTime.addMonths(1);
                break;
            case "quarter":
                currentTime.addMonths(3);
                break;
            case "year":
                currentTime.addYears(1);
                break;
            default:
                throw new IllegalArgumentException("Invalid duration specified: " + interval);
        }
    }

    private void floorToBeginningOfInterval(String interval, MutableDateTime currentTime) {
        switch (interval) {
            case "minute":
                currentTime.minuteOfDay().roundFloor();
                break;
            case "hour":
                currentTime.hourOfDay().roundFloor();
                break;
            case "day":
                currentTime.dayOfMonth().roundFloor();
                break;
            case "week":
                currentTime.weekOfWeekyear().roundFloor();
                break;
            case "month":
                currentTime.monthOfYear().roundFloor();
                break;
            case "quarter":
                // set the month to the beginning of the quarter
                int currentQuarter = ((currentTime.getMonthOfYear() - 1) / 3);
                int startOfQuarter = (currentQuarter * 3) + 1;
                currentTime.setMonthOfYear(startOfQuarter);
                currentTime.monthOfYear().roundFloor();
                break;
            case "year":
                currentTime.yearOfCentury().roundFloor();
                break;
            default:
                throw new IllegalArgumentException("Invalid duration specified: " + interval);
        }
    }

    private Duration estimateIntervalStep(String interval) {
        Duration step;
        switch (interval) {
            case "minute":
                step = Minutes.ONE.toStandardDuration();
                break;
            case "hour":
                step = Hours.ONE.toStandardDuration();
                break;
            case "day":
                step = Days.ONE.toStandardDuration();
                break;
            case "week":
                step = Weeks.ONE.toStandardDuration();
                break;
            case "month":
                step = Days.days(31).toStandardDuration();
                break;
            case "quarter":
                step = Days.days(31 * 3).toStandardDuration();
                break;
            case "year":
                step = Days.days(365).toStandardDuration();
                break;
            default:
                throw new IllegalArgumentException("Invalid duration specified: " + interval);
        }
        return step;
    }

    @BodyParser.Of(BodyParser.FormUrlEncoded.class)
    public Result addWidget(String dashboardId) {
        try {
            final Map<String, String> params = flattenFormUrlEncoded(request().body().asFormUrlEncoded());

            String query = params.get("query");
            String rangeType = params.get("rangeType");
            String description = params.get("description");

            Dashboard dashboard = dashboardService.get(dashboardId);

            // Determine timerange type.
            TimeRange timerange;
            try {
                int relative = 0;
                if (params.get("relative") != null) {
                    relative = Integer.parseInt(params.get("relative"));
                }

                timerange = TimeRange.factory(rangeType, relative, params.get("from"), params.get("to"), params.get("keyword"));
            } catch (InvalidRangeParametersException e2) {
                return status(400, views.html.errors.error.render("Invalid range parameters provided.", e2, request()));
            } catch (IllegalArgumentException e1) {
                return status(400, views.html.errors.error.render("Invalid range type provided.", e1, request()));
            }

            String streamId;
            if (params.containsKey("streamId")) {
                streamId = params.get("streamId");
            } else {
                streamId = params.get("streamid");
            }

            final DashboardWidget widget;
            try {
                final DashboardWidget.Type widgetType = DashboardWidget.Type.valueOf(params.get("widgetType"));
                switch (widgetType) {
                    case SEARCH_RESULT_COUNT: {
                        final Boolean trend = Boolean.parseBoolean(params.get("trend"));
                        if (trend) {
                            if (!rangeType.equals("relative")) {
                                Logger.error("Cannot add search result count widget with trend on a non relative time range");
                                return badRequest();
                            }
                            final Boolean lowerIsBetter = Boolean.parseBoolean(params.get("lowerIsBetter"));
                            widget = new SearchResultCountWidget(dashboard, query, timerange, description, trend, lowerIsBetter);
                        } else {
                            widget = new SearchResultCountWidget(dashboard, query, timerange, description);
                        }
                        break;
                    }
                    case STREAM_SEARCH_RESULT_COUNT: {
                        if (!canReadStream(streamId)) return unauthorized();
                        final Boolean trend = Boolean.parseBoolean(params.get("trend"));
                        if (trend) {
                            if (!rangeType.equals("relative")) {
                                Logger.error("Cannot add search result count widget with trend on a non relative time range");
                                return badRequest();
                            }
                            final Boolean lowerIsBetter = Boolean.parseBoolean(params.get("lowerIsBetter"));
                            widget = new StreamSearchResultCountWidget(dashboard, query, timerange, description, trend, lowerIsBetter, streamId);
                        } else {
                            widget = new StreamSearchResultCountWidget(dashboard, query, timerange, description, streamId);
                        }
                        break;
                    }
                    case FIELD_CHART:
                        Map<String, Object> config = new HashMap<String, Object>() {{
                            put("field", params.get("field"));
                            put("valuetype", params.get("valuetype"));
                            put("renderer", params.get("renderer"));
                            put("interpolation", params.get("interpolation"));
                            put("interval", params.get("interval"));
                        }};
                        if (!canReadStream(streamId)) return unauthorized();

                        widget = new FieldChartWidget(dashboard, query, timerange, description, streamId, config);
                        break;
                    case QUICKVALUES:
                        if (!canReadStream(streamId)) return unauthorized();
                        widget = new QuickvaluesWidget(dashboard, query, timerange, params.get("field"), description, streamId);
                        break;
                    case SEARCH_RESULT_CHART:
                        if (!canReadStream(streamId)) return unauthorized();
                        widget = new SearchResultChartWidget(dashboard, query, timerange, description, streamId, params.get("interval"));
                        break;
                    case STATS_COUNT: {
                        final String field = params.get("field");
                        final String statsFunction = params.get("statsFunction");
                        final Boolean trend = Boolean.parseBoolean(params.get("trend"));
                        if (trend) {
                            if (!rangeType.equals("relative")) {
                                Logger.error("Cannot add statistical count widget with trend on a non relative time range");
                                return badRequest();
                            }
                            final Boolean lowerIsBetter = Boolean.parseBoolean(params.get("lowerIsBetter"));
                            widget = new StatisticalCountWidget(dashboard, query, timerange, description, trend, lowerIsBetter, field, statsFunction, streamId);
                        } else {
                            widget = new StatisticalCountWidget(dashboard, query, timerange, description, field, statsFunction, streamId);
                        }
                        break;
                    }
                    default:
                        throw new IllegalArgumentException();
                }
            } catch (IllegalArgumentException e) {
                Logger.error("No such widget type: " + params.get("widgetType"));
                return badRequest();
            }

            dashboard.addWidget(widget);

            return created();
        } catch (APIException e) {
            String message = "Could not add widget. We got a HTTP " + e.getHttpCode() + ".";
            return status(504, views.html.errors.error.render(message, e, request()));
        } catch (IOException e) {
            return status(504, views.html.errors.error.render(ApiClient.ERROR_MSG_IO, e, request()));
        }
    }

    private boolean canReadStream(String streamId) {
        if (streamId == null) return true;
        return Permissions.isPermitted(RestPermissions.STREAMS_READ, streamId);
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

    @BodyParser.Of(BodyParser.FormUrlEncoded.class)
    public Result updateWidgetDescription(String dashboardId, String widgetId) {
        String newDescription = flattenFormUrlEncoded(request().body().asFormUrlEncoded()).get("description");

        if (newDescription == null || newDescription.trim().isEmpty()) {
            return badRequest();
        }

        try {
            Dashboard dashboard = dashboardService.get(dashboardId);
            DashboardWidget widget = dashboard.getWidget(widgetId);

            widget.updateDescription(api(), newDescription.trim());

            return ok().as(Http.MimeTypes.JSON);
        } catch (APIException e) {
            String message = "Could not get widget. We expected HTTP 200, but got a HTTP " + e.getHttpCode() + ".";
            return status(504, views.html.errors.error.render(message, e, request()));
        } catch (IOException e) {
            return status(504, views.html.errors.error.render(ApiClient.ERROR_MSG_IO, e, request()));
        }
    }

    @BodyParser.Of(BodyParser.FormUrlEncoded.class)
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

            return ok().as(Http.MimeTypes.JSON);
        } catch (APIException e) {
            String message = "Could not get widget. We expected HTTP 200, but got a HTTP " + e.getHttpCode() + ".";
            return status(504, views.html.errors.error.render(message, e, request()));
        } catch (IOException e) {
            return status(504, views.html.errors.error.render(ApiClient.ERROR_MSG_IO, e, request()));
        }
    }

}

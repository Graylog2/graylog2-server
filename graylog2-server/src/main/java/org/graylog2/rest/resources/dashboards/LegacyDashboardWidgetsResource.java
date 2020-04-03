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
package org.graylog2.rest.resources.dashboards;

import com.codahale.metrics.annotation.Timed;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog2.audit.AuditEventTypes;
import org.graylog2.audit.jersey.AuditEvent;
import org.graylog2.dashboards.Dashboard;
import org.graylog2.dashboards.DashboardService;
import org.graylog2.dashboards.widgets.DashboardWidget;
import org.graylog2.dashboards.widgets.DashboardWidgetCreator;
import org.graylog2.dashboards.widgets.InvalidWidgetConfigurationException;
import org.graylog2.dashboards.widgets.WidgetResultCache;
import org.graylog2.database.NotFoundException;
import org.graylog2.rest.models.dashboards.responses.WidgetSummary;
import org.graylog2.shared.rest.resources.RestResource;
import org.graylog2.shared.security.RestPermissions;
import org.graylog2.shared.system.activities.Activity;
import org.graylog2.shared.system.activities.ActivityWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.Map;

@RequiresAuthentication
@Api(value = "Legacy/Dashboards/Widgets", description = "Manage widgets of an existing dashboard")
@Path("/legacy/dashboards/{dashboardId}/widgets")
public class LegacyDashboardWidgetsResource extends RestResource {
    private static final Logger LOG = LoggerFactory.getLogger(LegacyDashboardWidgetsResource.class);

    private final DashboardWidgetCreator dashboardWidgetCreator;
    private final ActivityWriter activityWriter;
    private final WidgetResultCache widgetResultCache;
    private final DashboardService dashboardService;

    @Inject
    public LegacyDashboardWidgetsResource(DashboardWidgetCreator dashboardWidgetCreator,
                                          ActivityWriter activityWriter,
                                          WidgetResultCache widgetResultCache,
                                          DashboardService dashboardService) {
        this.dashboardWidgetCreator = dashboardWidgetCreator;
        this.activityWriter = activityWriter;
        this.widgetResultCache = widgetResultCache;
        this.dashboardService = dashboardService;
    }

    @GET
    @Deprecated
    @Timed
    @ApiOperation(value = "Get a widget")
    @Path("/{widgetId}")
    @ApiResponses(value = {
            @ApiResponse(code = 404, message = "Dashboard not found."),
            @ApiResponse(code = 404, message = "Widget not found."),
    })
    @Produces(MediaType.APPLICATION_JSON)
    public WidgetSummary getWidget(
            @ApiParam(name = "dashboardId", required = true)
            @PathParam("dashboardId") String dashboardId,
            @ApiParam(name = "widgetId", required = true)
            @PathParam("widgetId") String widgetId) throws NotFoundException {
        checkPermission(RestPermissions.DASHBOARDS_READ, dashboardId);

        final Dashboard dashboard = dashboardService.load(dashboardId);
        final DashboardWidget widget = dashboard.getWidget(widgetId);

        return WidgetSummary.create(widget.getId(), widget.getDescription(), widget.getType(), widget.getCacheTime(),
                widget.getCreatorUserId(), widget.getConfig());
    }

    @DELETE
    @Deprecated
    @Timed
    @ApiOperation(value = "Delete a widget")
    @Path("/{widgetId}")
    @ApiResponses(value = {
            @ApiResponse(code = 404, message = "Dashboard not found."),
            @ApiResponse(code = 404, message = "Widget not found."),
    })
    @Produces(MediaType.APPLICATION_JSON)
    @AuditEvent(type = AuditEventTypes.DASHBOARD_WIDGET_DELETE)
    public void remove(
            @ApiParam(name = "dashboardId", required = true)
            @PathParam("dashboardId") String dashboardId,
            @ApiParam(name = "widgetId", required = true)
            @PathParam("widgetId") String widgetId) throws NotFoundException {
        checkPermission(RestPermissions.DASHBOARDS_EDIT, dashboardId);

        final Dashboard dashboard = dashboardService.load(dashboardId);

        final DashboardWidget widget = dashboard.getWidget(widgetId);
        dashboardService.removeWidget(dashboard, widget);

        final String msg = "Deleted widget <" + widgetId + "> from dashboard <" + dashboardId + ">. Reason: REST request.";
        LOG.info(msg);
        activityWriter.write(new Activity(msg, LegacyDashboardsResource.class));
    }

    @GET
    @Deprecated
    @Timed
    @ApiOperation(value = "Get a single widget value.")
    @Path("/{widgetId}/value")
    @ApiResponses(value = {
            @ApiResponse(code = 404, message = "Dashboard not found."),
            @ApiResponse(code = 404, message = "Widget not found."),
            @ApiResponse(code = 504, message = "Computation failed on indexer side.")
    })
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Object> widgetValue(@ApiParam(name = "dashboardId", required = true)
                                           @PathParam("dashboardId") String dashboardId,
                                           @ApiParam(name = "widgetId", required = true)
                                           @PathParam("widgetId") String widgetId) throws NotFoundException, InvalidWidgetConfigurationException {
        checkPermission(RestPermissions.DASHBOARDS_READ, dashboardId);

        final Dashboard dashboard = dashboardService.load(dashboardId);

        final DashboardWidget widget = dashboard.getWidget(widgetId);
        if (widget == null) {
            final String msg = "Widget " + widgetId + " on dashboard " + dashboardId + " not found.";
            LOG.error(msg);
            throw new javax.ws.rs.NotFoundException(msg);
        }

        return widgetResultCache.getComputationResultForDashboardWidget(widget).asMap();
    }
}

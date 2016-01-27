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
import com.google.common.collect.ImmutableMap;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog2.dashboards.Dashboard;
import org.graylog2.dashboards.DashboardService;
import org.graylog2.dashboards.widgets.DashboardWidget;
import org.graylog2.dashboards.widgets.DashboardWidgetCreator;
import org.graylog2.dashboards.widgets.InvalidWidgetConfigurationException;
import org.graylog2.dashboards.widgets.WidgetResultCache;
import org.graylog2.database.NotFoundException;
import org.graylog2.plugin.database.ValidationException;
import org.graylog2.plugin.indexer.searches.timeranges.InvalidRangeParametersException;
import org.graylog2.rest.models.dashboards.requests.AddWidgetRequest;
import org.graylog2.rest.models.dashboards.requests.UpdateWidgetRequest;
import org.graylog2.rest.models.dashboards.responses.WidgetSummary;
import org.graylog2.shared.rest.resources.RestResource;
import org.graylog2.shared.security.RestPermissions;
import org.graylog2.shared.system.activities.Activity;
import org.graylog2.shared.system.activities.ActivityWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.Map;

@RequiresAuthentication
@Api(value = "Dashboards/Widgets", description = "Manage widgets of an existing dashboard")
@Path("/dashboards/{dashboardId}/widgets")
public class DashboardWidgetsResource extends RestResource {
    private static final Logger LOG = LoggerFactory.getLogger(DashboardWidgetsResource.class);

    private final DashboardWidgetCreator dashboardWidgetCreator;
    private final ActivityWriter activityWriter;
    private final WidgetResultCache widgetResultCache;
    private final DashboardService dashboardService;

    @Inject
    public DashboardWidgetsResource(DashboardWidgetCreator dashboardWidgetCreator, ActivityWriter activityWriter, WidgetResultCache widgetResultCache, DashboardService dashboardService) {
        this.dashboardWidgetCreator = dashboardWidgetCreator;
        this.activityWriter = activityWriter;
        this.widgetResultCache = widgetResultCache;
        this.dashboardService = dashboardService;
    }

    @POST
    @Timed
    @ApiOperation(value = "Add a widget to a dashboard")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 404, message = "Dashboard not found."),
            @ApiResponse(code = 400, message = "Validation error."),
            @ApiResponse(code = 400, message = "No such widget type."),
    })
    public Response addWidget(
            @ApiParam(name = "dashboardId", required = true)
            @PathParam("dashboardId") String dashboardId,
            @ApiParam(name = "JSON body", required = true) AddWidgetRequest awr) throws ValidationException, NotFoundException {
        checkPermission(RestPermissions.DASHBOARDS_EDIT, dashboardId);

        // Bind to streams for reader users and check stream permission.
        if (awr.config().containsKey("stream_id")) {
            checkPermission(RestPermissions.STREAMS_READ, (String) awr.config().get("stream_id"));
        } else {
            checkPermission(RestPermissions.SEARCHES_ABSOLUTE);
            checkPermission(RestPermissions.SEARCHES_RELATIVE);
            checkPermission(RestPermissions.SEARCHES_KEYWORD);
        }

        final DashboardWidget widget;
        try {
            widget = dashboardWidgetCreator.fromRequest(awr, getCurrentUser().getName());

            final Dashboard dashboard = dashboardService.load(dashboardId);

            dashboardService.addWidget(dashboard, widget);
        } catch (DashboardWidget.NoSuchWidgetTypeException e2) {
            LOG.debug("No such widget type.", e2);
            throw new BadRequestException("No such widget type.", e2);
        } catch (InvalidRangeParametersException e3) {
            LOG.debug("Invalid timerange parameters provided.", e3);
            throw new BadRequestException("Invalid timerange parameters provided.", e3);
        } catch (InvalidWidgetConfigurationException e4) {
            LOG.debug("Invalid widget configuration.", e4);
            throw new BadRequestException("Invalid widget configuration.", e4);
        }

        final Map<String, String> result = ImmutableMap.of("widget_id", widget.getId());
        final URI widgetUri = getUriBuilderToSelf().path(DashboardWidgetsResource.class, "getWidget")
                .build(dashboardId, widget.getId());

        return Response.created(widgetUri).entity(result).build();
    }

    @GET
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
    @Timed
    @ApiOperation(value = "Delete a widget")
    @Path("/{widgetId}")
    @ApiResponses(value = {
            @ApiResponse(code = 404, message = "Dashboard not found."),
            @ApiResponse(code = 404, message = "Widget not found."),
    })
    @Produces(MediaType.APPLICATION_JSON)
    public void remove(
            @ApiParam(name = "dashboardId", required = true)
            @PathParam("dashboardId") String dashboardId,
            @ApiParam(name = "widgetId", required = true)
            @PathParam("widgetId") String widgetId) throws NotFoundException {
        checkPermission(RestPermissions.DASHBOARDS_EDIT, dashboardId);

        final Dashboard dashboard = dashboardService.load(dashboardId);

        final DashboardWidget widget = dashboard.getWidget(widgetId);
        this.widgetResultCache.invalidate(widget);
        dashboardService.removeWidget(dashboard, widget);

        final String msg = "Deleted widget <" + widgetId + "> from dashboard <" + dashboardId + ">. Reason: REST request.";
        LOG.info(msg);
        activityWriter.write(new Activity(msg, DashboardsResource.class));
    }

    @GET
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
            LOG.error("Widget not found.");
            throw new javax.ws.rs.NotFoundException();
        }

        return widgetResultCache.getComputationResultForDashboardWidget(widget).asMap();
    }

    @PUT
    @Timed
    @ApiOperation(value = "Update a widget")
    @Path("/{widgetId}")
    @ApiResponses(value = {
            @ApiResponse(code = 404, message = "Dashboard not found."),
            @ApiResponse(code = 404, message = "Widget not found."),
    })
    @Produces(MediaType.APPLICATION_JSON)
    public void updateWidget(@ApiParam(name = "dashboardId", required = true)
                             @PathParam("dashboardId") String dashboardId,
                             @ApiParam(name = "widgetId", required = true)
                             @PathParam("widgetId") String widgetId,
                             @ApiParam(name = "JSON body", required = true)
                             @Valid @NotNull AddWidgetRequest awr) throws ValidationException, NotFoundException {
        checkPermission(RestPermissions.DASHBOARDS_EDIT, dashboardId);

        final Dashboard dashboard = dashboardService.load(dashboardId);

        final DashboardWidget widget = dashboard.getWidget(widgetId);
        if (widget == null) {
            LOG.error("Widget not found.");
            throw new javax.ws.rs.NotFoundException();
        }

        try {
            final DashboardWidget updatedWidget = dashboardWidgetCreator.fromRequest(widgetId, awr, widget.getCreatorUserId());
            updatedWidget.setCacheTime(awr.cacheTime());

            dashboardService.removeWidget(dashboard, widget);
            dashboardService.addWidget(dashboard, updatedWidget);
            this.widgetResultCache.invalidate(widget);
        } catch (DashboardWidget.NoSuchWidgetTypeException e2) {
            LOG.error("No such widget type.", e2);
            throw new BadRequestException(e2);
        } catch (InvalidRangeParametersException e3) {
            LOG.error("Invalid timerange parameters provided.", e3);
            throw new BadRequestException(e3);
        } catch (InvalidWidgetConfigurationException e4) {
            LOG.error("Invalid widget configuration.", e4);
            throw new BadRequestException(e4);
        }

        LOG.info("Updated widget <" + widgetId + "> on dashboard <" + dashboardId + ">. Reason: REST request.");
    }

    @Deprecated
    @PUT
    @Timed
    @ApiOperation(value = "Update description of a widget")
    @Path("/{widgetId}/description")
    @ApiResponses(value = {
            @ApiResponse(code = 404, message = "Dashboard not found."),
            @ApiResponse(code = 404, message = "Widget not found."),
    })
    @Produces(MediaType.APPLICATION_JSON)
    public void updateDescription(@ApiParam(name = "dashboardId", required = true)
                                  @PathParam("dashboardId") String dashboardId,
                                  @ApiParam(name = "widgetId", required = true)
                                  @PathParam("widgetId") String widgetId,
                                  @ApiParam(name = "JSON body", required = true)
                                  @Valid UpdateWidgetRequest uwr) throws ValidationException, NotFoundException {
        checkPermission(RestPermissions.DASHBOARDS_EDIT, dashboardId);

        final Dashboard dashboard = dashboardService.load(dashboardId);

        final DashboardWidget widget = dashboard.getWidget(widgetId);
        if (widget == null) {
            LOG.error("Widget not found.");
            throw new javax.ws.rs.NotFoundException();
        }

        dashboardService.updateWidgetDescription(dashboard, widget, uwr.description());

        LOG.info("Updated description of widget <" + widgetId + "> on dashboard <" + dashboardId + ">. Reason: REST request.");
    }

    @Deprecated
    @PUT
    @Timed
    @ApiOperation(value = "Update cache time of a widget")
    @Path("/{widgetId}/cachetime")
    @ApiResponses(value = {
            @ApiResponse(code = 404, message = "Dashboard not found."),
            @ApiResponse(code = 404, message = "Widget not found."),
    })
    @Produces(MediaType.APPLICATION_JSON)
    public void updateCacheTime(@ApiParam(name = "dashboardId", required = true)
                                @PathParam("dashboardId") String dashboardId,
                                @ApiParam(name = "widgetId", required = true)
                                @PathParam("widgetId") String widgetId,
                                @ApiParam(name = "JSON body", required = true)
                                @Valid UpdateWidgetRequest uwr) throws ValidationException, NotFoundException {
        checkPermission(RestPermissions.DASHBOARDS_EDIT, dashboardId);

        final Dashboard dashboard = dashboardService.load(dashboardId);

        final DashboardWidget widget = dashboard.getWidget(widgetId);
        if (widget == null) {
            LOG.error("Widget not found.");
            throw new javax.ws.rs.NotFoundException();
        }

        dashboardService.updateWidgetCacheTime(dashboard, widget, uwr.cacheTime());
        this.widgetResultCache.invalidate(widget);

        LOG.info("Updated cache time of widget <" + widgetId + "> on dashboard <" + dashboardId + "> to " +
                "[" + uwr.cacheTime() + "]. Reason: REST request.");
    }
}

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

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.annotation.Timed;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.graylog2.dashboards.Dashboard;
import org.graylog2.dashboards.DashboardRegistry;
import org.graylog2.dashboards.DashboardService;
import org.graylog2.dashboards.widgets.DashboardWidget;
import org.graylog2.dashboards.widgets.InvalidWidgetConfigurationException;
import org.graylog2.database.NotFoundException;
import org.graylog2.plugin.database.ValidationException;
import org.graylog2.indexer.searches.Searches;
import org.graylog2.indexer.searches.timeranges.InvalidRangeParametersException;
import org.graylog2.plugin.Tools;
import org.graylog2.shared.rest.resources.RestResource;
import org.graylog2.rest.models.dashboards.requests.AddWidgetRequest;
import org.graylog2.rest.models.dashboards.requests.CreateDashboardRequest;
import org.graylog2.rest.models.dashboards.requests.UpdateDashboardRequest;
import org.graylog2.rest.models.dashboards.requests.UpdateWidgetRequest;
import org.graylog2.rest.resources.dashboards.requests.WidgetPositions;
import org.graylog2.rest.models.dashboards.responses.DashboardList;
import org.graylog2.shared.security.RestPermissions;
import org.graylog2.shared.security.RestrictToMaster;
import org.graylog2.shared.system.activities.Activity;
import org.graylog2.shared.system.activities.ActivityWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@RequiresAuthentication
@Api(value = "Dashboards", description = "Manage dashboards")
@Path("/dashboards")
public class DashboardsResource extends RestResource {
    private static final Logger LOG = LoggerFactory.getLogger(DashboardsResource.class);

    private DashboardService dashboardService;
    private DashboardRegistry dashboardRegistry;
    private ActivityWriter activityWriter;
    private MetricRegistry metricRegistry;
    private final Searches searches;

    @Inject
    public DashboardsResource(DashboardService dashboardService,
                              DashboardRegistry dashboardRegistry,
                              ActivityWriter activityWriter,
                              MetricRegistry metricRegistry,
                              Searches searches) {
        this.dashboardService = dashboardService;
        this.dashboardRegistry = dashboardRegistry;
        this.activityWriter = activityWriter;
        this.metricRegistry = metricRegistry;
        this.searches = searches;
    }

    @POST
    @Timed
    @ApiOperation(value = "Create a dashboard")
    @RequiresPermissions(RestPermissions.DASHBOARDS_CREATE)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 403, message = "Request must be performed against master node.")
    })
    @RestrictToMaster
    public Response create(@ApiParam(name = "JSON body", required = true) CreateDashboardRequest cr) throws ValidationException {
        // Create dashboard.
        final Dashboard dashboard = dashboardService.create(cr.title(), cr.description(), getCurrentUser().getName(), Tools.iso8601());
        final String id = dashboardService.save(dashboard);

        dashboardRegistry.add(dashboard);

        final Map<String, String> result = ImmutableMap.of("dashboard_id", id);
        final URI dashboardUri = getUriBuilderToSelf().path(DashboardsResource.class)
                .path("{dashboardId}")
                .build(id);

        return Response.created(dashboardUri).entity(result).build();
    }

    @GET
    @Timed
    @ApiOperation(value = "Get a list of all dashboards and all configurations of their widgets.")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 403, message = "Request must be performed against master node.")
    })
    @RestrictToMaster
    public DashboardList list() {
        final List<Map<String, Object>> dashboards = Lists.newArrayList();
        for (Dashboard dashboard : dashboardService.all()) {
            if (isPermitted(RestPermissions.DASHBOARDS_READ, dashboard.getId())) {
                dashboards.add(dashboard.asMap());
            }
        }

        return DashboardList.create(dashboards.size(),dashboards);
    }

    @GET
    @Timed
    @ApiOperation(value = "Get a single dashboards and all configurations of its widgets.")
    @Path("/{dashboardId}")
    @ApiResponses(value = {
            @ApiResponse(code = 404, message = "Dashboard not found."),
            @ApiResponse(code = 403, message = "Request must be performed against master node.")
    })
    @Produces(MediaType.APPLICATION_JSON)
    @RestrictToMaster
    public Map<String, Object> get(@ApiParam(name = "dashboardId", required = true)
                                   @PathParam("dashboardId") String dashboardId) throws NotFoundException {
        checkPermission(RestPermissions.DASHBOARDS_READ, dashboardId);

        return dashboardService.load(dashboardId).asMap();
    }

    @DELETE
    @Timed
    @ApiOperation(value = "Delete a dashboard and all its widgets")
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{dashboardId}")
    @ApiResponses(value = {
            @ApiResponse(code = 404, message = "Dashboard not found."),
            @ApiResponse(code = 403, message = "Request must be performed against master node.")
    })
    @RestrictToMaster
    public void delete(@ApiParam(name = "dashboardId", required = true)
                       @PathParam("dashboardId") String dashboardId) throws NotFoundException {
        checkPermission(RestPermissions.DASHBOARDS_EDIT, dashboardId);

        final Dashboard dashboard = dashboardService.load(dashboardId);
        dashboardRegistry.remove(dashboardId);
        dashboardService.destroy(dashboard);

        final String msg = "Deleted dashboard <" + dashboard.getId() + ">. Reason: REST request.";
        LOG.info(msg);
        activityWriter.write(new Activity(msg, DashboardsResource.class));
    }

    @PUT
    @Timed
    @ApiOperation(value = "Update the settings of a dashboard.")
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{dashboardId}")
    @ApiResponses(value = {
            @ApiResponse(code = 404, message = "Dashboard not found.")
    })
    public void update(@ApiParam(name = "dashboardId", required = true)
                       @PathParam("dashboardId") String dashboardId,
                       @ApiParam(name = "JSON body", required = true) UpdateDashboardRequest cr) throws ValidationException, NotFoundException {
        checkPermission(RestPermissions.DASHBOARDS_EDIT, dashboardId);

        final Dashboard dashboard = dashboardService.load(dashboardId);
        if (cr.title() != null) {
            dashboard.setTitle(cr.title());
        }

        if (cr.description() != null) {
            dashboard.setDescription(cr.description());
        }

        // Validations are happening here.
        dashboardService.save(dashboard);
    }

    @PUT
    @Timed
    @ApiOperation(value = "Update/set the positions of dashboard widgets.")
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{dashboardId}/positions")
    @ApiResponses(value = {
            @ApiResponse(code = 404, message = "Dashboard not found.")
    })
    public void setPositions(
            @ApiParam(name = "dashboardId", required = true)
            @PathParam("dashboardId") String dashboardId,
            @ApiParam(name = "JSON body", required = true)
            @Valid WidgetPositions uwpr) throws NotFoundException, ValidationException {
        checkPermission(RestPermissions.DASHBOARDS_EDIT, dashboardId);

        final Dashboard dashboard = dashboardService.load(dashboardId);
        dashboardService.updateWidgetPositions(dashboard, uwpr);
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
            @ApiResponse(code = 403, message = "Request must be performed against master node.")
    })
    @Path("/{dashboardId}/widgets")
    @RestrictToMaster
    public Response addWidget(
            @ApiParam(name = "dashboardId", required = true)
            @PathParam("dashboardId") String dashboardId,
            @ApiParam(name = "JSON body", required = true) AddWidgetRequest awr) throws ValidationException {
        checkPermission(RestPermissions.DASHBOARDS_EDIT, dashboardId);

        // Bind to streams for reader users and check stream permission.
        if (awr.config().containsKey("stream_id")) {
            checkPermission(RestPermissions.STREAMS_READ, (String) awr.config().get("stream_id"));
        } else {
            checkPermission(RestPermissions.SEARCHES_ABSOLUTE);
            checkPermission(RestPermissions.SEARCHES_RELATIVE);
            checkPermission(RestPermissions.SEARCHES_KEYWORD);
        }

        DashboardWidget widget;
        try {
            widget = DashboardWidget.fromRequest(metricRegistry, searches, awr, getCurrentUser().getName());

            Dashboard dashboard = dashboardRegistry.get(dashboardId);

            if (dashboard == null) {
                LOG.error("Dashboard [{}] not found.", dashboardId);
                throw new WebApplicationException(404);
            }

            dashboardService.addWidget(dashboard, widget);
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

        final Map<String, String> result = ImmutableMap.of("widget_id", widget.getId());
        final URI widgetUri = getUriBuilderToSelf().path(DashboardsResource.class)
                .path("{dashboardId}/widgets/{widgetId}")
                .build(dashboardId, widget.getId());

        return Response.created(widgetUri).entity(result).build();
    }

    @DELETE
    @Timed
    @ApiOperation(value = "Delete a widget")
    @Path("/{dashboardId}/widgets/{widgetId}")
    @ApiResponses(value = {
            @ApiResponse(code = 404, message = "Dashboard not found."),
            @ApiResponse(code = 404, message = "Widget not found."),
            @ApiResponse(code = 403, message = "Request must be performed against master node.")
    })
    @Produces(MediaType.APPLICATION_JSON)
    @RestrictToMaster
    public void remove(
            @ApiParam(name = "dashboardId", required = true)
            @PathParam("dashboardId") String dashboardId,
            @ApiParam(name = "widgetId", required = true)
            @PathParam("widgetId") String widgetId) {
        checkPermission(RestPermissions.DASHBOARDS_EDIT, dashboardId);

        final Dashboard dashboard = dashboardRegistry.get(dashboardId);
        if (dashboard == null) {
            LOG.error("Dashboard not found.");
            throw new javax.ws.rs.NotFoundException();
        }

        final DashboardWidget widget = dashboard.getWidget(widgetId);
        dashboardService.removeWidget(dashboard, widget);

        final String msg = "Deleted widget <" + widgetId + "> from dashboard <" + dashboardId + ">. Reason: REST request.";
        LOG.info(msg);
        activityWriter.write(new Activity(msg, DashboardsResource.class));
    }

    @GET
    @Timed
    @ApiOperation(value = "Get a single widget value.")
    @Path("/{dashboardId}/widgets/{widgetId}/value")
    @ApiResponses(value = {
            @ApiResponse(code = 404, message = "Dashboard not found."),
            @ApiResponse(code = 404, message = "Widget not found."),
            @ApiResponse(code = 403, message = "Request must be performed against master node."),
            @ApiResponse(code = 504, message = "Computation failed on indexer side.")
    })
    @Produces(MediaType.APPLICATION_JSON)
    @RestrictToMaster
    public Map<String, Object> widgetValue(@ApiParam(name = "dashboardId", required = true)
                                           @PathParam("dashboardId") String dashboardId,
                                           @ApiParam(name = "widgetId", required = true)
                                           @PathParam("widgetId") String widgetId) {
        checkPermission(RestPermissions.DASHBOARDS_READ, dashboardId);

        Dashboard dashboard = dashboardRegistry.get(dashboardId);

        if (dashboard == null) {
            LOG.error("Dashboard not found.");
            throw new javax.ws.rs.NotFoundException();
        }

        final DashboardWidget widget = dashboard.getWidget(widgetId);
        if (widget == null) {
            LOG.error("Widget not found.");
            throw new javax.ws.rs.NotFoundException();
        }

        try {
            return widget.getComputationResult().asMap();
        } catch (ExecutionException e) {
            LOG.error("Error while computing dashboard.", e);
            throw new WebApplicationException(e, Response.Status.GATEWAY_TIMEOUT);
        }
    }

    @PUT
    @Timed
    @ApiOperation(value = "Update description of a widget")
    @Path("/{dashboardId}/widgets/{widgetId}/description")
    @ApiResponses(value = {
            @ApiResponse(code = 404, message = "Dashboard not found."),
            @ApiResponse(code = 404, message = "Widget not found."),
            @ApiResponse(code = 403, message = "Request must be performed against master node.")
    })
    @Produces(MediaType.APPLICATION_JSON)
    @RestrictToMaster
    public void updateDescription(@ApiParam(name = "dashboardId", required = true)
                                  @PathParam("dashboardId") String dashboardId,
                                  @ApiParam(name = "widgetId", required = true)
                                  @PathParam("widgetId") String widgetId,
                                  @ApiParam(name = "JSON body", required = true)
                                  @Valid UpdateWidgetRequest uwr) throws ValidationException {
        checkPermission(RestPermissions.DASHBOARDS_EDIT, dashboardId);

        final Dashboard dashboard = dashboardRegistry.get(dashboardId);
        if (dashboard == null) {
            LOG.error("Dashboard not found.");
            throw new javax.ws.rs.NotFoundException();
        }

        final DashboardWidget widget = dashboard.getWidget(widgetId);
        if (widget == null) {
            LOG.error("Widget not found.");
            throw new javax.ws.rs.NotFoundException();
        }

        dashboardService.updateWidgetDescription(dashboard, widget, uwr.description());

        LOG.info("Updated description of widget <" + widgetId + "> on dashboard <" + dashboardId + ">. Reason: REST request.");
    }

    @PUT
    @Timed
    @ApiOperation(value = "Update cache time of a widget")
    @Path("/{dashboardId}/widgets/{widgetId}/cachetime")
    @ApiResponses(value = {
            @ApiResponse(code = 404, message = "Dashboard not found."),
            @ApiResponse(code = 404, message = "Widget not found."),
            @ApiResponse(code = 403, message = "Request must be performed against master node.")
    })
    @Produces(MediaType.APPLICATION_JSON)
    @RestrictToMaster
    public void updateCacheTime(@ApiParam(name = "dashboardId", required = true)
                                @PathParam("dashboardId") String dashboardId,
                                @ApiParam(name = "widgetId", required = true)
                                @PathParam("widgetId") String widgetId,
                                @ApiParam(name = "JSON body", required = true)
                                @Valid UpdateWidgetRequest uwr) throws ValidationException {
        checkPermission(RestPermissions.DASHBOARDS_EDIT, dashboardId);

        final Dashboard dashboard = dashboardRegistry.get(dashboardId);
        if (dashboard == null) {
            LOG.error("Dashboard not found.");
            throw new javax.ws.rs.NotFoundException();
        }

        final DashboardWidget widget = dashboard.getWidget(widgetId);
        if (widget == null) {
            LOG.error("Widget not found.");
            throw new javax.ws.rs.NotFoundException();
        }

        dashboardService.updateWidgetCacheTime(dashboard, widget, uwr.cacheTime());

        LOG.info("Updated cache time of widget <" + widgetId + "> on dashboard <" + dashboardId + "> to " +
                "[" + uwr.cacheTime() + "]. Reason: REST request.");
    }
}
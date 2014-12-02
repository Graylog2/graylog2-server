/**
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
 */
package org.graylog2.rest.resources.dashboards;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.annotation.Timed;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.graylog2.dashboards.Dashboard;
import org.graylog2.dashboards.DashboardImpl;
import org.graylog2.dashboards.DashboardRegistry;
import org.graylog2.dashboards.DashboardService;
import org.graylog2.dashboards.widgets.DashboardWidget;
import org.graylog2.dashboards.widgets.InvalidWidgetConfigurationException;
import org.graylog2.database.ValidationException;
import org.graylog2.indexer.searches.Searches;
import org.graylog2.indexer.searches.timeranges.InvalidRangeParametersException;
import org.graylog2.plugin.Tools;
import org.graylog2.rest.resources.RestResource;
import org.graylog2.rest.resources.dashboards.requests.AddWidgetRequest;
import org.graylog2.rest.resources.dashboards.requests.CreateRequest;
import org.graylog2.rest.resources.dashboards.requests.UpdateRequest;
import org.graylog2.rest.resources.dashboards.requests.UpdateWidgetPositionsRequest;
import org.graylog2.rest.resources.dashboards.requests.UpdateWidgetRequest;
import org.graylog2.security.RestPermissions;
import org.graylog2.shared.system.activities.Activity;
import org.graylog2.shared.system.activities.ActivityWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
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
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
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
    public Response create(@ApiParam(required = true) String body) {
        restrictToMaster();

        CreateRequest cr;
        try {
            cr = objectMapper.readValue(body, CreateRequest.class);
        } catch(IOException e) {
            LOG.error("Error while parsing JSON", e);
            throw new WebApplicationException(e, Response.Status.BAD_REQUEST);
        }

        // Create dashboard.
        Map<String, Object> dashboardData = Maps.newHashMap();
        dashboardData.put("title", cr.title);
        dashboardData.put("description", cr.description);
        dashboardData.put("creator_user_id", getCurrentUser().getName());
        dashboardData.put("created_at", Tools.iso8601());

        Dashboard dashboard = new DashboardImpl(dashboardData);
        String id;
        try {
            id = dashboardService.save(dashboard);
        } catch (ValidationException e) {
            LOG.error("Validation error.", e);
            throw new WebApplicationException(e, Response.Status.BAD_REQUEST);
        }

        dashboardRegistry.add(dashboard);

        Map<String, Object> result = Maps.newHashMap();
        result.put("dashboard_id", id);

        return Response.status(Response.Status.CREATED).entity(json(result)).build();
    }

    @GET @Timed
    @ApiOperation(value = "Get a list of all dashboards and all configurations of their widgets.")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 403, message = "Request must be performed against master node.")
    })
    public String list() {
        restrictToMaster();
        List<Map<String, Object>> dashboards = Lists.newArrayList();

        for (Dashboard dashboard : dashboardService.all()) {
            if (isPermitted(RestPermissions.DASHBOARDS_READ, dashboard.getId())) {
                dashboards.add(dashboard.asMap());
            }
        }

        Map<String, Object> result = Maps.newHashMap();
        result.put("total", dashboards.size());
        result.put("dashboards", dashboards);

        return json(result);
    }

    @GET @Timed
    @ApiOperation(value = "Get a single dashboards and all configurations of its widgets.")
    @Path("/{dashboardId}")
    @ApiResponses(value = {
            @ApiResponse(code = 404, message = "Dashboard not found."),
            @ApiResponse(code = 403, message = "Request must be performed against master node.")
    })
    @Produces(MediaType.APPLICATION_JSON)
    public String get(@ApiParam(name= "dashboardId", required = true) @PathParam("dashboardId") String dashboardId) {
        restrictToMaster();
        checkPermission(RestPermissions.DASHBOARDS_READ, dashboardId);

        try {
            Dashboard dashboard = dashboardService.load(dashboardId);
            return json(dashboard.asMap());
        } catch (org.graylog2.database.NotFoundException e) {
            throw new WebApplicationException(404);
        }
    }

    @DELETE @Timed
    @ApiOperation(value = "Delete a dashboard and all its widgets")
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{dashboardId}")
    @ApiResponses(value = {
            @ApiResponse(code = 404, message = "Dashboard not found."),
            @ApiResponse(code = 403, message = "Request must be performed against master node.")
    })
    public Response delete(@ApiParam(name = "dashboardId", required = true) @PathParam("dashboardId") String dashboardId) {
        restrictToMaster();
        checkPermission(RestPermissions.DASHBOARDS_EDIT, dashboardId);

        try {
            Dashboard dashboard = dashboardService.load(dashboardId);
            dashboardRegistry.remove(dashboardId);
            dashboardService.destroy(dashboard);

            String msg = "Deleted dashboard <" + dashboard.getId() + ">. Reason: REST request.";
            LOG.info(msg);
            activityWriter.write(new Activity(msg, DashboardsResource.class));
        } catch (org.graylog2.database.NotFoundException e) {
            throw new WebApplicationException(404);
        }

        return Response.status(Response.Status.fromStatusCode(204)).build();
    }

    @PUT @Timed
    @ApiOperation(value = "Update the settings of a dashboard.")
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{dashboardId}")
    @ApiResponses(value = {
            @ApiResponse(code = 404, message = "Dashboard not found.")
    })
    public Response update(@ApiParam(name = "JSON body", required = true) String body,
                           @ApiParam(name = "dashboardId", required = true) @PathParam("dashboardId") String dashboardId) {
        checkPermission(RestPermissions.DASHBOARDS_EDIT, dashboardId);
        try {
            UpdateRequest cr;
            try {
                cr = objectMapper.readValue(body, UpdateRequest.class);
            } catch(IOException e) {
                LOG.error("Error while parsing JSON", e);
                throw new WebApplicationException(e, Response.Status.BAD_REQUEST);
            }

            Dashboard dashboard = dashboardService.load(dashboardId);

            if(cr.title != null) {
                dashboard.setTitle(cr.title);
            }

            if (cr.description != null) {
                dashboard.setDescription(cr.description);
            }

            // Validations are happening here.
            dashboardService.save(dashboard);
        } catch (org.graylog2.database.NotFoundException e) {
            throw new WebApplicationException(404);
        } catch (ValidationException e) {
            LOG.error("Validation error.", e);
            throw new WebApplicationException(e, Response.Status.BAD_REQUEST);
        }

        return Response.status(Response.Status.OK).build();
    }

    @PUT @Timed
    @ApiOperation(value = "Update/set the positions of dashboard widgets.")
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{dashboardId}/positions")
    @ApiResponses(value = {
            @ApiResponse(code = 404, message = "Dashboard not found.")
    })
    public Response setPositions(@ApiParam(name = "JSON body", required = true) String body,
                           @ApiParam(name = "dashboardId", required = true) @PathParam("dashboardId") String dashboardId) {
        checkPermission(RestPermissions.DASHBOARDS_EDIT, dashboardId);
        try {
            UpdateWidgetPositionsRequest uwpr;
            try {
                uwpr = objectMapper.readValue(body, UpdateWidgetPositionsRequest.class);
            } catch(IOException e) {
                LOG.error("Error while parsing JSON", e);
                throw new WebApplicationException(e, Response.Status.BAD_REQUEST);
            }

            Dashboard dashboard = dashboardService.load(dashboardId);
            dashboardService.updateWidgetPositions(dashboard, uwpr.positions);
        } catch (org.graylog2.database.NotFoundException e) {
            throw new WebApplicationException(404);
        } catch (ValidationException e) {
            LOG.error("Validation error.", e);
            throw new WebApplicationException(e, Response.Status.BAD_REQUEST);
        }

        return Response.status(Response.Status.OK).build();
    }

    @POST @Timed
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
    public Response addWidget(@ApiParam(name = "JSON body", required = true) String body,
                              @ApiParam(name = "dashboardId", required = true) @PathParam("dashboardId") String dashboardId) {
        restrictToMaster();
        checkPermission(RestPermissions.DASHBOARDS_EDIT, dashboardId);

        AddWidgetRequest awr;
        try {
            awr = objectMapper.readValue(body, AddWidgetRequest.class);
        } catch(IOException e) {
            LOG.error("Error while parsing JSON", e);
            throw new WebApplicationException(e, Response.Status.BAD_REQUEST);
        }

        // Bind to streams for reader users and check stream permission.
        if (awr.config.containsKey("stream_id")) {
            checkPermission(RestPermissions.STREAMS_READ, (String) awr.config.get("stream_id"));
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
        } catch (ValidationException e1) {
            LOG.error("Validation error.", e1);
            throw new WebApplicationException(e1, Response.Status.BAD_REQUEST);
        } catch (DashboardWidget.NoSuchWidgetTypeException e2) {
            LOG.error("No such widget type.", e2);
            throw new WebApplicationException(e2, Response.Status.BAD_REQUEST);
        } catch (InvalidRangeParametersException e3) {
            LOG.error("Invalid timerange parameters provided.", e3);
            throw new WebApplicationException(e3, Response.Status.BAD_REQUEST);
        } catch (InvalidWidgetConfigurationException e4) {
            LOG.error("Invalid widget configuration.", e4);
            throw new WebApplicationException(e4, Response.Status.BAD_REQUEST);
        }

        Map<String, Object> result = Maps.newHashMap();
        result.put("widget_id", widget.getId());

        return Response.status(Response.Status.CREATED).entity(json(result)).build();
    }

    @DELETE @Timed
    @ApiOperation(value = "Delete a widget")
    @Path("/{dashboardId}/widgets/{widgetId}")
    @ApiResponses(value = {
            @ApiResponse(code = 404, message = "Dashboard not found."),
            @ApiResponse(code = 404, message = "Widget not found."),
            @ApiResponse(code = 403, message = "Request must be performed against master node.")
    })
    @Produces(MediaType.APPLICATION_JSON)
    public Response remove(
            @ApiParam(name = "dashboardId", required = true) @PathParam("dashboardId") String dashboardId,
            @ApiParam(name = "widgetId", required = true) @PathParam("widgetId") String widgetId) {
        restrictToMaster();
        checkPermission(RestPermissions.DASHBOARDS_EDIT, dashboardId);

        if (dashboardId == null || dashboardId.isEmpty()) {
            LOG.error("Missing dashboard ID. Returning HTTP 400.");
            throw new WebApplicationException(400);
        }

        if (widgetId == null || widgetId.isEmpty()) {
            LOG.error("Missing widget ID. Returning HTTP 400.");
            throw new WebApplicationException(400);
        }

        final Dashboard dashboard = dashboardRegistry.get(dashboardId);

        if (dashboard == null) {
            LOG.error("Dashboard not found.");
            throw new WebApplicationException(404);
        }

        final DashboardWidget widget = dashboard.getWidget(widgetId);

        dashboardService.removeWidget(dashboard, widget);

        String msg = "Deleted widget <" + widgetId + "> from dashboard <" + dashboardId + ">. Reason: REST request.";
        LOG.info(msg);
        activityWriter.write(new Activity(msg, DashboardsResource.class));

        return Response.status(Response.Status.NO_CONTENT).build();
    }

    @GET @Timed
    @ApiOperation(value = "Get a single widget value.")
    @Path("/{dashboardId}/widgets/{widgetId}/value")
    @ApiResponses(value = {
            @ApiResponse(code = 404, message = "Dashboard not found."),
            @ApiResponse(code = 404, message = "Widget not found."),
            @ApiResponse(code = 403, message = "Request must be performed against master node."),
            @ApiResponse(code = 504, message = "Computation failed on indexer side.")
    })
    @Produces(MediaType.APPLICATION_JSON)
    public Response widgetValue(@ApiParam(name = "dashboardId", required = true) @PathParam("dashboardId") String dashboardId,
                              @ApiParam(name = "widgetId", required = true) @PathParam("widgetId") String widgetId) {
        restrictToMaster();
        checkPermission(RestPermissions.DASHBOARDS_READ, dashboardId);

        Dashboard dashboard = dashboardRegistry.get(dashboardId);

        if (dashboard == null) {
            LOG.error("Dashboard not found.");
            throw new WebApplicationException(404);
        }

        DashboardWidget widget = dashboard.getWidget(widgetId);

        if (widget == null) {
            LOG.error("Widget not found.");
            throw new WebApplicationException(404);
        }

        try {
            return Response.status(Response.Status.OK).entity(json(widget.getComputationResult().asMap())).build();
        } catch (ExecutionException e) {
            LOG.error("Error while computing dashboard.", e);
            return Response.status(Response.Status.GATEWAY_TIMEOUT).build();
        }
    }

    @PUT @Timed
    @ApiOperation(value = "Update description of a widget")
    @Path("/{dashboardId}/widgets/{widgetId}/description")
    @ApiResponses(value = {
            @ApiResponse(code = 404, message = "Dashboard not found."),
            @ApiResponse(code = 404, message = "Widget not found."),
            @ApiResponse(code = 403, message = "Request must be performed against master node.")
    })
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateDescription(
            @ApiParam(name = "JSON body", required = true) String body,
            @ApiParam(name = "dashboardId", required = true) @PathParam("dashboardId") String dashboardId,
            @ApiParam(name = "widgetId", required = true) @PathParam("widgetId") String widgetId) {
        restrictToMaster();
        checkPermission(RestPermissions.DASHBOARDS_EDIT, dashboardId);

        UpdateWidgetRequest uwr;
        try {
            uwr = objectMapper.readValue(body, UpdateWidgetRequest.class);
        } catch(IOException e) {
            LOG.error("Error while parsing JSON", e);
            throw new WebApplicationException(e, Response.Status.BAD_REQUEST);
        }

        if (dashboardId == null || dashboardId.isEmpty()) {
            LOG.error("Missing dashboard ID. Returning HTTP 400.");
            throw new WebApplicationException(400);
        }

        if (widgetId == null || widgetId.isEmpty()) {
            LOG.error("Missing widget ID. Returning HTTP 400.");
            throw new WebApplicationException(400);
        }

        try {
            Dashboard dashboard = dashboardRegistry.get(dashboardId);

            if (dashboard == null) {
                LOG.error("Dashboard not found.");
                throw new WebApplicationException(404);
            }

            DashboardWidget widget = dashboard.getWidget(widgetId);

            if (widget == null) {
                LOG.error("Widget not found.");
                throw new WebApplicationException(404);
            }

            dashboardService.updateWidgetDescription(dashboard, widget, uwr.description);
        } catch (ValidationException e) {
            LOG.error("Validation error.", e);
            throw new WebApplicationException(e, Response.Status.BAD_REQUEST);
        }

        LOG.info("Updated description of widget <" + widgetId + "> on dashboard <" + dashboardId + ">. Reason: REST request.");
        return Response.status(Response.Status.OK).build();
    }

    @PUT @Timed
    @ApiOperation(value = "Update cache time of a widget")
    @Path("/{dashboardId}/widgets/{widgetId}/cachetime")
    @ApiResponses(value = {
            @ApiResponse(code = 404, message = "Dashboard not found."),
            @ApiResponse(code = 404, message = "Widget not found."),
            @ApiResponse(code = 403, message = "Request must be performed against master node.")
    })
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateCacheTime(
            @ApiParam(name = "JSON body", required = true) String body,
            @ApiParam(name = "dashboardId", required = true) @PathParam("dashboardId") String dashboardId,
            @ApiParam(name = "widgetId", required = true) @PathParam("widgetId") String widgetId) {
        restrictToMaster();
        checkPermission(RestPermissions.DASHBOARDS_EDIT, dashboardId);

        UpdateWidgetRequest uwr;
        try {
            uwr = objectMapper.readValue(body, UpdateWidgetRequest.class);
        } catch(IOException e) {
            LOG.error("Error while parsing JSON", e);
            throw new WebApplicationException(e, Response.Status.BAD_REQUEST);
        }

        if (dashboardId == null || dashboardId.isEmpty()) {
            LOG.error("Missing dashboard ID. Returning HTTP 400.");
            throw new WebApplicationException(400);
        }

        if (widgetId == null || widgetId.isEmpty()) {
            LOG.error("Missing widget ID. Returning HTTP 400.");
            throw new WebApplicationException(400);
        }

        try {
            Dashboard dashboard = dashboardRegistry.get(dashboardId);

            if (dashboard == null) {
                LOG.error("Dashboard not found.");
                throw new WebApplicationException(404);
            }

            DashboardWidget widget = dashboard.getWidget(widgetId);

            if (widget == null) {
                LOG.error("Widget not found.");
                throw new WebApplicationException(404);
            }

            dashboardService.updateWidgetCacheTime(dashboard, widget, uwr.cacheTime);
        } catch (ValidationException e) {
            LOG.error("Validation error.", e);
            throw new WebApplicationException(e, Response.Status.BAD_REQUEST);
        }

        LOG.info("Updated cache time of widget <" + widgetId + "> on dashboard <" + dashboardId + "> to " +
                "[" + uwr.cacheTime + "]. Reason: REST request.");
        return Response.status(Response.Status.OK).build();
    }

}

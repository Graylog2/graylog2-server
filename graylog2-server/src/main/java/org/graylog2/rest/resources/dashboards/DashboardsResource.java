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
package org.graylog2.rest.resources.dashboards;

import com.beust.jcommander.internal.Lists;
import com.codahale.metrics.annotation.Timed;
import com.google.common.collect.Maps;
import org.bson.types.ObjectId;
import org.graylog2.dashboards.Dashboard;
import org.graylog2.dashboards.widgets.DashboardWidget;
import org.graylog2.database.*;
import org.graylog2.indexer.searches.timeranges.InvalidRangeParametersException;
import org.graylog2.rest.documentation.annotations.*;
import org.graylog2.rest.resources.RestResource;
import org.graylog2.rest.resources.dashboards.requests.AddWidgetRequest;
import org.graylog2.rest.resources.dashboards.requests.CreateRequest;
import org.graylog2.rest.resources.dashboards.requests.UpdateWidgetRequest;
import org.graylog2.system.activities.Activity;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
@Api(value = "Dashboards", description = "Manage dashboards")
@Path("/dashboards")
public class DashboardsResource extends RestResource {
    private static final Logger LOG = LoggerFactory.getLogger(DashboardsResource.class);

    @POST
    @Timed
    @ApiOperation(value = "Create a dashboard")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 403, message = "Request must be performed against master node.")
    })
    public Response create(@ApiParam(title = "JSON body", required = true) String body) {
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
        dashboardData.put("creator_user_id", cr.creatorUserId);
        dashboardData.put("created_at", new DateTime(DateTimeZone.UTC));

        Dashboard dashboard = new Dashboard(dashboardData, core);
        ObjectId id;
        try {
            id = dashboard.save();
        } catch (ValidationException e) {
            LOG.error("Validation error.", e);
            throw new WebApplicationException(e, Response.Status.BAD_REQUEST);
        }

        core.dashboards().add(dashboard);

        Map<String, Object> result = Maps.newHashMap();
        result.put("dashboard_id", id.toStringMongod());

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

        for (Dashboard dashboard: Dashboard.all(core)) {
            dashboards.add(dashboard.asMap());
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
    public String get(@ApiParam(title = "dashboardId", required = true) @PathParam("dashboardId") String dashboardId) {
        restrictToMaster();

        try {
            Dashboard dashboard = Dashboard.load(loadObjectId(dashboardId), core);
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
    public Response delete(@ApiParam(title = "dashboardId", required = true) @PathParam("dashboardId") String dashboardId) {
        restrictToMaster();

        try {
            Dashboard dashboard = Dashboard.load(loadObjectId(dashboardId), core);
            core.dashboards().remove(dashboardId);
            dashboard.destroy();

            String msg = "Deleted dashboard <" + dashboard.getId() + ">. Reason: REST request.";
            LOG.info(msg);
            core.getActivityWriter().write(new Activity(msg, DashboardsResource.class));
        } catch (org.graylog2.database.NotFoundException e) {
            throw new WebApplicationException(404);
        }

        return Response.status(Response.Status.fromStatusCode(204)).build();
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
    public Response addWidget(@ApiParam(title = "JSON body", required = true) String body,
                              @ApiParam(title = "dashboardId", required = true) @PathParam("dashboardId") String dashboardId) {
        restrictToMaster();

        AddWidgetRequest awr;
        try {
            awr = objectMapper.readValue(body, AddWidgetRequest.class);
        } catch(IOException e) {
            LOG.error("Error while parsing JSON", e);
            throw new WebApplicationException(e, Response.Status.BAD_REQUEST);
        }

        DashboardWidget widget;
        try {
            widget = DashboardWidget.fromRequest(core, awr);

            Dashboard dashboard = core.dashboards().get(dashboardId);

            if (dashboard == null) {
                LOG.error("Dashboard not found.");
                throw new WebApplicationException(404);
            }

            dashboard.addWidget(widget);
        } catch (ValidationException e1) {
            LOG.error("Validation error.", e1);
            throw new WebApplicationException(e1, Response.Status.BAD_REQUEST);
        } catch (DashboardWidget.NoSuchWidgetTypeException e2) {
            LOG.error("No such widget type.", e2);
            throw new WebApplicationException(e2, Response.Status.BAD_REQUEST);
        } catch (InvalidRangeParametersException e3) {
            LOG.error("Invalid timerange parameters provided.", e3);
            throw new WebApplicationException(e3, Response.Status.BAD_REQUEST);
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
            @ApiParam(title = "dashboardId", required = true) @PathParam("dashboardId") String dashboardId,
            @ApiParam(title = "widgetId", required = true) @PathParam("widgetId") String widgetId) {
        restrictToMaster();

        if (dashboardId == null || dashboardId.isEmpty()) {
            LOG.error("Missing dashboard ID. Returning HTTP 400.");
            throw new WebApplicationException(400);
        }

        if (widgetId == null || widgetId.isEmpty()) {
            LOG.error("Missing widget ID. Returning HTTP 400.");
            throw new WebApplicationException(400);
        }

        Dashboard dashboard = core.dashboards().get(dashboardId);

        if (dashboard == null) {
            LOG.error("Dashboard not found.");
            throw new WebApplicationException(404);
        }
        dashboard.removeWidget(widgetId);

        String msg = "Deleted widget <" + widgetId + "> from dashboard <" + dashboardId + ">. Reason: REST request.";
        LOG.info(msg);
        core.getActivityWriter().write(new Activity(msg, DashboardsResource.class));

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
    public Response widgetValue(@ApiParam(title = "dashboardId", required = true) @PathParam("dashboardId") String dashboardId,
                              @ApiParam(title = "widgetId", required = true) @PathParam("widgetId") String widgetId) {
        restrictToMaster();

        Dashboard dashboard = core.dashboards().get(dashboardId);

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
            @ApiParam(title = "JSON body", required = true) String body,
            @ApiParam(title = "dashboardId", required = true) @PathParam("dashboardId") String dashboardId,
            @ApiParam(title = "widgetId", required = true) @PathParam("widgetId") String widgetId) {
        restrictToMaster();

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
            Dashboard dashboard = core.dashboards().get(dashboardId);

            if (dashboard == null) {
                LOG.error("Dashboard not found.");
                throw new WebApplicationException(404);
            }

            DashboardWidget widget = dashboard.getWidget(widgetId);

            if (widget == null) {
                LOG.error("Widget not found.");
                throw new WebApplicationException(404);
            }

            dashboard.updateWidgetDescription(widget, uwr.description);
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
            @ApiParam(title = "JSON body", required = true) String body,
            @ApiParam(title = "dashboardId", required = true) @PathParam("dashboardId") String dashboardId,
            @ApiParam(title = "widgetId", required = true) @PathParam("widgetId") String widgetId) {
        restrictToMaster();

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
            Dashboard dashboard = core.dashboards().get(dashboardId);

            if (dashboard == null) {
                LOG.error("Dashboard not found.");
                throw new WebApplicationException(404);
            }

            DashboardWidget widget = dashboard.getWidget(widgetId);

            if (widget == null) {
                LOG.error("Widget not found.");
                throw new WebApplicationException(404);
            }

            dashboard.updateWidgetCacheTime(widget, uwr.cacheTime);
        } catch (ValidationException e) {
            LOG.error("Validation error.", e);
            throw new WebApplicationException(e, Response.Status.BAD_REQUEST);
        }

        LOG.info("Updated cache time of widget <" + widgetId + "> on dashboard <" + dashboardId + "> to " +
                "[" + uwr.cacheTime + "]. Reason: REST request.");
        return Response.status(Response.Status.OK).build();
    }

}

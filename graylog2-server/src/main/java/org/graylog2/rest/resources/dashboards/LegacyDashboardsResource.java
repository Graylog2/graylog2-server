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
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.graylog2.audit.AuditEventTypes;
import org.graylog2.audit.jersey.AuditEvent;
import org.graylog2.dashboards.Dashboard;
import org.graylog2.dashboards.DashboardService;
import org.graylog2.database.NotFoundException;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.database.ValidationException;
import org.graylog2.plugin.database.users.User;
import org.graylog2.rest.models.dashboards.requests.CreateDashboardRequest;
import org.graylog2.rest.models.dashboards.requests.UpdateDashboardRequest;
import org.graylog2.rest.models.dashboards.requests.WidgetPositionsRequest;
import org.graylog2.rest.models.dashboards.responses.DashboardList;
import org.graylog2.shared.rest.resources.RestResource;
import org.graylog2.shared.security.RestPermissions;
import org.graylog2.shared.system.activities.Activity;
import org.graylog2.shared.system.activities.ActivityWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.validation.Valid;
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
import java.util.List;
import java.util.Map;

@RequiresAuthentication
@Api(value = "Legacy/Dashboards", description = "Manage dashboards")
@Path("/legacy/dashboards")
public class LegacyDashboardsResource extends RestResource {
    private static final Logger LOG = LoggerFactory.getLogger(LegacyDashboardsResource.class);

    private final DashboardService dashboardService;
    private final ActivityWriter activityWriter;

    @Inject
    public LegacyDashboardsResource(DashboardService dashboardService,
                                    ActivityWriter activityWriter) {
        this.dashboardService = dashboardService;
        this.activityWriter = activityWriter;
    }

    @POST
    @Timed
    @ApiOperation(value = "Create a dashboard")
    @RequiresPermissions(RestPermissions.DASHBOARDS_CREATE)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @AuditEvent(type = AuditEventTypes.DASHBOARD_CREATE)
    public Response create(@ApiParam(name = "JSON body", required = true) CreateDashboardRequest cr) throws ValidationException {
        // Create dashboard.
        final Dashboard dashboard = dashboardService.create(cr.title(), cr.description(), getCurrentUser().getName(), Tools.nowUTC());
        final String id = dashboardService.save(dashboard);

        final Map<String, String> result = ImmutableMap.of("dashboard_id", id);
        final URI dashboardUri = getUriBuilderToSelf().path(LegacyDashboardsResource.class, "get")
                .build(id);

        final User user = getCurrentUser();
        if (!user.isLocalAdmin()) {
            final List<String> permissions = ImmutableList.<String>builder()
                    .addAll(user.getPermissions())
                    .add(RestPermissions.DASHBOARDS_READ + ":" + id)
                    .add(RestPermissions.DASHBOARDS_EDIT + ":" + id)
                    .build();
            user.setPermissions(permissions);
            userService.save(user);
        }

        return Response.created(dashboardUri).entity(result).build();
    }

    @GET
    @Timed
    @ApiOperation(value = "Get a list of all dashboards and all configurations of their widgets.")
    @Produces(MediaType.APPLICATION_JSON)
    public DashboardList list() {
        final List<Map<String, Object>> dashboards = Lists.newArrayList();
        for (Dashboard dashboard : dashboardService.all()) {
            if (isPermitted(RestPermissions.DASHBOARDS_READ, dashboard.getId())) {
                dashboards.add(dashboard.asMap());
            }
        }

        return DashboardList.create(dashboards.size(), dashboards);
    }

    @GET
    @Timed
    @ApiOperation(value = "Get a single dashboards and all configurations of its widgets.")
    @Path("/{dashboardId}")
    @ApiResponses(value = {
            @ApiResponse(code = 404, message = "Dashboard not found."),
    })
    @Produces(MediaType.APPLICATION_JSON)
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
    })
    @AuditEvent(type = AuditEventTypes.DASHBOARD_DELETE)
    public void delete(@ApiParam(name = "dashboardId", required = true)
                       @PathParam("dashboardId") String dashboardId) throws NotFoundException, ValidationException {
        checkPermission(RestPermissions.DASHBOARDS_EDIT, dashboardId);

        final Dashboard dashboard = dashboardService.load(dashboardId);
        dashboardService.destroy(dashboard);

        final String msg = "Deleted dashboard <" + dashboard.getId() + ">. Reason: REST request.";
        LOG.info(msg);
        activityWriter.write(new Activity(msg, LegacyDashboardsResource.class));
    }

    @PUT
    @Timed
    @ApiOperation(value = "Update the settings of a dashboard.")
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{dashboardId}")
    @ApiResponses(value = {
            @ApiResponse(code = 404, message = "Dashboard not found.")
    })
    @AuditEvent(type = AuditEventTypes.DASHBOARD_UPDATE)
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
    @AuditEvent(type = AuditEventTypes.DASHBOARD_WIDGET_POSITIONS_UPDATE)
    public void setPositions(
            @ApiParam(name = "dashboardId", required = true)
            @PathParam("dashboardId") String dashboardId,
            @ApiParam(name = "JSON body", required = true)
            @Valid WidgetPositionsRequest uwpr) throws NotFoundException, ValidationException {
        checkPermission(RestPermissions.DASHBOARDS_EDIT, dashboardId);

        final Dashboard dashboard = dashboardService.load(dashboardId);
        dashboardService.updateWidgetPositions(dashboard, uwpr);
    }
}

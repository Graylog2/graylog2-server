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
import com.google.common.collect.Lists;
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
import org.graylog2.database.NotFoundException;
import org.graylog2.plugin.database.ValidationException;
import org.graylog2.rest.models.dashboards.responses.DashboardList;
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

    @GET
    @Deprecated
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
    @Deprecated
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
    @Deprecated
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
}

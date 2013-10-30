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
import org.graylog2.database.ValidationException;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.rest.documentation.annotations.Api;
import org.graylog2.rest.documentation.annotations.ApiOperation;
import org.graylog2.rest.documentation.annotations.ApiParam;
import org.graylog2.rest.resources.RestResource;
import org.graylog2.rest.resources.dashboards.requests.CreateRequest;
import org.graylog2.streams.StreamImpl;
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
    public Response create(@ApiParam(title = "JSON body", required = true) String body) {
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

        Map<String, Object> result = Maps.newHashMap();
        result.put("dashboard_id", id.toStringMongod());

        return Response.status(Response.Status.CREATED).entity(json(result)).build();
    }

    @GET @Timed
    @ApiOperation(value = "Get a list of all dashboards")
    @Produces(MediaType.APPLICATION_JSON)
    public String get() {
        List<Map<String, Object>> dashboards = Lists.newArrayList();

        for (Dashboard dashboard: Dashboard.all(core)) {
            dashboards.add(dashboard.asMap());
        }

        Map<String, Object> result = Maps.newHashMap();
        result.put("total", dashboards.size());
        result.put("dashboards", dashboards);

        return json(result);
    }

}

/*
 * Copyright 2014 TORCH GmbH
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
 */
package org.graylog2.rest.resources.filters;

import com.codahale.metrics.annotation.Timed;
import com.google.common.collect.Sets;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog2.database.*;
import org.graylog2.filters.FilterService;
import org.graylog2.filters.blacklist.FilterDescription;
import org.graylog2.rest.documentation.annotations.Api;
import org.graylog2.rest.documentation.annotations.ApiOperation;
import org.graylog2.rest.documentation.annotations.ApiParam;
import org.graylog2.rest.resources.RestResource;
import org.graylog2.security.RestPermissions;
import org.graylog2.users.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Set;

import static javax.ws.rs.core.Response.*;

@RequiresAuthentication
@Api(value = "Filters", description = "Message blacklist filters")
@Path("/filters/blacklist")
public class BlacklistSourceResource extends RestResource {
    private static final Logger log = LoggerFactory.getLogger(BlacklistSourceResource.class);

    private FilterService filterService;

    @Inject
    public BlacklistSourceResource(FilterService filterService) {
        this.filterService = filterService;
    }

    @POST
    @Timed
    @ApiOperation(value = "Create a blacklist filter", notes = "It can take up to a second until the change is applied")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response create(@ApiParam(title = "filterEntry", required = true) FilterDescription filterDescription) {
        checkPermission(RestPermissions.BLACKLISTENTRY_CREATE);

        // force the user name to be consistent with the requesting user
        final User currentUser = getCurrentUser();
        if (currentUser == null) {
            return serverError().entity("Could not load user.").build();
        }
        filterDescription.creatorUserId = currentUser.getName();
        final FilterDescription savedFilter;
        try {
            savedFilter = filterService.save(filterDescription);
        } catch (ValidationException e) {
            throw new BadRequestException(e);
        }
        return accepted().entity(savedFilter).build();
    }

    @GET
    @Timed
    @ApiOperation("Get all blacklist filters")
    @Produces(MediaType.APPLICATION_JSON)
    public Set<FilterDescription> getAll() {
        try {
            return filterService.loadAll();
        } catch (org.graylog2.database.NotFoundException e) {
            return Sets.newHashSet();
        }
    }

    @GET
    @Timed
    @Path("/{filterId}")
    @ApiOperation("Get the existing blacklist filter")
    @Produces(MediaType.APPLICATION_JSON)
    public FilterDescription get(@ApiParam(title = "filterId", required = true) @PathParam("filterId") String filterId) {
        try {
            return filterService.load(filterId);
        } catch (org.graylog2.database.NotFoundException e) {
            throw new NotFoundException();
        }
    }

    @PUT
    @Timed
    @Path("/{filterId}")
    @ApiOperation(value = "Update an existing blacklist filter", notes = "It can take up to a second until the change is applied")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response update(@ApiParam(title = "filterId", required = true) @PathParam("filterId") String filterId,
                           @ApiParam(title = "filterEntry", required = true) FilterDescription filterEntry) {
        FilterDescription filter;
        try {
            filter = filterService.load(filterId);
        } catch (org.graylog2.database.NotFoundException e) {
            return status(Response.Status.NOT_FOUND).build();
        }
        // did the filter type change?
        if (!filter.getClass().equals(filterEntry.getClass())) {
            // copy the relevant fields from the saved filter and then use the new class
            filterEntry._id = filter._id;
            filterEntry.createdAt = filter.createdAt;
            filterEntry.creatorUserId = filter.creatorUserId;
            filter = filterEntry;
        } else {
            // just copy the changable fields
            filter.description = filterEntry.description;
            filter.fieldName = filterEntry.fieldName;
            filter.name = filterEntry.name;
            filter.pattern = filterEntry.pattern;
        }
        try {
            filterService.save(filter);
        } catch (ValidationException e) {
            return status(Status.BAD_REQUEST).entity(e.getMessage()).build();
        }
        return ok().build();
    }

    @DELETE
    @Timed
    @ApiOperation(value = "Remove the existing blacklist filter", notes = "It can take up to a second until the change is applied")
    @Path("/{filterId}")
    public Response delete(@ApiParam(title = "filterId", required = true) @PathParam("filterId") String filterId) {
        final int deleted = filterService.delete(filterId);
        if (deleted == 0) {
            return status(Status.NOT_FOUND).build();
        }
        return Response.accepted().build();
    }
}

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
package org.graylog2.rest.resources.filters;

import com.codahale.metrics.annotation.Timed;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog2.plugin.database.ValidationException;
import org.graylog2.filters.FilterService;
import org.graylog2.filters.blacklist.FilterDescription;
import org.graylog2.shared.rest.resources.RestResource;
import org.graylog2.shared.security.RestPermissions;
import org.graylog2.plugin.database.users.User;
import org.hibernate.validator.constraints.NotEmpty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.Collections;
import java.util.Set;

@RequiresAuthentication
@Api(value = "Filters", description = "Message blacklist filters")
@Path("/filters/blacklist")
public class BlacklistSourceResource extends RestResource {
    private static final Logger LOG = LoggerFactory.getLogger(BlacklistSourceResource.class);

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
    public Response create(@ApiParam(name = "filterEntry", required = true)
                           @Valid @NotNull FilterDescription filterDescription) throws ValidationException {
        checkPermission(RestPermissions.BLACKLISTENTRY_CREATE);

        // force the user name to be consistent with the requesting user
        final User currentUser = getCurrentUser();
        if (currentUser == null) {
            throw new InternalServerErrorException("Could not load user.");
        }

        filterDescription.creatorUserId = currentUser.getName();

        final FilterDescription savedFilter = filterService.save(filterDescription);

        final URI filterUri = getUriBuilderToSelf().path(BlacklistSourceResource.class)
                .path("{filterId}")
                .build(savedFilter._id);

        return Response.created(filterUri).entity(savedFilter).build();

    }

    @GET
    @Timed
    @ApiOperation("Get all blacklist filters")
    @Produces(MediaType.APPLICATION_JSON)
    public Set<FilterDescription> getAll() {
        try {
            return filterService.loadAll();
        } catch (org.graylog2.database.NotFoundException e) {
            return Collections.emptySet();
        }
    }

    @GET
    @Timed
    @Path("/{filterId}")
    @ApiOperation("Get the existing blacklist filter")
    @Produces(MediaType.APPLICATION_JSON)
    public FilterDescription get(@ApiParam(name = "filterId", required = true)
                                 @PathParam("filterId")
                                 @NotEmpty String filterId) throws org.graylog2.database.NotFoundException {
        return filterService.load(filterId);
    }

    @PUT
    @Timed
    @Path("/{filterId}")
    @ApiOperation(value = "Update an existing blacklist filter", notes = "It can take up to a second until the change is applied")
    @Consumes(MediaType.APPLICATION_JSON)
    public void update(@ApiParam(name = "filterId", required = true)
                       @PathParam("filterId") String filterId,
                       @ApiParam(name = "filterEntry", required = true) FilterDescription filterEntry) throws org.graylog2.database.NotFoundException, ValidationException {
        FilterDescription filter = filterService.load(filterId);

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

        filterService.save(filter);
    }

    @DELETE
    @Timed
    @ApiOperation(value = "Remove the existing blacklist filter", notes = "It can take up to a second until the change is applied")
    @Path("/{filterId}")
    public void delete(@ApiParam(name = "filterId", required = true)
                       @PathParam("filterId") String filterId) {
        if (filterService.delete(filterId) == 0) {
            throw new NotFoundException();
        }
    }
}

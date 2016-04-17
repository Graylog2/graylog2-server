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
package org.graylog2.rest.resources.search;

import com.codahale.metrics.annotation.Timed;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.graylog2.database.NotFoundException;
import org.graylog2.indexer.searches.Searches;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.plugin.database.ValidationException;
import org.graylog2.rest.resources.search.requests.CreateSavedSearchRequest;
import org.graylog2.savedsearches.SavedSearch;
import org.graylog2.savedsearches.SavedSearchService;
import org.graylog2.shared.security.RestPermissions;

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
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.List;
import java.util.Map;

@RequiresAuthentication
@Api(value = "Search/Saved", description = "Saved searches")
@Path("/search/saved")
public class SavedSearchesResource extends SearchResource {
    private final SavedSearchService savedSearchService;

    @Inject
    public SavedSearchesResource(Searches searches,
                                 SavedSearchService savedSearchService,
                                 ClusterConfigService clusterConfigService) {
        super(searches, clusterConfigService);
        this.savedSearchService = savedSearchService;
    }

    @POST
    @Timed
    @ApiOperation(value = "Create a new saved search")
    @RequiresPermissions(RestPermissions.SAVEDSEARCHES_CREATE)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponse(code = 400, message = "Validation error")
    public Response create(@ApiParam(name = "JSON body", required = true)
                           @Valid CreateSavedSearchRequest cr) throws ValidationException {
        if (!isTitleTaken("", cr.title())) {
            final String msg = "Cannot save search " + cr.title() + ". Title is already taken.";
            throw new BadRequestException(msg);
        }

        final SavedSearch search = savedSearchService.create(cr.title(), cr.query(), getCurrentUser().getName(), Tools.nowUTC());
        final String id = savedSearchService.save(search);

        final URI searchUri = getUriBuilderToSelf().path(SavedSearchesResource.class)
                .path("{searchId}")
                .build(id);

        return Response.created(searchUri).build();
    }

    @GET
    @Timed
    @ApiOperation(value = "Get a list of all saved searches")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Object> list() {
        final List<Map<String, Object>> searches = Lists.newArrayList();
        for (SavedSearch search : savedSearchService.all()) {
            if (isPermitted(RestPermissions.SAVEDSEARCHES_READ, search.getId())) {
                searches.add(search.asMap());
            }
        }

        return ImmutableMap.of(
                "total", searches.size(),
                "searches", searches);
    }

    @PUT
    @Path("/{searchId}")
    @Timed
    @RequiresPermissions(RestPermissions.SAVEDSEARCHES_EDIT)
    @ApiOperation(value = "Update a saved search")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 404, message = "Saved search not found."),
            @ApiResponse(code = 400, message = "Invalid ObjectId."),
            @ApiResponse(code = 400, message = "Validation error")
    })
    public Map<String, Object> update(@ApiParam(name = "searchId", required = true)
                                      @PathParam("searchId") String searchId,
                                      @ApiParam(name = "JSON body", required = true)
                                      @Valid CreateSavedSearchRequest cr) throws NotFoundException, ValidationException {
        final SavedSearch search = savedSearchService.load(searchId);

        if (!isTitleTaken(searchId, cr.title())) {
            final String msg = "Cannot save search " + cr.title() + ". Title is already taken.";
            throw new BadRequestException(msg);
        }

        savedSearchService.update(search, cr.title(), cr.query());
        return search.asMap();
    }

    @GET
    @Path("/{searchId}")
    @Timed
    @ApiOperation(value = "Get a single saved search")
    @ApiResponses(value = {
            @ApiResponse(code = 404, message = "Saved search not found."),
            @ApiResponse(code = 400, message = "Invalid ObjectId.")
    })
    public Map<String, Object> get(@ApiParam(name = "searchId", required = true)
                                   @PathParam("searchId") String searchId) throws NotFoundException {
        checkPermission(RestPermissions.SAVEDSEARCHES_READ, searchId);

        return savedSearchService.load(searchId).asMap();
    }

    @DELETE
    @Path("/{searchId}")
    @Timed
    @ApiOperation(value = "Delete a saved search")
    @ApiResponses(value = {
            @ApiResponse(code = 404, message = "Saved search not found."),
            @ApiResponse(code = 400, message = "Invalid ObjectId.")
    })
    public void delete(@ApiParam(name = "searchId", required = true)
                       @PathParam("searchId") String searchId) throws NotFoundException {
        checkPermission(RestPermissions.SAVEDSEARCHES_EDIT, searchId);
        final SavedSearch search = savedSearchService.load(searchId);
        savedSearchService.destroy(search);
    }

    private boolean isTitleTaken(String searchId, String title) {
        for (SavedSearch savedSearch : savedSearchService.all()) {
            if (!savedSearch.getId().equals(searchId) && savedSearch.getTitle().equals(title)) {
                return false;
            }
        }

        return true;
    }
}

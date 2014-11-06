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
package org.graylog2.rest.resources.search;

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
import org.graylog2.database.ValidationException;
import org.graylog2.indexer.searches.Searches;
import org.graylog2.plugin.Tools;
import org.graylog2.rest.resources.search.requests.CreateSavedSearchRequest;
import org.graylog2.savedsearches.SavedSearch;
import org.graylog2.savedsearches.SavedSearchImpl;
import org.graylog2.savedsearches.SavedSearchService;
import org.graylog2.security.RestPermissions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
@RequiresAuthentication
@Api(value = "Search/Saved", description = "Saved searches")
@Path("/search/saved")
public class SavedSearchesResource extends SearchResource {

    private static final Logger LOG = LoggerFactory.getLogger(SavedSearchesResource.class);

    private final SavedSearchService savedSearchService;

    @Inject
    public SavedSearchesResource(Searches searches,
                                 SavedSearchService savedSearchService) {
        super(searches);
        this.savedSearchService = savedSearchService;
    }

    @POST
    @Timed
    @ApiOperation(value = "Create a new saved search")
    @RequiresPermissions(RestPermissions.SAVEDSEARCHES_CREATE)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response create(@ApiParam(name = "JSON body", required = true) String body) {
        CreateSavedSearchRequest cr;

        try {
            cr = objectMapper.readValue(body, CreateSavedSearchRequest.class);
        } catch(IOException e) {
            LOG.error("Error while parsing JSON", e);
            throw new WebApplicationException(e, Response.Status.BAD_REQUEST);
        }

        // Create saved search
        Map<String, Object> searchData = Maps.newHashMap();
        searchData.put("title", cr.title);
        searchData.put("query", cr.query);
        searchData.put("creator_user_id", getCurrentUser().getName());
        searchData.put("created_at", Tools.iso8601());

        SavedSearch search = new SavedSearchImpl(searchData);
        String id;
        try {
            id = savedSearchService.save(search);
        } catch (ValidationException e) {
            LOG.error("Validation error.", e);
            throw new WebApplicationException(e, Response.Status.BAD_REQUEST);
        }

        Map<String, Object> result = Maps.newHashMap();
        result.put("search_id", id);

        return Response.status(Response.Status.CREATED).entity(json(result)).build();
    }

    @GET @Timed
    @ApiOperation(value = "Get a list of all saved searches")
    @Produces(MediaType.APPLICATION_JSON)
    public String list() {
        List<Map<String, Object>> searches = Lists.newArrayList();
        for (SavedSearch search : savedSearchService.all()) {
            if (isPermitted(RestPermissions.SAVEDSEARCHES_READ, search.getId())) {
                searches.add(search.asMap());
            }
        }

        Map<String, Object> result = Maps.newHashMap();
        result.put("total", searches.size());
        result.put("searches", searches);

        return json(result);
    }

    @GET @Path("/{searchId}") @Timed
    @ApiOperation(value = "Get a single saved search")
    @ApiResponses(value = {
            @ApiResponse(code = 404, message = "Saved search not found."),
            @ApiResponse(code = 400, message = "Invalid ObjectId.")
    })
    public String get(@ApiParam(name = "searchId", required = true) @PathParam("searchId") String searchId) {
        if (searchId == null || searchId.isEmpty()) {
            LOG.error("Missing searchId. Returning HTTP 400.");
            throw new WebApplicationException(400);
        }
        checkPermission(RestPermissions.SAVEDSEARCHES_READ, searchId);

        try {
            SavedSearch search = savedSearchService.load(searchId);
            return json(search.asMap());
        } catch (org.graylog2.database.NotFoundException e) {
            throw new WebApplicationException(404);
        }
    }

    @DELETE @Path("/{searchId}") @Timed
    @ApiOperation(value = "Delete a saved search")
    @ApiResponses(value = {
            @ApiResponse(code = 404, message = "Saved search not found."),
            @ApiResponse(code = 400, message = "Invalid ObjectId.")
    })
    public Response delete(@ApiParam(name = "searchId", required = true) @PathParam("searchId") String searchId) {
        if (searchId == null || searchId.isEmpty()) {
            LOG.error("Missing searchId. Returning HTTP 400.");
            throw new WebApplicationException(400);
        }
        checkPermission(RestPermissions.SAVEDSEARCHES_EDIT, searchId);
        try {
            SavedSearch search = savedSearchService.load(searchId);
            savedSearchService.destroy(search);
        } catch (org.graylog2.database.NotFoundException e) {
            throw new WebApplicationException(404);
        }

        return Response.status(Response.Status.fromStatusCode(204)).build();
    }

}

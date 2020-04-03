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
import org.graylog2.audit.AuditEventTypes;
import org.graylog2.audit.jersey.AuditEvent;
import org.graylog2.database.NotFoundException;
import org.graylog2.decorators.DecoratorProcessor;
import org.graylog2.indexer.searches.Searches;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.savedsearches.SavedSearch;
import org.graylog2.savedsearches.SavedSearchService;
import org.graylog2.shared.security.RestPermissions;

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
@Api(value = "Legacy/Search/Saved", description = "Saved searches")
@Path("/legacy/search/saved")
public class LegacySavedSearchesResource extends SearchResource {
    private final SavedSearchService savedSearchService;

    @Inject
    public LegacySavedSearchesResource(Searches searches,
                                       SavedSearchService savedSearchService,
                                       ClusterConfigService clusterConfigService,
                                       DecoratorProcessor decoratorProcessor) {
        super(searches, clusterConfigService, decoratorProcessor);
        this.savedSearchService = savedSearchService;
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
    @AuditEvent(type = AuditEventTypes.SAVED_SEARCH_DELETE)
    public void delete(@ApiParam(name = "searchId", required = true)
                       @PathParam("searchId") String searchId) throws NotFoundException {
        checkPermission(RestPermissions.SAVEDSEARCHES_EDIT, searchId);
        final SavedSearch search = savedSearchService.load(searchId);
        savedSearchService.destroy(search);
    }
}

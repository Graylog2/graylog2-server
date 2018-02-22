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
package org.graylog.plugins.map.rest;

import com.codahale.metrics.annotation.Timed;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog.plugins.map.search.MapDataSearch;
import org.graylog.plugins.map.search.MapDataSearchRequest;
import org.graylog.plugins.map.search.MapDataSearchResult;
import org.graylog2.audit.jersey.NoAuditEvent;
import org.graylog2.decorators.DecoratorProcessor;
import org.graylog2.indexer.searches.Searches;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.plugin.indexer.searches.timeranges.AbsoluteRange;
import org.graylog2.plugin.indexer.searches.timeranges.KeywordRange;
import org.graylog2.plugin.indexer.searches.timeranges.RelativeRange;
import org.graylog2.plugin.rest.PluginRestResource;
import org.graylog2.rest.resources.search.SearchResource;
import org.graylog2.shared.security.RestPermissions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@RequiresAuthentication
@Api(value = "MapWidget", description = "Get map data")
@Path("/search/mapdata")
public class MapDataResource extends SearchResource implements PluginRestResource {
    private static final Logger LOG = LoggerFactory.getLogger(MapDataResource.class);

    private final MapDataSearch search;

    @Inject
    public MapDataResource(MapDataSearch search, Searches searches, ClusterConfigService clusterConfigService, DecoratorProcessor decoratorProcessor) {
        super(searches, clusterConfigService, decoratorProcessor);
        this.search = search;
    }

    @POST
    @Timed
    @ApiOperation(value = "Get map data")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @NoAuditEvent("only used to fetch map data, no changes made in the system")
    public MapDataSearchResult mapData(@ApiParam(name = "JSON body", required = true) MapDataSearchRequest request) {
        final String filter = "streams:" + request.streamId();

        switch (request.timerange().type()) {
            case AbsoluteRange.ABSOLUTE:
                checkSearchPermission(filter, RestPermissions.SEARCHES_ABSOLUTE);
                break;
            case RelativeRange.RELATIVE:
                checkSearchPermission(filter, RestPermissions.SEARCHES_RELATIVE);
                break;
            case KeywordRange.KEYWORD:
                checkSearchPermission(filter, RestPermissions.SEARCHES_KEYWORD);
                break;
        }

        try {
            return search.searchMapData(request);
        } catch (MapDataSearch.ValueTypeException e) {
            LOG.error("Map data query failed: {}", e.getMessage());
            throw new BadRequestException(e.getMessage());
        }
    }
}

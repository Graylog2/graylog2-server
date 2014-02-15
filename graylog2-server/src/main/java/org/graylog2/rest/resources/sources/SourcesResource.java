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
package org.graylog2.rest.resources.sources;

import com.codahale.metrics.annotation.Timed;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Maps;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.graylog2.indexer.IndexHelper;
import org.graylog2.indexer.results.TermsResult;
import org.graylog2.indexer.searches.timeranges.InvalidRangeParametersException;
import org.graylog2.indexer.searches.timeranges.RelativeRange;
import org.graylog2.rest.documentation.annotations.*;
import org.graylog2.rest.resources.RestResource;
import org.graylog2.security.RestPermissions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
@RequiresAuthentication
@RequiresPermissions(RestPermissions.SOURCES_READ)
@Api(value = "Sources", description = "Listing message sources (e.g. hosts sending logs)")
@Path("/sources")
public class SourcesResource extends RestResource {

    private static final Logger LOG = LoggerFactory.getLogger(SourcesResource.class);

    private static final String CACHE_KEY = "sources_list";

    private static final Cache<String, TermsResult> cache = CacheBuilder.newBuilder()
            .expireAfterWrite(10, TimeUnit.SECONDS)
            .build();

    @GET @Timed
    @ApiOperation(
            value = "Get a list of all sources (not more than 5000) that have messages in the current indices. " +
                    "The result is cached for 10 seconds.",
            notes = "Range: The parameter is in seconds relative to the current time. 86400 means 'in the last day'," +
                    "0 is special and means 'across all indices'")
    @ApiResponses(value = {
            @ApiResponse(code = 400, message = "Invalid range parameter provided.")
    })

    @Produces(MediaType.APPLICATION_JSON)
    public String list(
            @ApiParam(title = "range", description = "Relative timeframe to search in. See method description.", required = true)
            @QueryParam("range")
            final int range) {
        TermsResult sources;
        try {
            sources = cache.get(CACHE_KEY + range, new Callable<TermsResult>() {
                @Override
                public TermsResult call() throws Exception {
                    try {
                        return core.getIndexer().searches().terms("source", 5000, "*", new RelativeRange(range));
                    } catch (IndexHelper.InvalidRangeFormatException e) {
                        throw new ExecutionException(e);
                    } catch (InvalidRangeParametersException e) {
                        throw new ExecutionException(e);
                    }
                }
            });
        } catch (ExecutionException e) {
            if (e.getCause() instanceof InvalidRangeParametersException) {
                LOG.error("Invalid relative time range value.", e);
                throw new BadRequestException("Invalid time range " + range);
            } else {
                LOG.error("Could not calculate list of sources.", e);
                throw new WebApplicationException(500);
            }
        }

        Map<String, Object> result = Maps.newHashMap();
        result.put("total", sources.getTerms().size());
        result.put("sources", sources.getTerms());
        result.put("took_ms", sources.took().millis());
        result.put("range", range);

        return json(result);
    }

}

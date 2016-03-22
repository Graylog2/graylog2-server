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
package org.graylog2.rest.resources.sources;

import com.codahale.metrics.annotation.Timed;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.graylog2.indexer.InvalidRangeFormatException;
import org.graylog2.indexer.results.TermsResult;
import org.graylog2.indexer.searches.Searches;
import org.graylog2.plugin.indexer.searches.timeranges.InvalidRangeParametersException;
import org.graylog2.plugin.indexer.searches.timeranges.RelativeRange;
import org.graylog2.rest.models.sources.responses.SourcesList;
import org.graylog2.shared.rest.resources.RestResource;
import org.graylog2.shared.security.RestPermissions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.validation.constraints.Min;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@RequiresAuthentication
@RequiresPermissions(RestPermissions.SOURCES_READ)
@Api(value = "Sources", description = "Listing message sources (e.g. hosts sending logs)")
@Path("/sources")
public class SourcesResource extends RestResource {
    private static final Logger LOG = LoggerFactory.getLogger(SourcesResource.class);
    private static final Cache<Integer, TermsResult> CACHE = CacheBuilder.newBuilder()
            .expireAfterWrite(10, TimeUnit.SECONDS)
            .build();

    private final Searches searches;

    @Inject
    public SourcesResource(Searches searches) {
        this.searches = searches;
    }

    @GET
    @Timed
    @ApiOperation(
            value = "Get a list of all sources (not more than 5000) that have messages in the current indices. " +
                    "The result is cached for 10 seconds.",
            notes = "Range: The parameter is in seconds relative to the current time. 86400 means 'in the last day'," +
                    "0 is special and means 'across all indices'")
    @ApiResponses(value = {
            @ApiResponse(code = 400, message = "Invalid range parameter provided.")
    })
    @Produces(MediaType.APPLICATION_JSON)
    public SourcesList list(
            @ApiParam(name = "range", value = "Relative timeframe to search in. See method description.", required = true)
            @QueryParam("range") @Min(0) final int range,
            @ApiParam(name = "size", value = "Maximum size of the result set.", required = false, defaultValue = "5000")
            @QueryParam("size") @DefaultValue("5000") @Min(0) final int size) {
        final TermsResult sources;
        try {
            sources = CACHE.get(range, new Callable<TermsResult>() {
                @Override
                public TermsResult call() throws Exception {
                    try {
                        return searches.terms("source", size, "*", RelativeRange.create(range));
                    } catch (InvalidRangeParametersException | InvalidRangeFormatException e) {
                        throw new ExecutionException(e);
                    }
                }
            });
        } catch (ExecutionException e) {
            if (e.getCause() instanceof InvalidRangeParametersException) {
                LOG.error("Invalid relative time range: " + range, e);
                throw new BadRequestException("Invalid time range: " + range, e);
            } else {
                LOG.error("Could not calculate list of sources.", e);
                throw new InternalServerErrorException(e);
            }
        }

        return SourcesList.create(sources.getTerms().size(), sources.getTerms(), sources.took().millis(), range);
    }
}

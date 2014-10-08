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
package org.graylog2.rest.resources.system.indexer;

import com.codahale.metrics.annotation.Timed;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.graylog2.indexer.IndexFailure;
import org.graylog2.indexer.IndexFailureService;
import org.graylog2.rest.resources.RestResource;
import org.graylog2.rest.resources.system.indexer.responses.FailureCount;
import org.graylog2.security.RestPermissions;
import org.hibernate.validator.constraints.NotEmpty;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.validation.constraints.Min;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import java.util.List;
import java.util.Map;

@RequiresAuthentication
@Api(value = "Indexer/Failures", description = "Indexer failures")
@Path("/system/indexer/failures")
public class FailuresResource extends RestResource {
    private static final Logger LOG = LoggerFactory.getLogger(FailuresResource.class);

    @Inject
    private IndexFailureService indexFailureService;

    @GET
    @Timed
    @ApiOperation(value = "Total count of failed index operations since the given date.")
    @ApiResponses(value = {
            @ApiResponse(code = 400, message = "Invalid date parameter provided.")
    })
    @RequiresPermissions(RestPermissions.INDICES_FAILURES)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("count")
    public FailureCount count(@ApiParam(name = "since", value = "ISO8601 date", required = true)
                              @QueryParam("since") @NotEmpty String since) {
        final DateTime sinceDate;
        try {
            sinceDate = DateTime.parse(since);
        } catch (IllegalArgumentException e) {
            final String msg = "Invalid date parameter provided: [" + since + "]";
            LOG.error(msg, e);
            throw new BadRequestException(msg);
        }

        return FailureCount.create(indexFailureService.countSince(sinceDate));
    }

    @GET
    @Timed
    @ApiOperation(value = "Get a list of failed index operations.")
    @RequiresPermissions(RestPermissions.INDICES_FAILURES)
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Object> single(@ApiParam(name = "limit", value = "Limit", required = true)
                                      @QueryParam("limit") @Min(0) int limit,
                                      @ApiParam(name = "offset", value = "Offset", required = true)
                                      @QueryParam("offset") @Min(0) int offset) {
        final List<Map<String, Object>> failures = Lists.newArrayList();
        for (IndexFailure failure : indexFailureService.all(limit, offset)) {
            failures.add(failure.asMap());
        }

        return ImmutableMap.of(
                "failures", failures,
                "total", indexFailureService.totalCount());
    }
}

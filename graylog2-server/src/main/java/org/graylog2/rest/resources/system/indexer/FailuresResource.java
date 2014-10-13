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
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog2.indexer.IndexFailure;
import org.graylog2.indexer.IndexFailureService;
import com.wordnik.swagger.annotations.*;
import org.graylog2.rest.resources.RestResource;
import org.graylog2.security.RestPermissions;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Map;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
@RequiresAuthentication
@Api(value = "Indexer/Failures", description = "Indexer failures")
@Path("/system/indexer/failures")
public class FailuresResource extends RestResource {

    private static final Logger LOG = LoggerFactory.getLogger(FailuresResource.class);

    @Inject
    private IndexFailureService indexFailureService;

    @GET @Timed
    @ApiOperation(value = "Total count of failed index operations since the given date.")
    @ApiResponses(value = {
            @ApiResponse(code = 400, message = "Invalid date parameter provided.")
    })
    @Produces(MediaType.APPLICATION_JSON)
    @Path("count")
    public Response count(@ApiParam(name = "since", value = "ISO8601 date", required = false) @QueryParam("since") String since) {
        checkPermission(RestPermissions.INDICES_FAILURES);

        DateTime sinceDate;
        try {
            sinceDate = DateTime.parse(since);
        } catch(IllegalArgumentException e) {
            LOG.error("Invalid date parameter provided: [" + since + "]", e);
            throw new WebApplicationException(400);
        }

        Map<String, Long> result = Maps.newHashMap();
        result.put("count", indexFailureService.countSince(sinceDate));

        return Response.ok().entity(json(result)).build();
    }

    @GET @Timed
    @ApiOperation(value = "Get a list of failed index operations.")
    @Produces(MediaType.APPLICATION_JSON)
    public Response single(@ApiParam(name = "limit", value = "Limit", required = true) @QueryParam("limit") int limit,
                           @ApiParam(name = "offset", value = "Offset", required = true) @QueryParam("offset") int offset) {
        checkPermission(RestPermissions.INDICES_FAILURES);

        Map<String, Object> result = Maps.newHashMap();

        List<Map<String, Object>> failures = Lists.newArrayList();
        for (IndexFailure failure : indexFailureService.all(limit, offset)) {
            failures.add(failure.asMap());
        }

        result.put("failures", failures);
        result.put("total", indexFailureService.totalCount());

        return Response.ok().entity(json(result)).build();
    }

}

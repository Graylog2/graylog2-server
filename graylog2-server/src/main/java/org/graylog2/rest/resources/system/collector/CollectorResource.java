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
package org.graylog2.rest.resources.system.collector;

import com.codahale.metrics.annotation.Timed;
import com.google.common.base.Function;
import com.google.common.primitives.Ints;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.graylog2.Configuration;
import org.graylog2.collectors.Collector;
import org.graylog2.collectors.CollectorService;
import org.graylog2.collectors.Collectors;
import org.graylog2.database.NotFoundException;
import org.graylog2.rest.models.collector.responses.CollectorList;
import org.graylog2.rest.models.collector.responses.CollectorSummary;
import org.graylog2.shared.rest.resources.RestResource;
import org.graylog2.shared.security.RestPermissions;
import org.joda.time.DateTime;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.List;

@Api(value = "System/Collectors", description = "Management of Graylog Collectors.")
@Path("/system/collectors")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@RequiresAuthentication
@RequiresPermissions(RestPermissions.COLLECTORS_READ)
public class CollectorResource extends RestResource {
    private final CollectorService collectorService;
    private final LostCollectorFunction lostCollectorFunction;

    public class LostCollectorFunction implements Function<Collector, Boolean> {
        private final long timeOutInSeconds;

        @Inject
        public LostCollectorFunction(long timeOutInSeconds) {
            this.timeOutInSeconds = timeOutInSeconds;
        }

        @Override
        public Boolean apply(Collector collector) {
            final DateTime threshold = DateTime.now().minusSeconds(Ints.checkedCast(timeOutInSeconds));
            return collector.getLastSeen().isAfter(threshold);
        }
    }

    @Inject
    public CollectorResource(CollectorService collectorService, Configuration config) {
        this.collectorService = collectorService;
        this.lostCollectorFunction = new LostCollectorFunction(config.getCollectorInactiveThreshold().toSeconds());
    }

    @GET
    @Timed
    @ApiOperation(value = "Lists all existing collector registrations")
    public CollectorList list() {
        final List<Collector> collectors = collectorService.all();
        final List<CollectorSummary> collectorSummaries = Collectors.toSummaryList(collectors, lostCollectorFunction);
        return CollectorList.create(collectorSummaries);
    }

    @GET
    @Timed
    @Path("/{collectorId}")
    @ApiOperation(value = "Returns at most one collector summary for the specified collector id")
    @ApiResponses(value = {
            @ApiResponse(code = 404, message = "No collector with the specified id exists")
    })
    public CollectorSummary get(@ApiParam(name = "collectorId", required = true)
                            @PathParam("collectorId") String collectorId) throws NotFoundException {
        final Collector collector = collectorService.findById(collectorId);

        if (collector != null)
            return collector.toSummary(lostCollectorFunction);
        else
            throw new NotFoundException("Collector <" + collectorId + "> not found!");
    }
}

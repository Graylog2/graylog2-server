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
package org.graylog2.rest.resources.system;


import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog2.shared.rest.resources.RestResource;
import org.graylog2.system.traffic.TrafficCounterService;
import org.joda.time.Duration;

import javax.inject.Inject;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

@Api(value = "System/ClusterTraffic", description = "Cluster traffic stats")
@RequiresAuthentication
@Path("/system/cluster/traffic")
@Produces(MediaType.APPLICATION_JSON)
public class TrafficResource extends RestResource {

    private final TrafficCounterService trafficCounterService;

    @Inject
    public TrafficResource(TrafficCounterService trafficCounterService) {
        this.trafficCounterService = trafficCounterService;
    }

    @GET
    @ApiOperation(value = "Get the cluster traffic stats")
    public TrafficCounterService.TrafficHistogram get(@ApiParam(name = "days", value = "For how many days the traffic stats should be returned")
                                                      @QueryParam("days") @DefaultValue("30") int days,
                                                      @ApiParam(name = "daily", value = "Whether the traffic should be aggregate to daily values")
                                                      @QueryParam("daily") @DefaultValue("false") boolean daily) {
        final TrafficCounterService.TrafficHistogram trafficHistogram =
                trafficCounterService.clusterTrafficOfLastDays(Duration.standardDays(days),
                        daily ? TrafficCounterService.Interval.DAILY : TrafficCounterService.Interval.HOURLY);

        return trafficHistogram;
    }
}

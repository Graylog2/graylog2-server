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
package org.graylog2.shared.rest.resources.system;

import com.codahale.metrics.annotation.Timed;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import org.graylog2.shared.rest.resources.RestResource;
import org.graylog2.rest.models.system.responses.Throughput;
import org.graylog2.shared.security.RestPermissions;
import org.graylog2.shared.stats.ThroughputStats;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@RequiresAuthentication
@Api(value = "System/Throughput", description = "Message throughput of this node")
@Path("/system/throughput")
public class ThroughputResource extends RestResource {
    private final ThroughputStats throughputStats;

    @Inject
    public ThroughputResource(ThroughputStats throughputStats) {
        this.throughputStats = throughputStats;
    }

    @GET
    @Timed
    @RequiresPermissions(RestPermissions.THROUGHPUT_READ)
    @ApiOperation(value = "Current throughput of this node in messages per second")
    @Produces(MediaType.APPLICATION_JSON)
    public Throughput total() {
        return Throughput.create(throughputStats.getCurrentThroughput());
    }
}

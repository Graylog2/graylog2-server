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
package org.graylog2.shared.rest.resources.system;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.annotation.Timed;
import com.google.common.collect.Iterables;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.graylog2.rest.models.system.responses.Throughput;
import org.graylog2.shared.metrics.MetricUtils;
import org.graylog2.shared.rest.resources.RestResource;
import org.graylog2.shared.security.RestPermissions;
import org.graylog2.plugin.GlobalMetricNames;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.SortedMap;

@RequiresAuthentication
@Api(value = "System/Throughput", description = "Message throughput of this node")
@Path("/system/throughput")
public class ThroughputResource extends RestResource {
    private final MetricRegistry metricRegistry;

    @Inject
    public ThroughputResource(MetricRegistry metricRegistry) {
        this.metricRegistry = metricRegistry;
    }

    @GET
    @Timed
    @RequiresPermissions(RestPermissions.THROUGHPUT_READ)
    @ApiOperation(value = "Current throughput of this node in messages per second")
    @Produces(MediaType.APPLICATION_JSON)
    public Throughput total() {
        final SortedMap<String, Gauge> gauges = metricRegistry.getGauges(MetricUtils.filterSingleMetric(
                GlobalMetricNames.OUTPUT_THROUGHPUT_RATE));
        final Gauge gauge = Iterables.getOnlyElement(gauges.values(), null);
        if (gauge == null || !(gauge.getValue() instanceof Number)) {
            return Throughput.create(0);
        } else {
            return Throughput.create(((Number) gauge.getValue()).longValue());
        }
    }
}

/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
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
import org.graylog2.plugin.GlobalMetricNames;
import org.graylog2.rest.models.system.responses.Throughput;
import org.graylog2.shared.metrics.MetricUtils;
import org.graylog2.shared.rest.resources.RestResource;
import org.graylog2.shared.security.RestPermissions;

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

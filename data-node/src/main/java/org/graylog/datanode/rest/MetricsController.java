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

package org.graylog.datanode.rest;

import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.annotation.Timed;
import com.google.common.collect.Lists;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.graylog2.rest.models.system.metrics.requests.MetricsReadRequest;
import org.graylog2.rest.models.system.metrics.responses.MetricNamesResponse;
import org.graylog2.rest.models.system.metrics.responses.MetricsSummaryResponse;
import org.graylog2.shared.metrics.MetricUtils;

import java.util.List;
import java.util.Map;

@Path("/metrics")
@Produces(MediaType.APPLICATION_JSON)
public class MetricsController {

    private final MetricRegistry metricRegistry;

    @Inject
    public MetricsController(MetricRegistry metricRegistry) {
        this.metricRegistry = metricRegistry;
    }

    @GET
    @Timed
    public MetricRegistry metrics() {
        return metricRegistry;
    }

    @GET
    @Timed
    @Path("/names")
    public MetricNamesResponse metricNames() {
        return MetricNamesResponse.create(metricRegistry.getNames());
    }

    @GET
    @Timed
    @Path("/{metricName}")
    public Metric singleMetric(@PathParam("metricName") String metricName) {
        final Metric metric = metricRegistry.getMetrics().get(metricName);
        if (metric == null) {
            final String msg = "I do not have a metric called [" + metricName + "].";
            throw new NotFoundException(msg);
        }
        return metric;
    }

    @POST
    @Timed
    @Path("/multiple")
    public MetricsSummaryResponse multipleMetrics(@Valid @NotNull MetricsReadRequest request) {
        final Map<String, Metric> metrics = metricRegistry.getMetrics();

        final List<Map<String, Object>> metricsList = Lists.newArrayList();

        for (String name : request.metrics()) {
            final Metric metric = metrics.get(name);
            if (metric != null) {
                metricsList.add(MetricUtils.map(name, metric));
            }
        }

        return MetricsSummaryResponse.create(metricsList);
    }
}

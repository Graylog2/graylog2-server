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

import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.annotation.Timed;
import com.google.common.collect.Lists;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.graylog2.audit.jersey.NoAuditEvent;
import org.graylog2.rest.models.system.metrics.requests.MetricsReadRequest;
import org.graylog2.rest.models.system.metrics.responses.MetricNamesResponse;
import org.graylog2.rest.models.system.metrics.responses.MetricsSummaryResponse;
import org.graylog2.shared.metrics.MetricUtils;
import org.graylog2.shared.rest.resources.RestResource;
import org.graylog2.shared.security.RestPermissions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.List;
import java.util.Map;

@RequiresAuthentication
@Api(value = "System/Metrics", description = "Internal Graylog metrics")
@Path("/system/metrics")
public class MetricsResource extends RestResource {

    private static final Logger LOG = LoggerFactory.getLogger(MetricsResource.class);
    private final MetricRegistry metricRegistry;

    @Inject
    public MetricsResource(MetricRegistry metricRegistry) {
        this.metricRegistry = metricRegistry;
    }

    @GET
    @Timed
    @RequiresPermissions(RestPermissions.METRICS_READALL)
    @ApiOperation(value = "Get all metrics",
            notes = "Note that this might return a huge result set.")
    @Produces(MediaType.APPLICATION_JSON)
    public MetricRegistry metrics() {
        return metricRegistry;
    }

    @GET
    @Timed
    @Path("/names")
    @ApiOperation(value = "Get all metrics keys/names")
    @RequiresPermissions(RestPermissions.METRICS_ALLKEYS)
    @Produces(MediaType.APPLICATION_JSON)
    public MetricNamesResponse metricNames() {
        return MetricNamesResponse.create(metricRegistry.getNames());
    }

    @GET
    @Timed
    @Path("/{metricName}")
    @ApiOperation(value = "Get a single metric")
    @ApiResponses(value = {
            @ApiResponse(code = 404, message = "No such metric")
    })
    @Produces(MediaType.APPLICATION_JSON)
    public Metric singleMetric(@ApiParam(name = "metricName", required = true)
                               @PathParam("metricName") String metricName) {
        checkPermission(RestPermissions.METRICS_READ, metricName);

        final Metric metric = metricRegistry.getMetrics().get(metricName);
        if (metric == null) {
            final String msg = "I do not have a metric called [" + metricName + "].";
            LOG.debug(msg);
            throw new NotFoundException(msg);
        }

        return metric;
    }

    @POST
    @Timed
    @Path("/multiple")
    @ApiOperation("Get the values of multiple metrics at once")
    @ApiResponses(value = {
            @ApiResponse(code = 400, message = "Malformed body")
    })
    @NoAuditEvent("only used to retrieve multiple metrics")
    public MetricsSummaryResponse multipleMetrics(@ApiParam(name = "Requested metrics", required = true)
                                  @Valid @NotNull MetricsReadRequest request) {
        final Map<String, Metric> metrics = metricRegistry.getMetrics();

        final List<Map<String, Object>> metricsList = Lists.newArrayList();

        for (String name : request.metrics()) {
            if (!isPermitted(RestPermissions.METRICS_READ, name)) {
                continue;
            }

            final Metric metric = metrics.get(name);
            if (metric != null) {
                metricsList.add(MetricUtils.map(name, metric));
            }
        }

        return MetricsSummaryResponse.create(metricsList);
    }

    @GET
    @Timed
    @Path("/namespace/{namespace}")
    @ApiOperation(value = "Get all metrics of a namespace")
    @ApiResponses(value = {
            @ApiResponse(code = 404, message = "No such metric namespace")
    })
    @Produces(MediaType.APPLICATION_JSON)
    public MetricsSummaryResponse byNamespace(@ApiParam(name = "namespace", required = true)
                              @PathParam("namespace") String namespace) {
        final List<Map<String, Object>> metrics = Lists.newArrayList();
        for (Map.Entry<String, Metric> e : metricRegistry.getMetrics().entrySet()) {
            final String metricName = e.getKey();
            if (metricName.startsWith(namespace) && isPermitted(RestPermissions.METRICS_READ, metricName)) {
                try {
                    final Metric metric = e.getValue();
                    metrics.add(MetricUtils.map(metricName, metric));
                } catch (Exception ex) {
                    LOG.warn("Could not read metric in namespace list.", ex);
                }
            }
        }

        if (metrics.isEmpty()) {
            final String msg = "No metrics with namespace [" + namespace + "] found.";
            LOG.debug(msg);
            throw new NotFoundException(msg);
        }

        return MetricsSummaryResponse.create(metrics);
    }
}

/**
 * Copyright 2013 Lennart Koopmann <lennart@torch.sh>
 *
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
 *
 */
package org.graylog2.rest.resources.system;

import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Metric;
import com.codahale.metrics.Timer;
import com.codahale.metrics.annotation.Timed;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.graylog2.rest.documentation.annotations.*;
import org.graylog2.rest.resources.RestResource;
import org.graylog2.rest.resources.system.requests.MetricsReadRequest;
import org.graylog2.security.RestPermissions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
@RequiresAuthentication
@Api(value = "System/Metrics", description = "Internal Graylog2 metrics")
@Path("/system/metrics")
public class MetricsResource extends RestResource {

    private static final Logger LOG = LoggerFactory.getLogger(MetricsResource.class);

    @GET @Timed
    @RequiresPermissions(RestPermissions.METRICS_READALL)
    @ApiOperation(value = "Get all metrics",
                  notes = "Note that this might return a huge result set.")
    @Produces(MediaType.APPLICATION_JSON)
    public String metrics() {
        Map<String, Object> result = Maps.newHashMap();

        result.put("metrics", core.metrics().getMetrics());

        return json(result);
    }

    @GET @Timed
    @Path("/names")
    @ApiOperation(value = "Get all metrics keys/names")
    @RequiresPermissions(RestPermissions.METRICS_ALLKEYS)
    @Produces(MediaType.APPLICATION_JSON)
    public String metricNames() {
        Map<String, Object> result = Maps.newHashMap();
        result.put("names", core.metrics().getNames());

        return json(result);
    }

    @GET @Timed
    @Path("/{metricName}")
    @ApiOperation(value = "Get a single metric")
    @ApiResponses(value = {
            @ApiResponse(code = 404, message = "No such metric")
    })
    @Produces(MediaType.APPLICATION_JSON)
    public String singleMetric(@ApiParam(title = "metricName", required = true) @PathParam("metricName") String metricName) {
        checkPermission(RestPermissions.METRICS_READ, metricName);
        Metric metric = core.metrics().getMetrics().get(metricName);

        if (metric == null) {
            LOG.debug("I do not have a metric called [{}], returning 404.", metricName);
            throw new WebApplicationException(404);
        }

        return json(metric);
    }

    @POST @Timed
    @Path("/multiple")
    @ApiOperation("Get the values of multiple metrics at once")
    @ApiResponses(value = {
            @ApiResponse(code = 400, message = "Malformed body")
    })
    public String multipleMetrics(@ApiParam(title = "Requested metrics", required = true) MetricsReadRequest request) {
        final Map<String, Metric> metrics = core.metrics().getMetrics();

        List<Map<String, Object>> metricsList = Lists.newArrayList();
        if (request.metrics == null) {
            throw new BadRequestException("Metrics cannot be empty");
        }

        for (String name : request.metrics) {
            if (!isPermitted(RestPermissions.METRICS_READ, name)) {
                continue;
            }

            final Metric metric = metrics.get(name);
            if (metric != null) {
                metricsList.add(getMetricMap(name, metric));
            }
        }

        Map<String, Object> result = Maps.newHashMap();
        result.put("metrics", metricsList);
        result.put("total", metricsList.size());
        return json(result);
    }

    @GET @Timed
    @Path("/namespace/{namespace}")
    @ApiOperation(value = "Get all metrics of a namespace")
    @ApiResponses(value = {
            @ApiResponse(code = 404, message = "No such metric namespace")
    })
    @Produces(MediaType.APPLICATION_JSON)
    public String byNamespace(@ApiParam(title = "namespace", required = true) @PathParam("namespace") String namespace) {
        List<Map<String, Object>> metrics = Lists.newArrayList();

        for(Map.Entry<String, Metric> e : core.metrics().getMetrics().entrySet()) {
            final String metricName = e.getKey();
            if (metricName.startsWith(namespace) && isPermitted(RestPermissions.METRICS_READ, metricName)) {
                try {
                    final Metric metric = e.getValue();
                    Map<String, Object> metricMap = getMetricMap(metricName, metric);

                    metrics.add(metricMap);
                } catch(Exception ex) {
                    LOG.warn("Could not read metric in namespace list.", ex);
                    continue;
                }
            }
        }

        if (metrics.isEmpty()) {
            LOG.debug("No metrics with namespace [{}] found, returning 404.", namespace);
            throw new WebApplicationException(404);
        }

        Map<String, Object> result = Maps.newHashMap();
        result.put("metrics", metrics);
        result.put("total", metrics.size());

        return json(result);
    }

    private Map<String, Object> getMetricMap(String metricName, Metric metric) {
        String type = metric.getClass().getSimpleName().toLowerCase();

        if (type.isEmpty()) {
            type = "gauge";
        }

        Map<String, Object> metricMap = Maps.newHashMap();
        metricMap.put("full_name", metricName);
        metricMap.put("name", metricName.substring(metricName.lastIndexOf(".") + 1));
        metricMap.put("type", type);

        if (metric instanceof Timer) {
            metricMap.put("metric", buildTimerMap((Timer) metric));
        } else if(metric instanceof Meter) {
            metricMap.put("metric", buildMeterMap((Meter) metric));
        } else if(metric instanceof Histogram) {
            metricMap.put("metric", buildHistogramMap((Histogram) metric));
        } else {
            metricMap.put("metric", metric);
        }
        return metricMap;
    }

    enum MetricType {
        GAUGE,
        COUNTER,
        HISTOGRAM,
        METER,
        TIMER
    }

    @GET
    @Timed
    @Path("/{metricName}/history")
    @ApiOperation(value = "Get history of a single metric", notes = "The maximum retention time is currently only 5 minutes.")
    public String historicSingleMetric(
            @ApiParam(title = "metricName", required = true) @PathParam("metricName") String metricName,
            @ApiParam(title = "after", description = "Only values for after this UTC timestamp (1970 epoch)") @QueryParam("after") @DefaultValue("-1") long after
    ) {
        checkPermission(RestPermissions.METRICS_READHISTORY, metricName);
        BasicDBObject andQuery = new BasicDBObject();
        List<BasicDBObject> obj = new ArrayList<BasicDBObject>();
        obj.add(new BasicDBObject("name", metricName));
        if (after != -1) {
            obj.add(new BasicDBObject("$gt",  new BasicDBObject("$gt", new Date(after))));
        }
        andQuery.put("$and", obj);

        final DBCursor cursor = core.getMongoConnection().getDatabase().getCollection("graylog2_metrics")
                .find(andQuery).sort(new BasicDBObject("timestamp", 1));
        Map<String, Object> metricsData = Maps.newHashMap();
        metricsData.put("name", metricName);
        List<Object> values = Lists.newArrayList();
        metricsData.put("values", values);

        while (cursor.hasNext()) {
            final DBObject value = cursor.next();
            metricsData.put("node", value.get("node"));

            final MetricType metricType = MetricType.valueOf(((String) value.get("type")).toUpperCase());
            Map<String, Object> dataPoint = Maps.newHashMap();
            values.add(dataPoint);

            dataPoint.put("timestamp", value.get("timestamp"));
            metricsData.put("type", metricType.toString().toLowerCase());

            switch (metricType) {
                case GAUGE:
                    final Object gaugeValue = value.get("value");
                    dataPoint.put("value", gaugeValue);
                    break;
                case COUNTER:
                    dataPoint.put("count", value.get("count"));
                    break;
                case HISTOGRAM:
                    dataPoint.put("75-percentile", value.get("75-percentile"));
                    dataPoint.put("95-percentile", value.get("95-percentile"));
                    dataPoint.put("98-percentile", value.get("98-percentile"));
                    dataPoint.put("99-percentile", value.get("99-percentile"));
                    dataPoint.put("999-percentile", value.get("999-percentile"));
                    dataPoint.put("max", value.get("max"));
                    dataPoint.put("min", value.get("min"));
                    dataPoint.put("mean", value.get("mean"));
                    dataPoint.put("median", value.get("median"));
                    dataPoint.put("std_dev", value.get("std_dev"));
                    break;
                case METER:
                    dataPoint.put("count", value.get("count"));
                    dataPoint.put("1-minute-rate", value.get("1-minute-rate"));
                    dataPoint.put("5-minute-rate", value.get("5-minute-rate"));
                    dataPoint.put("15-minute-rate", value.get("15-minute-rate"));
                    dataPoint.put("mean-rate", value.get("mean-rate"));
                    break;
                case TIMER:
                    dataPoint.put("count", value.get("count"));
                    dataPoint.put("rate-unit", value.get("rate-unit"));
                    dataPoint.put("1-minute-rate", value.get("1-minute-rate"));
                    dataPoint.put("5-minute-rate", value.get("5-minute-rate"));
                    dataPoint.put("15-minute-rate", value.get("15-minute-rate"));
                    dataPoint.put("mean-rate", value.get("mean-rate"));
                    dataPoint.put("duration-unit", value.get("duration-unit"));
                    dataPoint.put("75-percentile", value.get("75-percentile"));
                    dataPoint.put("95-percentile", value.get("95-percentile"));
                    dataPoint.put("98-percentile", value.get("98-percentile"));
                    dataPoint.put("99-percentile", value.get("99-percentile"));
                    dataPoint.put("999-percentile", value.get("999-percentile"));
                    dataPoint.put("max", value.get("max"));
                    dataPoint.put("min", value.get("min"));
                    dataPoint.put("mean", value.get("mean"));
                    dataPoint.put("median", value.get("median"));
                    dataPoint.put("stddev", value.get("stddev"));
                    break;
            }

        }

        return json(metricsData);
    }
}

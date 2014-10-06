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
package org.graylog2.rest.resources.system;

import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.annotation.Timed;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.graylog2.database.MongoConnection;
import org.graylog2.metrics.MetricUtils;
import org.graylog2.rest.resources.RestResource;
import org.graylog2.rest.resources.system.requests.MetricsReadRequest;
import org.graylog2.security.RestPermissions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;

@RequiresAuthentication
@Api(value = "System/Metrics", description = "Internal Graylog2 metrics")
@Path("/system/metrics")
public class MetricsResource extends RestResource {

    private static final Logger LOG = LoggerFactory.getLogger(MetricsResource.class);
    private final MetricRegistry metricRegistry;
    private final MongoConnection mongoConnection;

    @Inject
    public MetricsResource(MetricRegistry metricRegistry,
                           MongoConnection mongoConnection) {
        this.metricRegistry = metricRegistry;
        this.mongoConnection = mongoConnection;
    }

    @GET
    @Timed
    @RequiresPermissions(RestPermissions.METRICS_READALL)
    @ApiOperation(value = "Get all metrics",
            notes = "Note that this might return a huge result set.")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Map<String, Metric>> metrics() {
        return ImmutableMap.of("metrics", metricRegistry.getMetrics());
    }

    @GET
    @Timed
    @Path("/names")
    @ApiOperation(value = "Get all metrics keys/names")
    @RequiresPermissions(RestPermissions.METRICS_ALLKEYS)
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, SortedSet<String>> metricNames() {
        return ImmutableMap.of("names", metricRegistry.getNames());
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
            LOG.debug("I do not have a metric called [{}], returning 404.", metricName);
            throw new NotFoundException();
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
    public Map<String, Object> multipleMetrics(@ApiParam(name = "Requested metrics", required = true)
                                               @Valid @NotNull MetricsReadRequest request) {
        final Map<String, Metric> metrics = metricRegistry.getMetrics();

        final List<Map<String, Object>> metricsList = Lists.newArrayList();
        if (request.metrics == null) {
            throw new BadRequestException("Metrics cannot be empty");
        }

        for (String name : request.metrics) {
            if (!isPermitted(RestPermissions.METRICS_READ, name)) {
                continue;
            }

            final Metric metric = metrics.get(name);
            if (metric != null) {
                metricsList.add(MetricUtils.map(name, metric));
            }
        }

        return ImmutableMap.of(
                "metrics", metricsList,
                "total", metricsList.size());
    }

    @GET
    @Timed
    @Path("/namespace/{namespace}")
    @ApiOperation(value = "Get all metrics of a namespace")
    @ApiResponses(value = {
            @ApiResponse(code = 404, message = "No such metric namespace")
    })
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Object> byNamespace(@ApiParam(name = "namespace", required = true)
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
            LOG.debug("No metrics with namespace [{}] found, returning 404.", namespace);
            throw new NotFoundException();
        }

        return ImmutableMap.of(
                "metrics", metrics,
                "total", metrics.size());
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
    public Map<String, Object> historicSingleMetric(
            @ApiParam(name = "metricName", required = true)
            @PathParam("metricName") String metricName,
            @ApiParam(name = "after", value = "Only values for after this UTC timestamp (1970 epoch)")
            @QueryParam("after") @DefaultValue("-1") long after
    ) {
        checkPermission(RestPermissions.METRICS_READHISTORY, metricName);

        BasicDBObject andQuery = new BasicDBObject();

        final List<BasicDBObject> obj = new ArrayList<BasicDBObject>();
        obj.add(new BasicDBObject("name", metricName));
        if (after != -1) {
            obj.add(new BasicDBObject("$gt", new BasicDBObject("$gt", new Date(after))));
        }
        andQuery.put("$and", obj);

        final DBCursor cursor = mongoConnection.getDatabase().getCollection("graylog2_metrics")
                .find(andQuery).sort(new BasicDBObject("timestamp", 1));

        final Map<String, Object> metricsData = Maps.newHashMap();
        metricsData.put("name", metricName);
        final List<Object> values = Lists.newArrayList();
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

        return metricsData;
    }
}

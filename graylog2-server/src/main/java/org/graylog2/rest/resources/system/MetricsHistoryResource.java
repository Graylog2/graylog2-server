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

import com.codahale.metrics.annotation.Timed;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog2.database.MongoConnection;
import org.graylog2.shared.rest.resources.RestResource;
import org.graylog2.shared.security.RestPermissions;

import javax.inject.Inject;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@RequiresAuthentication
@Api(value = "System/Metrics/History", description = "Get history of metrics")
@Path("/system/metrics/{metricName}/history")
@Produces(MediaType.APPLICATION_JSON)
public class MetricsHistoryResource extends RestResource {
    private final MongoConnection mongoConnection;

    @Inject
    public MetricsHistoryResource(MongoConnection mongoConnection) {
        this.mongoConnection = mongoConnection;
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

            final MetricType metricType = MetricType.valueOf(((String) value.get("type")).toUpperCase(Locale.ENGLISH));
            Map<String, Object> dataPoint = Maps.newHashMap();
            values.add(dataPoint);

            dataPoint.put("timestamp", value.get("timestamp"));
            metricsData.put("type", metricType.toString().toLowerCase(Locale.ENGLISH));

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

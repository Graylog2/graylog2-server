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
package org.graylog2.rest.resources.system.monitoring;

import com.codahale.metrics.annotation.Timed;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog.plugins.views.search.engine.QueryExecutionStats;
import org.graylog.plugins.views.search.engine.monitoring.collection.QueryExecutionStatsCollector;
import org.graylog.plugins.views.search.engine.monitoring.data.histogram.Histogram;
import org.graylog.plugins.views.search.engine.monitoring.data.histogram.creation.TimerangeHistogramCreation;
import org.graylog.plugins.views.search.engine.monitoring.data.time.PeriodChooser;
import org.graylog2.indexer.searches.SearchesClusterConfig;
import org.graylog2.rest.MoreMediaTypes;
import org.graylog2.shared.rest.resources.RestResource;
import org.joda.time.Period;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiFunction;

import static org.graylog2.shared.rest.documentation.generator.Generator.CLOUD_VISIBLE;

@RequiresAuthentication
@Api(value = "System/Monitoring", tags = {CLOUD_VISIBLE})
@Path("/system/monitoring")
public class MonitoringResource extends RestResource {

    public static final String AVG_FUNCTION_NAME = "Avg. duration (ms)";
    public static final String MAX_FUNCTION_NAME = "Max. duration (ms)";
    public static final String PERCENT_FUNCTION_NAME = "Percent. of recent queries";
    private final QueryExecutionStatsCollector<QueryExecutionStats> executionStatsCollector;
    private final TimerangeHistogramCreation<Period, QueryExecutionStats> timerangeHistogramCreation;


    @Inject
    public MonitoringResource(final QueryExecutionStatsCollector<QueryExecutionStats> executionStatsCollector) {
        this.executionStatsCollector = executionStatsCollector;
        Map<String, BiFunction<Collection<QueryExecutionStats>, Integer, Number>> valueFunctions = new LinkedHashMap<>();
        valueFunctions.put(MonitoringResource.AVG_FUNCTION_NAME,
                (executionStats, numTotalStats) -> (long) executionStats.stream().mapToLong(QueryExecutionStats::duration).average().orElse(0L));
        valueFunctions.put(MonitoringResource.MAX_FUNCTION_NAME,
                (executionStats, numTotalStats) -> executionStats.stream().mapToLong(QueryExecutionStats::duration).max().orElse(0L));
        valueFunctions.put(MonitoringResource.PERCENT_FUNCTION_NAME,
                (executionStats, numTotalStats) -> (long) (100 * (numTotalStats > 0 ? (float) executionStats.size() / numTotalStats : 0L)));
        this.timerangeHistogramCreation = new TimerangeHistogramCreation<>(
                new ArrayList<>(SearchesClusterConfig.createDefault().relativeTimerangeOptions().keySet()),
                new PeriodChooser(),
                valueFunctions
        );
    }

    @GET
    @Timed
    @ApiOperation(value = "Get timerange-based histogram of queries durations and percentage in recent query population")
    @Path("timerange_histogram")
    @Produces({MediaType.APPLICATION_JSON, MoreMediaTypes.TEXT_CSV})
    public Histogram getTimerangeHistogram() {
        final Collection<QueryExecutionStats> allStats = executionStatsCollector.getAllStats();
        return timerangeHistogramCreation.create(allStats);
    }

}


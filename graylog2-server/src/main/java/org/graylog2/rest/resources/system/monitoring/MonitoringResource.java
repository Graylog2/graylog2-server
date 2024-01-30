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
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.graylog.plugins.views.search.engine.QueryExecutionStats;
import org.graylog.plugins.views.search.engine.monitoring.collection.StatsCollector;
import org.graylog.plugins.views.search.engine.monitoring.data.histogram.Histogram;
import org.graylog.plugins.views.search.engine.monitoring.data.histogram.creation.AverageValueComputation;
import org.graylog.plugins.views.search.engine.monitoring.data.histogram.creation.MaxValueComputation;
import org.graylog.plugins.views.search.engine.monitoring.data.histogram.creation.MultiValueSingleInputHistogramCreation;
import org.graylog.plugins.views.search.engine.monitoring.data.histogram.creation.PercentageValueComputation;
import org.graylog.plugins.views.search.engine.monitoring.data.histogram.creation.PeriodBasedBinChooser;
import org.graylog.plugins.views.search.engine.monitoring.data.histogram.creation.ValueComputation;
import org.graylog2.indexer.searches.SearchesClusterConfig;
import org.graylog2.rest.MoreMediaTypes;
import org.graylog2.shared.rest.resources.RestResource;
import org.graylog2.shared.security.RestPermissions;
import org.joda.time.Period;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.graylog2.shared.rest.documentation.generator.Generator.CLOUD_VISIBLE;

@RequiresAuthentication
@Api(value = "System/Monitoring", tags = {CLOUD_VISIBLE})
@Path("/system/monitoring")
public class MonitoringResource extends RestResource {

    public static final String AVG_FUNCTION_NAME = "Avg. duration (ms)";
    public static final String MAX_FUNCTION_NAME = "Max. duration (ms)";
    public static final String PERCENT_FUNCTION_NAME = "Percent. of recent queries";
    public static final String TIMERANGE = "Timerange";
    private final StatsCollector<QueryExecutionStats> executionStatsCollector;
    private final MultiValueSingleInputHistogramCreation<Period, QueryExecutionStats> histogramCreator;


    @Inject
    public MonitoringResource(final StatsCollector<QueryExecutionStats> executionStatsCollector) {
        this.executionStatsCollector = executionStatsCollector;
        Map<String, ValueComputation<QueryExecutionStats, Long>> valueFunctions = new LinkedHashMap<>();
        valueFunctions.put(MonitoringResource.AVG_FUNCTION_NAME, new AverageValueComputation<>(QueryExecutionStats::duration));
        valueFunctions.put(MonitoringResource.MAX_FUNCTION_NAME, new MaxValueComputation<>(QueryExecutionStats::duration));
        valueFunctions.put(MonitoringResource.PERCENT_FUNCTION_NAME, new PercentageValueComputation<>());
        this.histogramCreator = new MultiValueSingleInputHistogramCreation<>(
                new ArrayList<>(SearchesClusterConfig.createDefault().relativeTimerangeOptions().keySet()),
                new PeriodBasedBinChooser(),
                valueFunctions,
                TIMERANGE
        );
    }

    @GET
    @Timed
    @ApiOperation(value = "Get timerange-based histogram of queries durations and percentage in recent query population")
    @Path("query_duration_histogram")
    @Produces({MediaType.APPLICATION_JSON, MoreMediaTypes.TEXT_CSV})
    @RequiresPermissions({RestPermissions.MONITORING_READ})
    public Histogram getQueryDurationHistogram() {
        final Collection<QueryExecutionStats> allStats = executionStatsCollector.getAllStats();
        return histogramCreator.create(allStats);
    }

}


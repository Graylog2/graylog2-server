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
import org.graylog.plugins.views.search.engine.monitoring.data.histogram.Bin;
import org.graylog.plugins.views.search.engine.monitoring.data.histogram.Histogram;
import org.graylog.plugins.views.search.engine.monitoring.data.histogram.MultiValueBin;
import org.graylog.plugins.views.search.engine.monitoring.data.histogram.NamedBinDefinition;
import org.graylog2.indexer.searches.SearchesClusterConfig;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;
import org.graylog2.rest.MoreMediaTypes;
import org.graylog2.shared.rest.resources.RestResource;
import org.joda.time.Duration;
import org.joda.time.Period;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static org.graylog2.shared.rest.documentation.generator.Generator.CLOUD_VISIBLE;

@RequiresAuthentication
@Api(value = "System/Monitoring", tags = {CLOUD_VISIBLE})
@Path("/system/monitoring")
public class MonitoringResource extends RestResource {

    private final QueryExecutionStatsCollector<QueryExecutionStats> executionStatsCollector;

    @Inject
    public MonitoringResource(final QueryExecutionStatsCollector<QueryExecutionStats> executionStatsCollector) {
        this.executionStatsCollector = executionStatsCollector;
    }


    //TODO: for verification and debugging, will probably be removed in final version
    @GET
    @Timed
    @ApiOperation(value = "Get internal Graylog system messages")
    @Produces(MediaType.TEXT_PLAIN)
    @Path("data_points")
    public String getDataPoints() {
        List<Period> periods = new ArrayList<>(SearchesClusterConfig.createDefault().relativeTimerangeOptions().keySet());
        final Collection<QueryExecutionStats> allStats = executionStatsCollector.getAllStats();
        StringBuilder sb = new StringBuilder();
        for (QueryExecutionStats stat : allStats) {
            final Optional<Period> properPeriod = periods.stream()
                    .filter(per -> matches(per, stat.effectiveTimeRange()))
                    .findFirst();
            sb.append(properPeriod.map(Objects::toString).orElse("-"))
                    .append(" ").append(stat.duration()).append("\n");
        }
        return sb.toString();
    }

    @GET
    @Timed
    @ApiOperation(value = "Get internal Graylog system messages")
    @Path("timerange_histogram")
    @Produces({MediaType.APPLICATION_JSON, MoreMediaTypes.TEXT_CSV})
    public Histogram<NamedBinDefinition> getTimerangeHistogram() {
        List<Period> periods = new ArrayList<>(SearchesClusterConfig.createDefault().relativeTimerangeOptions().keySet());
        periods.sort(Comparator.comparing(Period::toStandardDuration));
        final Collection<QueryExecutionStats> allStats = executionStatsCollector.getAllStats();

        Map<Period, Collection<Long>> histogramPreparation = new LinkedHashMap<>();
        periods.forEach(p -> histogramPreparation.put(p, new LinkedList<>()));
        allStats.forEach(queryExecutionStats -> {
                    periods.stream()
                            .filter(per -> matches(per, queryExecutionStats.effectiveTimeRange()))
                            .findFirst()
                            .ifPresent(per -> histogramPreparation.get(per).add(queryExecutionStats.duration()));

                }
        );

        int totalStats = allStats.size();


        final List<? extends Bin<NamedBinDefinition>> bins = histogramPreparation.entrySet().stream()
                .map(
                        entry -> {
                            final NamedBinDefinition binDefinition = new NamedBinDefinition(entry.getKey().toString());
                            final long max = entry.getValue().stream().mapToLong(x -> x).max().orElse(0);
                            final long average = (long) entry.getValue().stream().mapToLong(x -> x).average().orElse(0);
                            return new MultiValueBin<>(binDefinition, List.of(average, max, totalStats > 0 ? (float) entry.getValue().size() / totalStats : 0));
                        }
                )
                .toList();
        return new Histogram<>(bins);
    }

    private boolean matches(Period binRange, TimeRange statsRange) {
        return binRange.toStandardDuration().compareTo(new Duration(statsRange.getFrom().toInstant(), statsRange.getTo().toInstant())) >= 0;
    }


}

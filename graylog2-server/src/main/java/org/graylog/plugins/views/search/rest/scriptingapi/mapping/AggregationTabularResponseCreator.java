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
package org.graylog.plugins.views.search.rest.scriptingapi.mapping;

import com.google.common.collect.ImmutableList;
import org.graylog.plugins.views.search.QueryResult;
import org.graylog.plugins.views.search.SearchJob;
import org.graylog.plugins.views.search.SearchType;
import org.graylog.plugins.views.search.rest.SearchJobDTO;
import org.graylog.plugins.views.search.rest.scriptingapi.request.AggregationRequestSpec;
import org.graylog.plugins.views.search.rest.scriptingapi.request.Metric;
import org.graylog.plugins.views.search.rest.scriptingapi.response.Metadata;
import org.graylog.plugins.views.search.rest.scriptingapi.response.TabularResponse;
import org.graylog.plugins.views.search.searchtypes.pivot.PivotResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AggregationTabularResponseCreator implements TabularResponseCreator {

    private static final Logger LOG = LoggerFactory.getLogger(AggregationTabularResponseCreator.class);

    public TabularResponse mapToResponse(final AggregationRequestSpec searchRequestSpec, final SearchJob searchJob) throws AggregationFailedException {
        final SearchJobDTO searchJobDTO = SearchJobDTO.fromSearchJob(searchJob);
        final QueryResult queryResult = searchJobDTO.results().get(SearchRequestSpecToSearchMapper.QUERY_ID);

        if (queryResult != null) {
            throwErrorIfAnyAvailable(queryResult);
            final SearchType.Result aggregationResult = queryResult.searchTypes().get(AggregationSpecToPivotMapper.PIVOT_ID);
            if (aggregationResult instanceof PivotResult pivotResult) {
                return mapToResponse(searchRequestSpec, pivotResult);
            }
        }

        LOG.warn("Scripting API failed to obtain aggregation for input : " + searchRequestSpec);
        throw new AggregationFailedException("Scripting API failed to obtain aggregation for input : " + searchRequestSpec);
    }

    private TabularResponse mapToResponse(final AggregationRequestSpec searchRequestSpec,
                                          final PivotResult pivotResult) {
        return new TabularResponse(
                searchRequestSpec.getSchema(),
                getDatarows(searchRequestSpec, pivotResult),
                new Metadata(pivotResult.effectiveTimerange())
        );
    }

    private static List<List<Object>> getDatarows(final AggregationRequestSpec searchRequestSpec,
                                                  final PivotResult pivotResult) {
        final int numGroupings = searchRequestSpec.groupings().size();

        return pivotResult.rows()
                .stream()
                .map(pivRow -> {
                    final Stream<String> groupings = Stream.concat(
                            pivRow.key().stream(),
                            Collections.nCopies(numGroupings - pivRow.key().size(), "-").stream()  //sometimes pivotRow does not have enough keys - empty value!
                    );

                    final Stream<Object> metrics = searchRequestSpec.metrics().stream()
                            .map(m -> metricValue(pivRow.values(), m));

                    return Stream.concat(groupings, metrics).collect(Collectors.toList());
                })
                .collect(Collectors.toList());
    }

    private static Object metricValue(ImmutableList<PivotResult.Value> values, Metric metric) {
        return values.stream()
                .filter(value -> value.key().contains(metric.columnName()))
                .findFirst()
                .map(PivotResult.Value::value)
                .orElse("-");
    }
}

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
import org.graylog.plugins.views.search.permissions.SearchUser;
import org.graylog.plugins.views.search.rest.SearchJobDTO;
import org.graylog.plugins.views.search.rest.scriptingapi.request.AggregationRequestSpec;
import org.graylog.plugins.views.search.rest.scriptingapi.request.RequestedField;
import org.graylog.plugins.views.search.rest.scriptingapi.response.Metadata;
import org.graylog.plugins.views.search.rest.scriptingapi.response.TabularResponse;
import org.graylog.plugins.views.search.rest.scriptingapi.response.decorators.CachingDecorator;
import org.graylog.plugins.views.search.rest.scriptingapi.response.decorators.FieldDecorator;
import org.graylog.plugins.views.search.searchtypes.pivot.Pivot;
import org.graylog.plugins.views.search.searchtypes.pivot.PivotResult;
import org.graylog.plugins.views.search.searchtypes.pivot.SeriesSpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AggregationTabularResponseCreator implements TabularResponseCreator {

    private static final Logger LOG = LoggerFactory.getLogger(AggregationTabularResponseCreator.class);
    private final Set<FieldDecorator> decorators;

    @Inject
    public AggregationTabularResponseCreator(Set<FieldDecorator> decorators) {
        this.decorators = decorators;
    }

    public TabularResponse mapToResponse(final AggregationRequestSpec searchRequestSpec, final SearchJob searchJob, SearchUser searchUser) throws QueryFailedException {
        final SearchJobDTO searchJobDTO = SearchJobDTO.fromSearchJob(searchJob);


        final QueryResult queryResult = searchJobDTO.results().get(SearchRequestSpecToSearchMapper.QUERY_ID);

        if (queryResult != null) {
            throwErrorIfAnyAvailable(queryResult);
            final SearchType.Result aggregationResult = queryResult.searchTypes().get(AggregationSpecToPivotMapper.PIVOT_ID);

            if (aggregationResult instanceof PivotResult pivotResult) {
                final List<SeriesSpec> seriesSpecs = extractSeriesSpec(queryResult);
                return mapToResponse(searchRequestSpec, pivotResult, seriesSpecs, searchUser);
            }
        }

        LOG.warn("Scripting API failed to obtain aggregation for input : " + searchRequestSpec);
        throw new QueryFailedException("Scripting API failed to obtain aggregation for input : " + searchRequestSpec);
    }

    private List<SeriesSpec> extractSeriesSpec(QueryResult queryResult) {
        return queryResult.query().searchTypes().stream().filter(t -> AggregationSpecToPivotMapper.PIVOT_ID.equals(t.id())).findFirst().stream()
                .filter(searchType -> searchType instanceof Pivot)
                .map(pivot -> (Pivot) pivot)
                .flatMap(pivot -> pivot.series().stream())
                .collect(Collectors.toList());
    }

    private TabularResponse mapToResponse(final AggregationRequestSpec searchRequestSpec,
                                          final PivotResult pivotResult, List<SeriesSpec> seriesSpec, SearchUser searchUser) {
        return new TabularResponse(
                searchRequestSpec.getSchema(),
                getDatarows(searchRequestSpec, pivotResult, seriesSpec, searchUser),
                new Metadata(pivotResult.effectiveTimerange())
        );
    }

    private List<List<Object>> getDatarows(final AggregationRequestSpec searchRequestSpec,
                                           final PivotResult pivotResult, List<SeriesSpec> seriesSpecs, SearchUser searchUser) {
        final int numGroupings = searchRequestSpec.groupings().size();

        final Set<FieldDecorator> cachedDecorators = this.decorators.stream().map(CachingDecorator::new).collect(Collectors.toSet());

        return pivotResult.rows()
                .stream()
                .map(pivRow -> {
                    final Stream<Object> groupings = Stream.concat(
                            decorateGroupings(pivRow.key(), searchRequestSpec, cachedDecorators, searchUser),
                            Collections.nCopies(numGroupings - pivRow.key().size(), "-").stream()  //sometimes pivotRow does not have enough keys - empty value!
                    );

                    final ImmutableList<PivotResult.Value> values = pivRow.values();
                    final Stream<Object> metrics = seriesSpecs.stream().map(s -> metricValue(s, values));

                    return Stream.concat(groupings, metrics).collect(Collectors.toList());
                })
                .collect(Collectors.toList());
    }

    private Stream<Object> decorateGroupings(ImmutableList<String> keys, AggregationRequestSpec searchRequestSpec, Set<FieldDecorator> decorators, SearchUser searchUser) {
        List<Object> result = new ArrayList<>();
        for (int i = 0; i < keys.size(); i++) {
            final String value = keys.get(i);
            final RequestedField field = searchRequestSpec.groupings().get(i).requestedField();
            final Object decorated = decorate(decorators, field, value, searchUser);
            result.add(decorated);
        }
        return result.stream();
    }

    private static Object metricValue(SeriesSpec seriesSpec, ImmutableList<PivotResult.Value> values) {
        return values.stream()
                .filter(value -> isMetricValue(seriesSpec, value))
                .findFirst()
                .map(PivotResult.Value::value)
                .orElse("-");
    }

    private static boolean isMetricValue(SeriesSpec metric, PivotResult.Value value) {
        return value.key().contains(metric.literal());
    }
}

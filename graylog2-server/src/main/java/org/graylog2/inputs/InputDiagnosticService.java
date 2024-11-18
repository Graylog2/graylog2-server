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
package org.graylog2.inputs;

import com.google.common.collect.ImmutableSet;
import jakarta.inject.Inject;
import jakarta.ws.rs.InternalServerErrorException;
import org.apache.commons.lang3.StringUtils;
import org.graylog.plugins.views.search.Query;
import org.graylog.plugins.views.search.QueryResult;
import org.graylog.plugins.views.search.Search;
import org.graylog.plugins.views.search.SearchJob;
import org.graylog.plugins.views.search.SearchType;
import org.graylog.plugins.views.search.elasticsearch.ElasticsearchQueryString;
import org.graylog.plugins.views.search.engine.SearchExecutor;
import org.graylog.plugins.views.search.errors.SearchError;
import org.graylog.plugins.views.search.permissions.SearchUser;
import org.graylog.plugins.views.search.rest.ExecutionState;
import org.graylog.plugins.views.search.rest.SearchJobDTO;
import org.graylog.plugins.views.search.searchtypes.pivot.Pivot;
import org.graylog.plugins.views.search.searchtypes.pivot.PivotResult;
import org.graylog.plugins.views.search.searchtypes.pivot.buckets.Values;
import org.graylog.plugins.views.search.searchtypes.pivot.series.Count;
import org.graylog2.database.NotFoundException;
import org.graylog2.plugin.indexer.searches.timeranges.RelativeRange;
import org.graylog2.rest.models.system.inputs.responses.InputDiagnostics;
import org.graylog2.streams.StreamService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.AbstractMap;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.graylog2.plugin.Message.FIELD_GL2_SOURCE_INPUT;
import static org.graylog2.rest.models.system.inputs.responses.InputDiagnostics.EMPTY_DIAGNOSTICS;
import static org.graylog2.shared.utilities.StringUtils.f;

public class InputDiagnosticService {
    private static final Logger LOG = LoggerFactory.getLogger(InputDiagnosticService.class);

    private static final String QUERY_ID = "input_diagnostics_streams_query";
    private static final String PIVOT_ID = "input_diagnostics_streams_pivot";

    private final SearchExecutor searchExecutor;
    private final StreamService streamService;

    @Inject
    public InputDiagnosticService(SearchExecutor searchExecutor,
                                  StreamService streamService) {
        this.searchExecutor = searchExecutor;
        this.streamService = streamService;
    }

    public InputDiagnostics getInputDiagnostics(
            Input input, SearchUser searchUser) {
        final Search search = buildSearch(input);
        final SearchJob searchJob = searchExecutor.executeSync(search, searchUser, ExecutionState.empty());
        final SearchJobDTO searchJobDTO = SearchJobDTO.fromSearchJob(searchJob);
        final QueryResult queryResult = searchJobDTO.results().get(QUERY_ID);

        final Set<SearchError> errors = queryResult.errors();
        if (errors != null && !errors.isEmpty()) {
            String errorMsg = f("An error occurred while executing aggregation: %s",
                    errors.stream().map(SearchError::description).collect(Collectors.joining(", ")));
            LOG.error(errorMsg);
            throw new InternalServerErrorException(errorMsg);
        }

        final SearchType.Result aggregationResult = queryResult.searchTypes().get(PIVOT_ID);
        if (aggregationResult instanceof PivotResult pivotResult && pivotResult.total() > 0) {
            final List<AbstractMap.SimpleEntry<String, Long>> resultList = pivotResult.rows().stream()
                    .filter(row -> row.source().equals("leaf"))
                    .map(InputDiagnosticService::extractValues)
                    .toList();
            Map<String, Long> resultMap = new HashMap<>();
            resultList.forEach(
                    entry -> {
                        try {
                            final org.graylog2.plugin.streams.Stream stream = streamService.load(entry.getKey());
                            resultMap.put(stream.getTitle(), entry.getValue());
                        } catch (NotFoundException e) {
                            LOG.warn("Unable to load stream {}", entry.getKey(), e);
                        }
                    }
            );
            return new InputDiagnostics(resultMap);
        }

        return EMPTY_DIAGNOSTICS;
    }

    private Search buildSearch(Input input) {
        final SearchType searchType = Pivot.builder()
                .id(PIVOT_ID)
                .rollup(true)
                .rowGroups(Values.builder().fields(List.of("streams")).build())
                .series(Count.builder().build())
                .build();
        return Search.builder()
                .queries(ImmutableSet.of(
                        Query.builder()
                                .id(QUERY_ID)
                                .query(ElasticsearchQueryString.of(FIELD_GL2_SOURCE_INPUT + ":" + input.getId()))
                                .searchTypes(Collections.singleton(searchType))
                                .timerange(RelativeRange.create(900))
                                .build()
                ))
                .build();
    }

    private static AbstractMap.SimpleEntry<String, Long> extractValues(PivotResult.Row r) {
        if (r.values().size() != 1) {
            String errorMsg = f("Expected 1 value in aggregation result, but received [%d].", r.values().size());
            LOG.warn(errorMsg);
            throw new InternalServerErrorException(errorMsg);
        }
        final String streamId = r.key().get(0);
        if (StringUtils.isEmpty(streamId)) {
            String errorMsg = "Unable to retrieve stream ID from query result";
            LOG.warn(errorMsg);
            throw new InternalServerErrorException(errorMsg);
        }

        final Long count = (Long) r.values().get(0).value();
        return new AbstractMap.SimpleEntry<>(streamId, count);
    }
}

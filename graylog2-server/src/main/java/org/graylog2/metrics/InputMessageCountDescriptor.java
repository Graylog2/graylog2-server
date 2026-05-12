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
package org.graylog2.metrics;

import com.google.common.collect.ImmutableSet;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.InternalServerErrorException;
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
import org.graylog2.metrics.cache.EntityCachedMetricsDescriptor;
import org.graylog2.metrics.cache.MetricsCacheConfiguration;
import org.graylog2.plugin.indexer.searches.timeranges.RelativeRange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.graylog2.plugin.Message.FIELD_GL2_SOURCE_INPUT;
import static org.graylog2.shared.utilities.StringUtils.f;

/**
 * Provides {@code message_count} metrics for regular inputs.
 * Computes a 24h message count via OpenSearch aggregation on {@code gl2_source_input}.
 */
public class InputMessageCountDescriptor implements EntityCachedMetricsDescriptor {
    private static final Logger LOG = LoggerFactory.getLogger(InputMessageCountDescriptor.class);

    public static final String FIELD_NAME = "message_count";

    private static final int MESSAGE_COUNT_RANGE_SECONDS = 86400; // 24h
    private static final String QUERY_ID = "input_metrics_query";
    private static final String PIVOT_ID = "input_metrics_pivot";

    private final SearchExecutor searchExecutor;
    private final Duration cacheTtl;

    @Inject
    public InputMessageCountDescriptor(SearchExecutor searchExecutor,
                                       @Named(MetricsCacheConfiguration.METRICS_CACHE_TTL_LONG) Duration cacheTtl) {
        this.searchExecutor = searchExecutor;
        this.cacheTtl = cacheTtl;
    }

    @Override
    public String entityType() {
        return MetricsModule.ENTITY_TYPE_INPUTS;
    }

    @Override
    public String fieldName() {
        return FIELD_NAME;
    }

    @Override
    public Duration cacheTtl() {
        return cacheTtl;
    }

    @Override
    public Map<String, Object> computeField(Collection<String> entityIds, SearchUser searchUser) {
        final String queryString = entityIds.stream()
                .map(id -> FIELD_GL2_SOURCE_INPUT + ":" + id)
                .collect(Collectors.joining(" OR "));

        final SearchType searchType = Pivot.builder()
                .id(PIVOT_ID)
                .rollup(true)
                .rowGroups(Values.builder().fields(List.of(FIELD_GL2_SOURCE_INPUT)).build())
                .series(Count.builder().build())
                .build();

        final Search search = Search.builder()
                .queries(ImmutableSet.of(
                        Query.builder()
                                .id(QUERY_ID)
                                .query(ElasticsearchQueryString.of(queryString))
                                .searchTypes(Collections.singleton(searchType))
                                .timerange(RelativeRange.create(MESSAGE_COUNT_RANGE_SECONDS))
                                .build()
                ))
                .build();

        final SearchJob searchJob = searchExecutor.executeSync(search, searchUser, ExecutionState.empty());
        final SearchJobDTO searchJobDTO = SearchJobDTO.fromSearchJob(searchJob);
        final QueryResult queryResult = searchJobDTO.results().get(QUERY_ID);

        final Set<SearchError> errors = queryResult.errors();
        if (errors != null && !errors.isEmpty()) {
            final String errorMsg = f("Error executing input metrics aggregation: %s",
                    errors.stream().map(SearchError::description).collect(Collectors.joining(", ")));
            LOG.error(errorMsg);
            throw new InternalServerErrorException(errorMsg);
        }

        final Map<String, Object> counts = new HashMap<>();
        final SearchType.Result aggregationResult = queryResult.searchTypes().get(PIVOT_ID);
        if (aggregationResult instanceof PivotResult pivotResult) {
            for (final PivotResult.Row row : pivotResult.rows()) {
                if (!"leaf".equals(row.source()) || row.key().isEmpty() || row.values().isEmpty()) {
                    continue;
                }
                final String inputId = row.key().getFirst();
                final Long count = (Long) row.values().getFirst().value();
                counts.put(inputId, count);
            }
        }

        // Fill in zero for entities with no messages
        for (final String entityId : entityIds) {
            counts.putIfAbsent(entityId, 0L);
        }

        return counts;
    }
}

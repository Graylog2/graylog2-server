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
package org.graylog.plugins.views.search.engine;

import com.google.common.base.Stopwatch;
import org.graylog.plugins.views.search.GlobalOverride;
import org.graylog.plugins.views.search.Query;
import org.graylog.plugins.views.search.QueryResult;
import org.graylog.plugins.views.search.SearchJob;
import org.graylog.plugins.views.search.errors.QueryError;
import org.graylog.plugins.views.search.errors.SearchError;
import org.graylog.plugins.views.search.errors.SearchTypeError;
import org.graylog.plugins.views.search.searchtypes.pivot.PivotResult;
import org.graylog2.plugin.indexer.searches.timeranges.AbsoluteRange;
import org.graylog2.plugin.indexer.searches.timeranges.RelativeRange;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;
import org.joda.time.DateTime;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * A search backend that is capable of generating and executing search jobs
 *
 * @param <T> the type of the generated query
 */
public interface QueryBackend<T extends GeneratedQueryContext> {

    /**
     * Generate a backend-specific query out of the logical query structure.
     *
     * @param query the graylog query structure
     * @return a backend specific generated query
     */
    T generate(Query query, Set<SearchError> validationErrors);

    default boolean isAllMessages(TimeRange timeRange) {
        return timeRange instanceof RelativeRange && ((RelativeRange)timeRange).isAllMessages();
    }

    default AbsoluteRange effectiveTimeRangeForResult(Query query, QueryResult queryResult) {
        final TimeRange effectiveTimeRange = query.globalOverride().flatMap(GlobalOverride::timerange).orElse(query.timerange());

        if (isAllMessages(effectiveTimeRange)) {
            final Optional<AbsoluteRange> effectiveRange = queryResult.searchTypes().values().stream()
                    .filter(result -> result instanceof PivotResult)
                    .map(result -> ((PivotResult) result).effectiveTimerange())
                    .reduce((prev, next) -> {
                        final DateTime from = prev.from().compareTo(next.from()) < 0 ? prev.from() : next.from();
                        final DateTime to = prev.to().compareTo(next.to()) < 0 ? next.to() : prev.to();
                        return AbsoluteRange.create(from, to);
                    });

            if (effectiveRange.isPresent()) {
                return effectiveRange.get();
            }
        }
        return AbsoluteRange.create(effectiveTimeRange.getFrom(), effectiveTimeRange.getTo());
    }

    // TODO we can probably push job and query into the GeneratedQueryContext to simplify the signature
    default QueryResult run(SearchJob job, Query query, GeneratedQueryContext generatedQueryContext) {
        try {
            final Stopwatch stopwatch = Stopwatch.createStarted();
            final QueryExecutionStats.Builder statsBuilder = QueryExecutionStats.builderWithCurrentTime();
            // https://www.ibm.com/developerworks/java/library/j-jtp04298/index.html#3.0
            //noinspection unchecked
            final QueryResult result = doRun(job, query, (T) generatedQueryContext);
            stopwatch.stop();
            return result.toBuilder()
                    .executionStats(
                            statsBuilder.duration(stopwatch.elapsed(TimeUnit.MILLISECONDS))
                                    .effectiveTimeRange(effectiveTimeRangeForResult(query, result))
                                    .build())
                    .build();
        } catch (Exception e) {
            // the backend has very likely created a more specific error and added it to the context, but we fall
            // back to a generic error so we never throw exceptions into the engine.
            final QueryError queryError = new QueryError(query, e);
            generatedQueryContext.addError(queryError);
            return QueryResult.failedQueryWithError(query, queryError);
        }
    }

    /**
     * Run the generated query as part of the given query job.
     * <p>
     * This method is typically being run in an executor and can safely block.
     *
     * @param job          currently executing job
     * @param query        the individual query to run from the current job
     * @param queryContext the generated query by {@link #generate(Query, Set) <SearchError>)}
     * @return the result for the query
     * @throws RuntimeException if the query could not be executed for some reason
     */
    QueryResult doRun(SearchJob job, Query query, T queryContext);

    Set<String> getFieldsPresentInQueryResultDocuments(final Query normalizedQuery, final int size);

    default boolean isSearchTypeWithError(T queryContext, String searchTypeId) {
        return queryContext.errors().stream()
                .filter(q -> q instanceof SearchTypeError)
                .map(q -> (SearchTypeError) q)
                .map(SearchTypeError::searchTypeId)
                .anyMatch(id -> Objects.equals(id, searchTypeId));
    }
}

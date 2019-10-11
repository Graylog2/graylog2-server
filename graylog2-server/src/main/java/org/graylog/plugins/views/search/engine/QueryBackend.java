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
package org.graylog.plugins.views.search.engine;

import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableSet;
import org.graylog.plugins.views.search.Parameter;
import org.graylog.plugins.views.search.Query;
import org.graylog.plugins.views.search.QueryMetadata;
import org.graylog.plugins.views.search.QueryResult;
import org.graylog.plugins.views.search.SearchJob;
import org.graylog.plugins.views.search.errors.QueryError;
import org.graylog.plugins.views.search.searchtypes.pivot.PivotResult;
import org.graylog2.plugin.indexer.searches.timeranges.AbsoluteRange;
import org.graylog2.plugin.indexer.searches.timeranges.RelativeRange;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;
import org.joda.time.DateTime;

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
     * @param job                currently executing job
     * @param query              the graylog query structure
     * @param predecessorResults the query result of the preceding queries
     * @return a backend specific generated query
     */
    T generate(SearchJob job, Query query, Set<QueryResult> predecessorResults);

    default boolean isAllMessages(TimeRange timeRange) {
        return timeRange instanceof RelativeRange && ((RelativeRange)timeRange).range() == 0;
    }

    default AbsoluteRange effectiveTimeRangeForResult(Query query, QueryResult queryResult) {
        if (isAllMessages(query.timerange())) {
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
        return AbsoluteRange.create(query.timerange().getFrom(), query.timerange().getTo());
    }

    // TODO we can probably push job, query and predecessorResults into the GeneratedQueryContext to simplify the signature
    default QueryResult run(SearchJob job, Query query, GeneratedQueryContext generatedQueryContext, Set<QueryResult> predecessorResults) {
        try {
            final Stopwatch stopwatch = Stopwatch.createStarted();
            final QueryExecutionStats.Builder statsBuilder = QueryExecutionStats.builderWithCurrentTime();
            // https://www.ibm.com/developerworks/java/library/j-jtp04298/index.html#3.0
            //noinspection unchecked
            final QueryResult result = doRun(job, query, (T) generatedQueryContext, predecessorResults);
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
     * @param job                currently executing job
     * @param query              the individual query to run from the current job
     * @param queryContext       the generated query by {@link #generate(SearchJob, Query, Set)}
     * @param predecessorResults the query result of the preceding queries
     * @return the result for the query
     * @throws RuntimeException if the query could not be executed for some reason
     */
    QueryResult doRun(SearchJob job, Query query, T queryContext, Set<QueryResult> predecessorResults);

    /**
     * Parse the query and return structural information about it.
     * <p>
     * This method decomposes the backend-specific query and returns information about used parameters, optionally the
     * AST for syntax highlight and other information the UI can use to offer help.
     */
    QueryMetadata parse(ImmutableSet<Parameter> parameters, Query query);
}

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
package org.graylog.events.processor.aggregation;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.Uninterruptibles;
import com.google.inject.assistedinject.Assisted;
import org.graylog.plugins.views.search.Filter;
import org.graylog.plugins.views.search.Query;
import org.graylog.plugins.views.search.QueryResult;
import org.graylog.plugins.views.search.Search;
import org.graylog.plugins.views.search.SearchJob;
import org.graylog.plugins.views.search.SearchType;
import org.graylog.plugins.views.search.db.SearchJobService;
import org.graylog.plugins.views.search.elasticsearch.ElasticsearchQueryString;
import org.graylog.plugins.views.search.engine.QueryEngine;
import org.graylog.plugins.views.search.errors.QueryError;
import org.graylog.plugins.views.search.errors.SearchError;
import org.graylog.plugins.views.search.filter.OrFilter;
import org.graylog.plugins.views.search.filter.StreamFilter;
import org.graylog.plugins.views.search.searchtypes.pivot.BucketSpec;
import org.graylog.plugins.views.search.searchtypes.pivot.Pivot;
import org.graylog.plugins.views.search.searchtypes.pivot.PivotResult;
import org.graylog.plugins.views.search.searchtypes.pivot.SeriesSpec;
import org.graylog.plugins.views.search.searchtypes.pivot.buckets.Values;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.InternalServerErrorException;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import static com.google.common.base.MoreObjects.firstNonNull;
import static java.util.stream.Collectors.toSet;

public class PivotAggregationSearch implements AggregationSearch {
    private static final Logger LOG = LoggerFactory.getLogger(PivotAggregationSearch.class);

    private static final String QUERY_ID = "query-1";
    private static final String PIVOT_ID = "pivot-1";

    private final AggregationEventProcessorConfig config;
    private final AggregationEventProcessorParameters parameters;
    private final String searchOwner;
    private final SearchJobService searchJobService;
    private final QueryEngine queryEngine;
    private final long timeout = 60000; // TODO: Make search timeout configurable

    @Inject
    public PivotAggregationSearch(@Assisted AggregationEventProcessorConfig config,
                                  @Assisted AggregationEventProcessorParameters parameters,
                                  @Assisted String searchOwner,
                                  SearchJobService searchJobService,
                                  QueryEngine queryEngine) {
        this.config = config;
        this.parameters = parameters;
        this.searchOwner = searchOwner;
        this.searchJobService = searchJobService;
        this.queryEngine = queryEngine;
    }

    private String metricName(AggregationSeries series) {
        return String.format(Locale.ROOT, "metric/%s/%s/%s",
                series.function().toString().toLowerCase(Locale.ROOT), series.field().orElse("<no-field>"), series.id());
    }

    @Override
    public AggregationResult doSearch() {
        final SearchJob searchJob = getSearchJob(parameters, searchOwner);
        final QueryResult queryResult = searchJob.results().get(QUERY_ID);

        final Set<SearchError> errors = firstNonNull(queryResult.errors(), Collections.emptySet());

        if (!errors.isEmpty()) {
            LOG.error("Aggregation search resulted in {} errors", errors.size());
            errors.forEach(error -> {
                if (error instanceof QueryError) {
                    final QueryError queryError = (QueryError) error;
                    LOG.error("Aggregation search query <{}> returned an error: {}\n{}",
                            queryError.queryId(), queryError.description(), queryError.backtrace());
                } else {
                    LOG.error("Aggregation search returned an error: {}", error);
                }
            });
            // TODO: Throw a meaningful exception (or return a result object that contains the error) here to make
            //       sure it doesn't look like it's all working
            return null;
        }

        final PivotResult pivotResult = (PivotResult) queryResult.searchTypes().get(PIVOT_ID);

        return AggregationResult.builder()
                .keyResults(extractValues(pivotResult))
                .effectiveTimerange(pivotResult.effectiveTimerange())
                .totalAggregatedMessages(pivotResult.total())
                .build();
    }

    @VisibleForTesting
    ImmutableList<AggregationKeyResult> extractValues(PivotResult pivotResult) {
        final ImmutableList.Builder<AggregationKeyResult> results = ImmutableList.builder();

        // Example PivotResult structures. The "key" value is composed of: "metric/<function>/<field>/<series-id>"
        //
        // With group-by:
        // {
        //  "rows": [
        //    {
        //      "key": ["php", "box2"],
        //      "values": [
        //        {
        //          "key": ["metric/count/source/abc123"],
        //          "value": 86,
        //          "rollup": true,
        //          "source": "row-leaf"
        //        },
        //        {
        //          "key": ["metric/card/source/abc123"],
        //          "value": 1,
        //          "rollup": true,
        //          "source": "row-leaf"
        //        }
        //      ],
        //      "source": "leaf"
        //    },
        //    {
        //      "key": ["php"],
        //      "values": [
        //        {
        //          "key": ["metric/count/source/abc123"],
        //          "value": 86,
        //          "rollup": true,
        //          "source": "row-inner"
        //        },
        //        {
        //          "key": ["metric/card/source/abc123"],
        //          "value": 1,
        //          "rollup": true,
        //          "source": "row-inner"
        //        }
        //      ],
        //      "source": "non-leaf"
        //    },
        //    {
        //      "key": ["sshd","box2"],
        //      "values": [
        //        {
        //          "key": ["metric/count/source/abc123"],
        //          "value": 5,
        //          "rollup": true,
        //          "source": "row-leaf"
        //        },
        //        {
        //          "key": ["metric/card/source/abc123"],
        //          "value": 1,
        //          "rollup": true,
        //          "source": "row-leaf"
        //        }
        //      ],
        //      "source": "leaf"
        //    }
        //  ]
        //}
        //
        // Without group-by:
        // {
        //  "rows": [
        //    {
        //      "key": [],
        //      "values": [
        //        {
        //          "key": ["metric/count/source/abc123"],
        //          "value": 18341,
        //          "rollup": true,
        //          "source": "row-leaf"
        //        },
        //        {
        //          "key": ["metric/card/source/abc123"],
        //          "value": 1,
        //          "rollup": true,
        //          "source": "row-leaf"
        //        }
        //      ],
        //      "source": "leaf"
        //    }
        //  ]
        //}
        for (final PivotResult.Row row : pivotResult.rows()) {
            if (!"leaf".equals(row.source())) {
                // "non-leaf" values can show up when the "rollup" feature is enabled in the pivot search type
                continue;
            }

            final ImmutableList.Builder<AggregationSeriesValue> values = ImmutableList.builder();

            for (final PivotResult.Value value : row.values()) {
                if (!"row-leaf".equals(value.source())) {
                    // "row-inner" values can show up when the "rollup" feature is enabled in the pivot search type
                    continue;
                }

                for (final AggregationSeries series : config.series()) {
                    if (!value.key().isEmpty() && value.key().get(0).equals(metricName(series))) {
                        // Some Elasticsearch aggregations can return a "null" value. (e.g. avg on a non-existent field)
                        // We are using NaN in that case to make sure our conditions will work.
                        final Object maybeNumberValue = firstNonNull(value.value(), Double.NaN);

                        if (maybeNumberValue instanceof Number) {
                            final double numberValue = ((Number) maybeNumberValue).doubleValue();
                            final AggregationSeriesValue seriesValue = AggregationSeriesValue.builder()
                                    .key(row.key())
                                    .value(numberValue)
                                    .series(series)
                                    .build();

                            values.add(seriesValue);
                        } else {
                            // Should not happen
                            throw new IllegalStateException("Got unexpected non-number value for " + series.toString() + " " + row.toString() + " " + value.toString());
                        }
                    }
                }
            }

            results.add(AggregationKeyResult.builder()
                    .key(row.key())
                    .seriesValues(values.build())
                    .build());
        }

        return results.build();
    }

    private SearchJob getSearchJob(AggregationEventProcessorParameters parameters, String username) {
        final Search search = Search.builder()
                .queries(ImmutableSet.of(getQuery(parameters)))
                .build();
        final SearchJob searchJob = queryEngine.execute(searchJobService.create(search, username));
        try {
            Uninterruptibles.getUninterruptibly(searchJob.getResultFuture(), timeout, TimeUnit.MILLISECONDS);
        } catch (ExecutionException e) {
            // TODO: Throw EventProcessorExecutionException instead
            throw new InternalServerErrorException("Error executing search job: " + e.getMessage());
        } catch (TimeoutException e) {
            // TODO: Throw EventProcessorExecutionException instead
            throw new InternalServerErrorException("Timeout while executing search job");
        } catch (Exception e) {
            // TODO: Throw EventProcessorExecutionException instead
            throw e;
        }

        return searchJob;
    }

    private Query getQuery(AggregationEventProcessorParameters parameters) {
        final Pivot.Builder pivotBuilder = Pivot.builder()
                .id(PIVOT_ID)
                .rollup(true);

        final ImmutableList<SeriesSpec> series = config.series().stream()
                .map(entry -> entry.function().toSeriesSpec(metricName(entry), entry.field().orElse(null)))
                .collect(ImmutableList.toImmutableList());

        if (!series.isEmpty()) {
            pivotBuilder.series(series);
        }

        if (!config.groupBy().isEmpty()) {
            final List<BucketSpec> groupBy = config.groupBy().stream()
                    .map(field -> Values.builder()
                            // The pivot search type (as of Graylog 3.1.0) is using the "terms" aggregation under
                            // the hood. The "terms" aggregation is meant to return the "top" terms and does not allow
                            // and efficient retrieval and pagination over all terms.
                            // Using Integer.MAX_VALUE as a limit can be very expensive with high cardinality grouping.
                            // The ES documentation recommends to use the "Composite" aggregation instead.
                            //
                            // See the ES documentation for more details:
                            //   https://www.elastic.co/guide/en/elasticsearch/reference/current/search-aggregations-bucket-terms-aggregation.html#search-aggregations-bucket-terms-aggregation-size
                            //   https://www.elastic.co/guide/en/elasticsearch/reference/current/search-aggregations-bucket-composite-aggregation.html
                            //
                            // The "Composite" aggregation is only available since ES version 6.1, unfortunately.
                            //
                            // TODO: Either find a way to use the composite aggregation when the ES version in use is
                            //       recent enough, and/or use a more conservative limit here and make it configurable
                            //       by the user.
                            .limit(Integer.MAX_VALUE)
                            .field(field)
                            .build())
                    .collect(Collectors.toList());
            pivotBuilder.rowGroups(groupBy);
        }

        final Set<SearchType> searchTypes = Collections.singleton(pivotBuilder.build());

        final Query.Builder queryBuilder = Query.builder()
                .id(QUERY_ID)
                .searchTypes(searchTypes)
                .query(ElasticsearchQueryString.builder().queryString(config.query()).build())
                .timerange(parameters.timerange());

        // Streams in parameters should override the ones in the config
        final Set<String> streams = parameters.streams().isEmpty() ? config.streams() : parameters.streams();

        if (!streams.isEmpty()) {
            queryBuilder.filter(filteringForStreamIds(streams));
        }

        return queryBuilder.build();
    }

    private Filter filteringForStreamIds(Set<String> streamIds) {
        final Set<Filter> streamFilters = streamIds.stream()
                .map(StreamFilter::ofId)
                .collect(toSet());
        return OrFilter.builder()
                .filters(streamFilters)
                .build();
    }
}

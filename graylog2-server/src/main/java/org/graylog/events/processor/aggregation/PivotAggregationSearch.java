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
import org.graylog.events.configuration.EventsConfigurationProvider;
import org.graylog.events.processor.EventDefinition;
import org.graylog.events.processor.EventProcessorException;
import org.graylog.events.search.MoreSearch;
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
import org.graylog.plugins.views.search.searchtypes.pivot.buckets.Time;
import org.graylog.plugins.views.search.searchtypes.pivot.buckets.TimeUnitInterval;
import org.graylog.plugins.views.search.searchtypes.pivot.buckets.Values;
import org.graylog.plugins.views.search.searchtypes.pivot.series.Count;
import org.graylog2.plugin.streams.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.ArrayList;
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
    private static final String STREAMS_QUERY_ID = "streams-query-1";
    private static final String STREAMS_PIVOT_ID = "streams-pivot-1";
    private static final String STREAMS_PIVOT_COUNT_ID = "streams-pivot-count-1";

    private final AggregationEventProcessorConfig config;
    private final AggregationEventProcessorParameters parameters;
    private final String searchOwner;
    private final SearchJobService searchJobService;
    private final QueryEngine queryEngine;
    private final EventsConfigurationProvider configurationProvider;
    private final EventDefinition eventDefinition;
    private final MoreSearch moreSearch;

    @Inject
    public PivotAggregationSearch(@Assisted AggregationEventProcessorConfig config,
                                  @Assisted AggregationEventProcessorParameters parameters,
                                  @Assisted String searchOwner,
                                  @Assisted EventDefinition eventDefinition,
                                  SearchJobService searchJobService,
                                  QueryEngine queryEngine,
                                  EventsConfigurationProvider configProvider,
                                  MoreSearch moreSearch) {
        this.config = config;
        this.parameters = parameters;
        this.searchOwner = searchOwner;
        this.eventDefinition = eventDefinition;
        this.searchJobService = searchJobService;
        this.queryEngine = queryEngine;
        this.configurationProvider = configProvider;
        this.moreSearch = moreSearch;
    }

    private String metricName(AggregationSeries series) {
        return String.format(Locale.ROOT, "metric/%s/%s/%s",
                series.function().toString().toLowerCase(Locale.ROOT), series.field().orElse("<no-field>"), series.id());
    }

    @Override
    public AggregationResult doSearch() throws EventProcessorException {
        final SearchJob searchJob = getSearchJob(parameters, searchOwner, config.searchWithinMs());
        final QueryResult queryResult = searchJob.results().get(QUERY_ID);
        final QueryResult streamQueryResult = searchJob.results().get(STREAMS_QUERY_ID);

        final Set<SearchError> aggregationErrors = firstNonNull(queryResult.errors(), Collections.emptySet());
        final Set<SearchError> streamErrors = firstNonNull(streamQueryResult.errors(), Collections.emptySet());

        if (!aggregationErrors.isEmpty() || !streamErrors.isEmpty()) {
            final Set<SearchError> errors = aggregationErrors.isEmpty() ? streamErrors : aggregationErrors;

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

            if (errors.size() > 1) {
                throw new EventProcessorException("Pivot search failed with multiple errors.", false, eventDefinition);
            } else {
                throw new EventProcessorException(errors.iterator().next().description(), false, eventDefinition);
            }
        }

        final PivotResult pivotResult = (PivotResult) queryResult.searchTypes().get(PIVOT_ID);
        final PivotResult streamsResult = (PivotResult) streamQueryResult.searchTypes().get(STREAMS_PIVOT_ID);

        return AggregationResult.builder()
                .keyResults(extractValues(pivotResult))
                .effectiveTimerange(pivotResult.effectiveTimerange())
                .totalAggregatedMessages(pivotResult.total())
                .sourceStreams(extractSourceStreams(streamsResult))
                .build();
    }

    private ImmutableSet<String> extractSourceStreams(PivotResult pivotResult) {
        return pivotResult.rows().stream()
            // "non-leaf" values can show up when the "rollup" feature is enabled in the pivot search type
            .filter(row -> "leaf".equals(row.source()))
            // We can just take the first key value because we only group by "streams"
            .map(row -> row.key().get(0))
            .collect(ImmutableSet.toImmutableSet());
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

    private SearchJob getSearchJob(AggregationEventProcessorParameters parameters, String username, long searchWithinMs) throws EventProcessorException {
        final Search search = Search.builder()
                .queries(ImmutableSet.of(getAggregationQuery(parameters, searchWithinMs), getSourceStreamsQuery(parameters)))
                .build();
        final SearchJob searchJob = queryEngine.execute(searchJobService.create(search, username));
        try {
            Uninterruptibles.getUninterruptibly(
                searchJob.getResultFuture(),
                configurationProvider.get().eventsSearchTimeout(),
                TimeUnit.MILLISECONDS);
        } catch (ExecutionException e) {
            throw new EventProcessorException("Error executing search job: " + e.getMessage(), false, eventDefinition, e);
        } catch (TimeoutException e) {
            throw new EventProcessorException("Timeout while executing search job.", false, eventDefinition, e);
        } catch (Exception e) {
            throw new EventProcessorException("Unhandled exception in search job.", false, eventDefinition, e);
        }

        return searchJob;
    }

    /**
     * Returns the query to compute the sources streams for the aggregation.
     *
     * @param parameters processor parameters
     * @return source streams query
     */
    private Query getSourceStreamsQuery(AggregationEventProcessorParameters parameters) {
        final Pivot pivot = Pivot.builder()
            .id(STREAMS_PIVOT_ID)
            .rollup(true)
            .rowGroups(ImmutableList.of(Values.builder().limit(Integer.MAX_VALUE).field("streams").build()))
            .series(ImmutableList.of(Count.builder().id(STREAMS_PIVOT_COUNT_ID).build()))
            .build();

        final Set<SearchType> searchTypes = Collections.singleton(pivot);
        final Query.Builder queryBuilder = Query.builder()
            .id(STREAMS_QUERY_ID)
            .searchTypes(searchTypes)
            .query(ElasticsearchQueryString.builder().queryString(config.query()).build())
            .timerange(parameters.timerange());

        final Set<String> streams = getStreams(parameters);
        if (!streams.isEmpty()) {
            queryBuilder.filter(filteringForStreamIds(streams));
        }

        return queryBuilder.build();
    }

    /**
     * Returns the query to compute the aggregation.
     *
     * @param parameters processor parameters
     * @param searchWithinMs processor search within period. Used to build the date histogram
     * @return aggregation query
     */
    private Query getAggregationQuery(AggregationEventProcessorParameters parameters, long searchWithinMs) {
        final Pivot.Builder pivotBuilder = Pivot.builder()
                .id(PIVOT_ID)
                .rollup(true);

        final ImmutableList<SeriesSpec> series = config.series().stream()
                .map(entry -> entry.function().toSeriesSpec(metricName(entry), entry.field().orElse(null)))
                .collect(ImmutableList.toImmutableList());

        if (!series.isEmpty()) {
            pivotBuilder.series(series);
        }

        // Wrap every aggregation with a date histogram bucket of the searchWithin time range.
        // This allows us to run aggregations over larger time ranges than the searchWithin time.
        // The results will be received in time buckets of the searchWithin time size.
        final Time interval = Time.builder().interval(TimeUnitInterval.Builder.builder()
                .timeunit(String.valueOf(searchWithinMs) + "ms")
                .build()).field("timestamp").build();
        final List<BucketSpec> groupBy = new ArrayList<>();
        groupBy.add(interval);
        if (!config.groupBy().isEmpty()) {
            groupBy.addAll(config.groupBy().stream()
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
                    .collect(Collectors.toList()));
            pivotBuilder.rowGroups(groupBy);
        }

        final Set<SearchType> searchTypes = Collections.singleton(pivotBuilder.build());

        final Query.Builder queryBuilder = Query.builder()
                .id(QUERY_ID)
                .searchTypes(searchTypes)
                .query(ElasticsearchQueryString.builder().queryString(config.query()).build())
                .timerange(parameters.timerange());

        final Set<String> streams = getStreams(parameters);
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

    private Set<String> getStreams(AggregationEventProcessorParameters parameters) {
        // Streams in parameters should override the ones in the config
        Set<String> streamIds = parameters.streams().isEmpty() ? config.streams() : parameters.streams();
        final Set<String> existingStreams = moreSearch.loadStreams(streamIds).stream()
                .map(Stream::getId)
                .collect(toSet());
        final Set<String> nonExistingStreams = streamIds.stream()
                .filter(stream -> !existingStreams.contains(stream))
                .collect(toSet());
        if (nonExistingStreams.size() != 0) {
            LOG.warn("Removing non-existing streams <{}> from event definition <{}>/<{}>",
                    nonExistingStreams,
                    eventDefinition.id(),
                    eventDefinition.title());
        }
        return existingStreams;
    }
}

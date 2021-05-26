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
import org.graylog.plugins.views.search.errors.EmptyParameterError;
import org.graylog.plugins.views.search.errors.QueryError;
import org.graylog.plugins.views.search.errors.SearchError;
import org.graylog.plugins.views.search.filter.OrFilter;
import org.graylog.plugins.views.search.filter.StreamFilter;
import org.graylog.plugins.views.search.rest.PermittedStreams;
import org.graylog.plugins.views.search.searchtypes.pivot.BucketSpec;
import org.graylog.plugins.views.search.searchtypes.pivot.Pivot;
import org.graylog.plugins.views.search.searchtypes.pivot.PivotResult;
import org.graylog.plugins.views.search.searchtypes.pivot.SeriesSpec;
import org.graylog.plugins.views.search.searchtypes.pivot.buckets.DateRangeBucket;
import org.graylog.plugins.views.search.searchtypes.pivot.buckets.DateRange;
import org.graylog.plugins.views.search.searchtypes.pivot.buckets.Values;
import org.graylog.plugins.views.search.searchtypes.pivot.series.Count;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;
import org.graylog2.plugin.streams.Stream;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
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
import static com.google.common.base.Strings.isNullOrEmpty;
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
    private final PermittedStreams permittedStreams;

    @Inject
    public PivotAggregationSearch(@Assisted AggregationEventProcessorConfig config,
                                  @Assisted AggregationEventProcessorParameters parameters,
                                  @Assisted String searchOwner,
                                  @Assisted EventDefinition eventDefinition,
                                  SearchJobService searchJobService,
                                  QueryEngine queryEngine,
                                  EventsConfigurationProvider configProvider,
                                  MoreSearch moreSearch,
                                  PermittedStreams permittedStreams) {
        this.config = config;
        this.parameters = parameters;
        this.searchOwner = searchOwner;
        this.eventDefinition = eventDefinition;
        this.searchJobService = searchJobService;
        this.queryEngine = queryEngine;
        this.configurationProvider = configProvider;
        this.moreSearch = moreSearch;
        this.permittedStreams = permittedStreams;
    }

    private String metricName(AggregationSeries series) {
        return String.format(Locale.ROOT, "metric/%s/%s/%s",
                series.function().toString().toLowerCase(Locale.ROOT), series.field().orElse("<no-field>"), series.id());
    }

    @Override
    public AggregationResult doSearch() throws EventProcessorException {
        final SearchJob searchJob = getSearchJob(parameters, searchOwner, config.searchWithinMs(), config.executeEveryMs());
        final QueryResult queryResult = searchJob.results().get(QUERY_ID);
        final QueryResult streamQueryResult = searchJob.results().get(STREAMS_QUERY_ID);

        final Set<SearchError> aggregationErrors = firstNonNull(queryResult.errors(), Collections.emptySet());
        final Set<SearchError> streamErrors = firstNonNull(streamQueryResult.errors(), Collections.emptySet());

        if (!aggregationErrors.isEmpty() || !streamErrors.isEmpty()) {
            final Set<SearchError> errors = aggregationErrors.isEmpty() ? streamErrors : aggregationErrors;

            errors.forEach(error -> {
                if (error instanceof QueryError) {
                    final QueryError queryError = (QueryError) error;
                    final String backtrace = queryError.backtrace() != null ? queryError.backtrace() : "";
                    if (error instanceof EmptyParameterError) {
                        LOG.debug("Aggregation search query <{}> with empty Parameter: {}\n{}",
                                queryError.queryId(), queryError.description(), backtrace);
                    } else {
                        LOG.error("Aggregation search query <{}> returned an error: {}\n{}",
                                queryError.queryId(), queryError.description(), backtrace);
                    }
                } else {
                    LOG.error("Aggregation search returned an error: {}", error);
                }
            });

            // If we have only EmptyParameterErrors, just return an empty Result
            if (! (errors.stream().filter(e -> !(e instanceof EmptyParameterError)).count() > 1)) {
                return AggregationResult.empty();
            }
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
    ImmutableList<AggregationKeyResult> extractValues(PivotResult pivotResult) throws EventProcessorException {
        final ImmutableList.Builder<AggregationKeyResult> results = ImmutableList.builder();

        // Example PivotResult structures. The row value "key" is composed of: "metric/<function>/<field>/<series-id>"
        // The row "key" always contains the date range bucket value as first element.
        //
        // With group-by:
        // {
        //  "rows": [
        //    {
        //      "key": ["2020-03-27T16:23:12Z", "php", "box2"],
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
        //      "key": ["2020-03-27T16:23:12Z", "php"],
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
        //      "key": ["2020-03-27T16:23:12Z", "sshd","box2"],
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
        //      "key": ["2020-03-27T16:23:12Z"],
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

            // Safety guard against programming errors
            if (row.key().size() == 0 || isNullOrEmpty(row.key().get(0))) {
                throw new EventProcessorException("Invalid row key! Expected at least the date range timestamp value: " + row.key().toString(), true, eventDefinition);
            }

            // We always wrap aggregations in date range buckets so we can run aggregations for multiple ranges at once.
            // The timestamp value of the date range bucket will be part of the result.
            final String timeKey = row.key().get(0);
            final List<String> groupKey;

            if (row.key().size() > 1) {
                // The date range bucket value must not be exposed to consumers as part of the key so they
                // don't have to unwrap the key all the time.
                groupKey = row.key().subList(1, row.key().size());
            } else {
                groupKey = ImmutableList.of();
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
                                    .key(groupKey)
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
                    .key(groupKey)
                    .timestamp(DateTime.parse(timeKey).withZone(DateTimeZone.UTC))
                    .seriesValues(values.build())
                    .build());
        }

        return results.build();
    }

    private SearchJob getSearchJob(AggregationEventProcessorParameters parameters, String username,
                                   long searchWithinMs, long executeEveryMs) throws EventProcessorException {
        Search search = Search.builder()
                .queries(ImmutableSet.of(getAggregationQuery(parameters, searchWithinMs, executeEveryMs), getSourceStreamsQuery(parameters)))
                .parameters(config.queryParameters())
                .build();
        // This adds all streams if none were provided
        // TODO: Once we introduce "EventProcessor owners" this should only load the permitted streams of the
        //       user who created this EventProcessor.
        search = search.addStreamsToQueriesWithoutStreams(() -> permittedStreams.load((streamId) -> true));
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
     * @param searchWithinMs processor search within period. Used to build the date range buckets
     * @param executeEveryMs
     * @return aggregation query
     */
    private Query getAggregationQuery(AggregationEventProcessorParameters parameters, long searchWithinMs, long executeEveryMs) {
        final Pivot.Builder pivotBuilder = Pivot.builder()
                .id(PIVOT_ID)
                .rollup(true);

        final ImmutableList<SeriesSpec> series = config.series().stream()
                .map(entry -> entry.function().toSeriesSpec(metricName(entry), entry.field().orElse(null)))
                .collect(ImmutableList.toImmutableList());

        if (!series.isEmpty()) {
            pivotBuilder.series(series);
        }

        // Wrap every aggregation with date range buckets of the searchWithin time range.
        // If the aggregation is configured to be using a sliding window (searchWithin > executeEveryMs)
        // the time ranges will overlap.
        // This allows us to run aggregations over larger time ranges than the searchWithin time.
        // The results will be received in time buckets of the searchWithin time size.
        final DateRangeBucket dateRangeBucket = buildDateRangeBuckets(parameters.timerange(), searchWithinMs, executeEveryMs);
        final List<BucketSpec> groupBy = new ArrayList<>();

        // The first bucket must be the date range!
        groupBy.add(dateRangeBucket);

        if (!config.groupBy().isEmpty()) {
            // Then we add the configured groups
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
        }

        // We always have row groups because of the date range buckets
        pivotBuilder.rowGroups(groupBy);

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

    @VisibleForTesting
    static DateRangeBucket buildDateRangeBuckets(TimeRange timeRange, long searchWithinMs, long executeEveryMs) {
        final ImmutableList.Builder<DateRange> ranges = ImmutableList.builder();
        DateTime from = timeRange.getFrom();
        DateTime to;
        do {
            // The smallest configurable unit is 1 sec.
            // By dividing it before casting we avoid a potential int overflow
            to = from.plusSeconds((int) (searchWithinMs / 1000));
            ranges.add(DateRange.builder().from(from).to(to).build());
            from = from.plusSeconds((int) executeEveryMs/ 1000);
        } while (to.isBefore(timeRange.getTo()));

        return DateRangeBucket.builder().field("timestamp").ranges(ranges.build()).build();
    }
}

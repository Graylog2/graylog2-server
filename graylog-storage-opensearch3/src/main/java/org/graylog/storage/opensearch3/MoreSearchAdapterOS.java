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
package org.graylog.storage.opensearch3;

import com.google.common.base.Stopwatch;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.graylog.events.event.EventDto;
import org.graylog.events.processor.EventProcessorException;
import org.graylog.events.search.MoreSearch;
import org.graylog.events.search.MoreSearchAdapter;
import org.graylog.plugins.views.search.searchfilters.model.UsedSearchFilter;
import org.graylog.plugins.views.search.searchtypes.pivot.buckets.AutoInterval;
import org.graylog2.indexer.results.ChunkedResult;
import org.graylog2.indexer.results.MultiChunkResultRetriever;
import org.graylog2.indexer.results.ResultChunk;
import org.graylog2.indexer.results.ResultMessage;
import org.graylog2.indexer.results.ResultMessageFactory;
import org.graylog2.indexer.searches.ChunkCommand;
import org.graylog2.indexer.searches.Sorting;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.indexer.searches.timeranges.AbsoluteRange;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;
import org.opensearch.client.json.JsonData;
import org.opensearch.client.opensearch._types.ExpandWildcard;
import org.opensearch.client.opensearch._types.FieldSort;
import org.opensearch.client.opensearch._types.FieldValue;
import org.opensearch.client.opensearch._types.SortOptions;
import org.opensearch.client.opensearch._types.SortOrder;
import org.opensearch.client.opensearch._types.aggregations.Aggregation;
import org.opensearch.client.opensearch._types.aggregations.DateHistogramAggregate;
import org.opensearch.client.opensearch._types.aggregations.DateHistogramBucket;
import org.opensearch.client.opensearch._types.aggregations.FieldDateMath;
import org.opensearch.client.opensearch._types.aggregations.LongTermsAggregate;
import org.opensearch.client.opensearch._types.aggregations.MultiBucketBase;
import org.opensearch.client.opensearch._types.mapping.FieldType;
import org.opensearch.client.opensearch._types.query_dsl.BoolQuery;
import org.opensearch.client.opensearch._types.query_dsl.Query;
import org.opensearch.client.opensearch._types.query_dsl.TermsQueryField;
import org.opensearch.client.opensearch.core.SearchResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static com.google.common.base.Strings.isNullOrEmpty;

public class MoreSearchAdapterOS implements MoreSearchAdapter {
    private static final Logger LOG = LoggerFactory.getLogger(MoreSearchAdapterOS.class);
    private static final String termsAggregationName = "alert_type";
    private static final String histogramAggregationName = "histogram";

    private final OfficialOpensearchClient opensearchClient;
    private final Boolean allowLeadingWildcard;
    private final MultiChunkResultRetriever multiChunkResultRetriever;
    private final ResultMessageFactory messageFactory;

    @Inject
    public MoreSearchAdapterOS(OfficialOpensearchClient opensearchClient,
                               @Named("allow_leading_wildcard_searches") Boolean allowLeadingWildcard,
                               MultiChunkResultRetriever multiChunkResultRetriever,
                               ResultMessageFactory messageFactory) {
        this.opensearchClient = opensearchClient;
        this.allowLeadingWildcard = allowLeadingWildcard;
        this.multiChunkResultRetriever = multiChunkResultRetriever;
        this.messageFactory = messageFactory;
    }

    @Override
    public MoreSearch.Result eventSearch(String queryString, TimeRange timerange, Set<String> affectedIndices,
                                         Sorting sorting, int page, int perPage, Set<String> eventStreams,
                                         String filterString, Set<String> forbiddenSourceStreams, Map<String, Set<String>> extraFilters) {

        final org.opensearch.client.opensearch.core.SearchRequest newSearchRequest = org.opensearch.client.opensearch.core.SearchRequest.of(builder -> {
            builder.query(createQuery(queryString, timerange, eventStreams, filterString, forbiddenSourceStreams, extraFilters));
            builder.from((page - 1) * perPage);
            builder.size(perPage);
            builder.trackTotalHits(th -> th.enabled(true));
            builder.sort(newCreateSorting(sorting));

            builder.ignoreUnavailable(true);
            builder.expandWildcards(ExpandWildcard.Open);

            if(!affectedIndices.isEmpty()) {
                builder.index(new ArrayList<>(affectedIndices));
            }

            return builder;
        });

        if (LOG.isDebugEnabled()) {
            LOG.debug("Query:\n{}", newSearchRequest.query().toJsonString());
            LOG.debug("Execute search: {}", newSearchRequest.toJsonString());
        }

        final org.opensearch.client.opensearch.core.SearchResponse<Map> searchResult = opensearchClient.sync(c -> c.search(newSearchRequest, Map.class), "Unable to perform search query");

        final List<ResultMessage> hits = searchResult.hits().hits().stream()
                .map(hit -> messageFactory.parseFromSource(hit.id(), hit.index(), hit.source(), hit.highlight()))
                .collect(Collectors.toList());

        final long total = searchResult.hits().total().value();

        return MoreSearch.Result.builder()
                .results(hits)
                .resultsCount(total)
                .duration(searchResult.took())
                .usedIndexNames(affectedIndices)
                .executedQuery(newSearchRequest.toJsonString())
                .build();
    }

    private Query createQuery(String queryString, TimeRange timerange, Set<String> eventStreams, String filterString, Set<String> forbiddenSourceStreams, Map<String, Set<String>> extraFilters) {

        final BoolQuery.Builder boolQuery = BoolQuery.builder();

        final Query query = (queryString.isEmpty() || queryString.equals("*"))
                ? Query.builder().matchAll(m -> m).build()
                : queryStringQuery(queryString, allowLeadingWildcard);

        boolQuery.filter(query);
        boolQuery.filter(termsQuery(EventDto.FIELD_STREAMS, eventStreams));
        boolQuery.filter(timerangeQuery(timerange));


        extraFilters.entrySet()
                .stream()
                .flatMap(extraFilter -> extraFilter.getValue()
                        .stream()
                        .map(value -> buildExtraFilter(extraFilter.getKey(), value)))
                .forEach(boolQuery::filter);

        if (!isNullOrEmpty(filterString)) {
            boolQuery.filter(Query.builder().queryString(qs -> qs.query(filterString)).build());
        }

        if (!forbiddenSourceStreams.isEmpty()) {
            // If an event has any stream in "source_streams" that the calling search user is not allowed to access,
            // the event must not be in the search result.
            boolQuery.filter(forbiddenStreamsQuery(forbiddenSourceStreams));
        }
        return Query.of(b -> b.bool(boolQuery.build()));
    }

    @Nonnull
    private Query forbiddenStreamsQuery(Set<String> forbiddenSourceStreams) {
        final List<FieldValue> values = forbiddenSourceStreams.stream().map(FieldValue::of).toList();
        return Query.builder().bool(
                boolQueryBuilder -> boolQueryBuilder.mustNot(
                        mustNotBuilder -> mustNotBuilder.terms(
                                termsQueryBuilder -> termsQueryBuilder.field(EventDto.FIELD_SOURCE_STREAMS).terms(
                                        terms -> terms.value(values)
                                )
                        )
                )
        ).build();
    }

    private Query timerangeQuery(TimeRange timerange) {
        return Query.builder().range(TimeRangeQueryFactory.createTimeRangeQuery(timerange)).build();
    }

    private Query termsQuery(String field, Set<String> eventStreams) {
        final List<FieldValue> values = eventStreams.stream().map(FieldValue::of).collect(Collectors.toList());
        return Query.builder().terms(terms -> terms.field(field).terms(TermsQueryField.builder().value(values).build())).build();
    }

    private Query queryStringQuery(String queryString, Boolean allowLeadingWildcard) {
        return Query.builder().queryString(qs -> qs.query(queryString).allowLeadingWildcard(allowLeadingWildcard)).build();
    }

    @Override
    public MoreSearch.Histogram eventHistogram(String queryString, AbsoluteRange timerange, Set<String> affectedIndices,
                                               Set<String> eventStreams, String filterString, Set<String> forbiddenSourceStreams,
                                               ZoneId timeZone, Map<String, Set<String>> extraFilters) {
        final var filter = createQuery(queryString, timerange, eventStreams, filterString, forbiddenSourceStreams, extraFilters);

        final org.opensearch.client.opensearch.core.SearchRequest newSearchRequest = org.opensearch.client.opensearch.core.SearchRequest.of(builder -> {
            builder.query(filter);
            builder.size(0);
            builder.trackTotalHits(th -> th.enabled(true));

            builder.ignoreUnavailable(true);
            builder.expandWildcards(ExpandWildcard.Open);

            if(!affectedIndices.isEmpty()) {
                builder.index(new LinkedList<>(affectedIndices));
            }

            final var autoInterval = AutoInterval.create();
            final var interval = autoInterval.toDateInterval(timerange);

            final Aggregation histogramAggregation = Aggregation.builder().dateHistogram(dh -> {

                dh.interval(t -> t.time(interval.getQuantity().toString() + interval.getUnit()));

                dh.field(EventDto.FIELD_EVENT_TIMESTAMP)
                        .timeZone(timeZone.getId())
                        .minDocCount(0)
                        .extendedBounds(bounds -> bounds
                                .min(FieldDateMath.builder().expr(Tools.buildElasticSearchTimeFormat(timerange.from())).build())
                                .max(FieldDateMath.builder().expr(Tools.buildElasticSearchTimeFormat(timerange.to())).build()));

                return dh;
            })
                    .aggregations(termsAggregationName, Aggregation.builder().terms(terms -> terms.minDocCount(0).field(EventDto.FIELD_ALERT)).build())
                    .build();

            builder.aggregations(histogramAggregationName, histogramAggregation);
            return builder;
        });



        if (LOG.isDebugEnabled()) {
            LOG.debug("Query:\n{}", newSearchRequest.query().toJsonString());
            LOG.debug("Execute search: {}", newSearchRequest.toJsonString());
        }

        final SearchResponse<Map> searchResult = opensearchClient.sync(c -> c.search(newSearchRequest, Map.class), "Unable to perform search query");

        final DateHistogramAggregate histogramResult = searchResult.aggregations().get(histogramAggregationName).dateHistogram();
        final var histogramBuckets = histogramResult.buckets();

        final List<DateHistogramBucket> buckets = histogramBuckets.array();

        final var alerts = new ArrayList<MoreSearch.Histogram.Bucket>(buckets.size());
        final var events = new ArrayList<MoreSearch.Histogram.Bucket>(buckets.size());

        buckets.forEach(bucket -> {
            final LongTermsAggregate parsedTerms = bucket.aggregations().get(termsAggregationName).lterms();
            final ZonedDateTime dateTime = Instant.ofEpochMilli(bucket.key()).atZone(timeZone);
            final var alertCount = parsedTerms.buckets().array().stream().filter(b -> b.keyAsString().equals("true")).findFirst().map(MultiBucketBase::docCount).orElse(0L);
            final var eventCount = parsedTerms.buckets().array().stream().filter(b -> b.keyAsString().equals("false")).findFirst().map(MultiBucketBase::docCount).orElse(0L);
            alerts.add(new MoreSearch.Histogram.Bucket(dateTime, alertCount));
            events.add(new MoreSearch.Histogram.Bucket(dateTime, eventCount));
        });

        return new MoreSearch.Histogram(new MoreSearch.Histogram.EventsBuckets(events, alerts));
    }

    static Query buildExtraFilter(String field, String value) {
        // Handle range queries, which require special query builders.
        if (value.startsWith("<=")) {
            return Query.builder().range(range -> range.field(field).lte(JsonData.of(value.replace("<=", "")))).build();
        } else if (value.startsWith(">=")) {
            return Query.builder().range(range -> range.field(field).gte(JsonData.of(value.replace(">=", "")))).build();
        } else if (value.startsWith("<")) {
            return Query.builder().range(range -> range.field(field).lt(JsonData.of(value.replace("<", "")))).build();
        } else if (value.startsWith(">")) {
            return Query.builder().range(range -> range.field(field).gt(JsonData.of(value.replace(">", "")))).build();
        }
        return Query.builder().multiMatch(multiMatch -> multiMatch.fields(field).query(value)).build();
    }

    private SortOptions newCreateSorting(Sorting sorting) {
        final org.opensearch.client.opensearch._types.SortOrder order = sortOrder(sorting);

        return SortOptions.of(builder -> {
            if (EventDto.FIELD_TIMERANGE_START.equals(sorting.getField())) {
                builder.field(sort -> withUnmapped(sort.field(EventDto.FIELD_TIMERANGE_START).order(order), sorting));
                builder.field(sort -> withUnmapped(sort.field(EventDto.FIELD_TIMERANGE_END).order(order), sorting));
            } else {
                builder.field(sort -> withUnmapped(sort.field(sorting.getField()).order(order), sorting));
            }
            return builder;
        });
    }

    private FieldSort.Builder withUnmapped(FieldSort.Builder builder, Sorting sorting) {
        return sorting.getUnmappedType()
                .map(unmappedType -> builder
                        .unmappedType(fieldType(unmappedType))
                        .missing(missingValue(sorting)))
                .orElse(builder);
    }

    private FieldValue missingValue(Sorting sorting) {
        final boolean first = sorting.getUppercasedDirection().equals(SortOrder.Asc.name());
        return FieldValue.of(first ? "_first" : "_last");
    }

    private FieldType fieldType(String typeName) {
        return Arrays.stream(FieldType.values())
                .filter(t -> t.jsonValue().equalsIgnoreCase(typeName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Invalid field type: " + typeName));
    }

    private org.opensearch.client.opensearch._types.SortOrder sortOrder(Sorting sorting) {
        return Arrays.stream(org.opensearch.client.opensearch._types.SortOrder.values())
                .filter(s -> s.name().equalsIgnoreCase(sorting.getUppercasedDirection()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No sorting option named " + sorting.getUppercasedDirection()));
    }

    @Override
    public void scrollEvents(String queryString, TimeRange timeRange, Set<String> affectedIndices, Set<String> streams,
                             List<UsedSearchFilter> filters, int batchSize, ScrollEventsCallback resultCallback) throws EventProcessorException {
        final ChunkCommand chunkCommand = buildScrollCommand(queryString, timeRange, affectedIndices, filters, streams, batchSize);

        final ChunkedResult chunkedResult = multiChunkResultRetriever.retrieveChunkedResult(chunkCommand);

        final AtomicBoolean continueScrolling = new AtomicBoolean(true);
        final Stopwatch stopwatch = Stopwatch.createStarted();
        try {
            ResultChunk resultChunk = chunkedResult.nextChunk();
            while (continueScrolling.get() && resultChunk != null) {
                final List<ResultMessage> messages = resultChunk.messages();

                LOG.debug("Passing <{}> messages to callback", messages.size());
                resultCallback.accept(Collections.unmodifiableList(messages), continueScrolling);

                // Stop if the resultCallback told us to stop
                if (!continueScrolling.get()) {
                    break;
                }

                resultChunk = chunkedResult.nextChunk();
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            try {
                // Tell Elasticsearch that we are done with the scroll so it can release resources as soon as possible
                // instead of waiting for the scroll timeout to kick in.
                chunkedResult.cancel();
            } catch (Exception ignored) {
            }
            LOG.debug("Scrolling done - took {} ms", stopwatch.stop().elapsed(TimeUnit.MILLISECONDS));
        }
    }
}

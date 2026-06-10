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

import jakarta.inject.Inject;
import org.graylog.storage.search.SearchCommand;
import org.graylog2.indexer.ranges.IndexRange;
import org.graylog2.indexer.results.ChunkedResult;
import org.graylog2.indexer.results.FieldStatsResult;
import org.graylog2.indexer.results.ResultMessage;
import org.graylog2.indexer.results.ResultMessageFactory;
import org.graylog2.indexer.results.SearchResult;
import org.graylog2.indexer.searches.ChunkCommand;
import org.graylog2.indexer.searches.SearchesAdapter;
import org.graylog2.indexer.searches.SearchesConfig;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;
import org.opensearch.client.json.JsonData;
import org.opensearch.client.opensearch._types.aggregations.Aggregate;
import org.opensearch.client.opensearch._types.aggregations.Aggregation;
import org.opensearch.client.opensearch._types.aggregations.CardinalityAggregate;
import org.opensearch.client.opensearch._types.aggregations.ExtendedStatsAggregate;
import org.opensearch.client.opensearch._types.aggregations.ValueCountAggregate;
import org.opensearch.client.opensearch.core.SearchRequest;
import org.opensearch.client.opensearch.core.SearchResponse;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.google.common.base.MoreObjects.firstNonNull;

public class SearchesAdapterOS implements SearchesAdapter {
    private static final String AGG_CARDINALITY = "gl2_field_cardinality";
    private static final String AGG_EXTENDED_STATS = "gl2_extended_stats";
    private static final String AGG_VALUE_COUNT = "gl2_value_count";

    private final OfficialOpensearchClient client;
    private final Scroll scroll;
    private final SearchRequestFactoryOS searchRequestFactory;
    private final ResultMessageFactory resultMessageFactory;

    @Inject
    public SearchesAdapterOS(OfficialOpensearchClient client,
                             Scroll scroll,
                             SearchRequestFactoryOS searchRequestFactory,
                             ResultMessageFactory resultMessageFactory) {
        this.client = client;
        this.scroll = scroll;
        this.searchRequestFactory = searchRequestFactory;
        this.resultMessageFactory = resultMessageFactory;
    }

    @Override
    public ChunkedResult scroll(ChunkCommand chunkCommand) {
        return scroll.retrieveChunkedResult(chunkCommand);
    }

    @Override
    public SearchResult search(Set<String> indices, Set<IndexRange> indexRanges, SearchesConfig config) {
        SearchRequest.Builder builder = searchRequestFactory.create(SearchCommand.from(config));


        if (indexRanges.isEmpty()) {
            return SearchResult.empty(config.query(), builder.build().toJsonString());
        }

        final SearchRequest searchRequest = builder
                .index(indices.stream().toList())
                .build();
        final SearchResponse<JsonData> searchResult = client.sync(
                c -> c.search(searchRequest, JsonData.class),
                "Unable to perform search query");

        final List<ResultMessage> resultMessages = extractResultMessages(searchResult);
        final long totalResults = searchResult.hits().total().value();
        final long tookMs = searchResult.took();
        final String builtQuery = searchRequest.toJsonString();

        return new SearchResult(resultMessages, totalResults, indexRanges, config.query(), builtQuery, tookMs);
    }

    private List<ResultMessage> extractResultMessages(SearchResponse<JsonData> searchResult) {
        return searchResult.hits().hits().stream()
                .map(hit -> resultMessageFactory.parseFromSource(hit.id(), hit.index(), OSSerializationUtils.toMap(hit.source())))
                .collect(Collectors.toList());
    }

    @Override
    public FieldStatsResult fieldStats(String query, String filter, TimeRange range, Set<String> indices, String field, boolean includeCardinality, boolean includeStats, boolean includeCount) {
        final SearchesConfig config = SearchesConfig.builder()
                .query(query)
                .filter(filter)
                .range(range)
                .offset(0)
                .limit(-1)
                .build();
        final SearchCommand searchCommand = SearchCommand.from(config);
        SearchRequest.Builder builder = searchRequestFactory.create(searchCommand);
        builder.index(indices.stream().toList());

        if (includeCount) {
            builder.aggregations(AGG_VALUE_COUNT, Aggregation.of(a -> a.valueCount(v -> v.field(field))));
        }
        if (includeStats) {
            builder.aggregations(AGG_EXTENDED_STATS, Aggregation.of(a -> a.extendedStats(v -> v.field(field))));
        }
        if (includeCardinality) {
            builder.aggregations(AGG_CARDINALITY, Aggregation.of(a -> a.cardinality(v -> v.field(field))));
        }

        SearchRequest searchRequest = builder.build();

        if (indices.isEmpty()) {
            return FieldStatsResult.empty(query, searchRequest.toJsonString());
        }

        final SearchResponse<JsonData> searchResult = client.sync(
                c -> c.search(searchRequest, JsonData.class),
                "Unable to retrieve fields stats"
        );

        final List<ResultMessage> resultMessages = extractResultMessages(searchResult);
        final long tookMs = searchResult.took();

        ExtendedStatsAggregate extendedStatsAggregation =
                Optional.ofNullable(searchResult.aggregations().get(AGG_EXTENDED_STATS))
                        .filter(Aggregate::isExtendedStats)
                        .map(Aggregate::extendedStats)
                        .orElse(null);
        ValueCountAggregate valueCountAggregation =
                Optional.ofNullable(searchResult.aggregations().get(AGG_VALUE_COUNT))
                        .filter(Aggregate::isValueCount)
                        .map(Aggregate::valueCount)
                        .orElse(null);
        CardinalityAggregate cardinalityAggregation =
                Optional.ofNullable(searchResult.aggregations().get(AGG_CARDINALITY))
                        .filter(Aggregate::isCardinality)
                        .map(Aggregate::cardinality)
                        .orElse(null);

        return createFieldStatsResult(extendedStatsAggregation,
                valueCountAggregation,
                cardinalityAggregation,
                resultMessages,
                query,
                searchRequest.toJsonString(),
                tookMs);
    }

    private FieldStatsResult createFieldStatsResult(ExtendedStatsAggregate extendedStatsAggregation,
                                                    ValueCountAggregate valueCountAggregation,
                                                    CardinalityAggregate cardinalityAggregation,
                                                    List<ResultMessage> resultMessages,
                                                    String query,
                                                    String builtQuery,
                                                    long tookMs) {
        final long cardinality = cardinalityAggregation == null ? Long.MIN_VALUE : cardinalityAggregation.value();
        final long count = valueCountAggregation == null ? Long.MIN_VALUE : valueCountAggregation.value().longValue();

        double sum = Double.NaN;
        double sumOfSquares = Double.NaN;
        double mean = Double.NaN;
        double min = Double.NaN;
        double max = Double.NaN;
        double variance = Double.NaN;
        double stdDeviation = Double.NaN;

        if (extendedStatsAggregation != null) {
            sum = firstNonNull(extendedStatsAggregation.sum(), Double.NaN);
            sumOfSquares = firstNonNull(extendedStatsAggregation.sumOfSquares(), Double.NaN);
            mean = firstNonNull(extendedStatsAggregation.avg(), Double.NaN);
            min = firstNonNull(extendedStatsAggregation.min(), Double.NaN);
            max = firstNonNull(extendedStatsAggregation.max(), Double.NaN);
            variance = firstNonNull(extendedStatsAggregation.variance(), Double.NaN);
            stdDeviation = firstNonNull(extendedStatsAggregation.stdDeviation(), Double.NaN);
        }

        return FieldStatsResult.create(
                count,
                sum,
                sumOfSquares,
                mean,
                min,
                max,
                variance,
                stdDeviation,
                cardinality,
                resultMessages,
                query,
                builtQuery,
                tookMs
        );
    }
}

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
package org.graylog.storage.elasticsearch7;

import com.google.common.collect.Streams;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.action.search.SearchRequest;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.action.search.SearchResponse;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.aggregations.AggregationBuilders;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.aggregations.metrics.Cardinality;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.aggregations.metrics.ExtendedStats;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.aggregations.metrics.ValueCount;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.builder.SearchSourceBuilder;
import org.graylog2.indexer.ranges.IndexRange;
import org.graylog2.indexer.results.CountResult;
import org.graylog2.indexer.results.FieldStatsResult;
import org.graylog2.indexer.results.ResultMessage;
import org.graylog2.indexer.results.ScrollResult;
import org.graylog2.indexer.results.SearchResult;
import org.graylog2.indexer.searches.ScrollCommand;
import org.graylog2.indexer.searches.SearchesAdapter;
import org.graylog2.indexer.searches.SearchesConfig;
import org.graylog2.indexer.searches.Sorting;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;

import javax.inject.Inject;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.google.common.base.MoreObjects.firstNonNull;

public class SearchesAdapterES7 implements SearchesAdapter {
    private static final String AGG_CARDINALITY = "gl2_field_cardinality";
    private static final String AGG_EXTENDED_STATS = "gl2_extended_stats";
    private static final String AGG_VALUE_COUNT = "gl2_value_count";

    private final ElasticsearchClient client;
    private final Scroll scroll;
    private final SearchRequestFactory searchRequestFactory;

    @Inject
    public SearchesAdapterES7(ElasticsearchClient client,
                              Scroll scroll,
                              SearchRequestFactory searchRequestFactory) {
        this.client = client;
        this.scroll = scroll;
        this.searchRequestFactory = searchRequestFactory;
    }

    @Override
    public CountResult count(Set<String> affectedIndices, String query, TimeRange range, String filter) {
        final SearchesConfig config = SearchesConfig.builder()
                .query(query)
                .range(range)
                .filter(filter)
                .limit(0)
                .offset(0)
                .build();
        final SearchSourceBuilder searchSourceBuilder = searchRequestFactory.create(config);
        final SearchRequest searchRequest = new SearchRequest(affectedIndices.toArray(new String[0]))
                .source(searchSourceBuilder);

        final SearchResponse result = client.search(searchRequest, "Fetching message count failed for indices ");

        return CountResult.create(result.getHits().getTotalHits().value, result.getTook().getMillis());
    }

    @Override
    public ScrollResult scroll(Set<String> indexWildcards, Sorting sorting, String filter, String query, TimeRange range, int limit, int offset, List<String> fields) {
        return scroll(ScrollCommand.builder()
                .indices(indexWildcards)
                .sorting(sorting)
                .filter(filter)
                .query(query)
                .range(range)
                .limit(limit)
                .offset(offset)
                .fields(fields)
                .build());
    }

    @Override
    public ScrollResult scroll(Set<String> indexWildcards, Sorting sorting, String filter, String query, int batchSize) {
        return scroll(ScrollCommand.builder()
                .indices(indexWildcards)
                .sorting(sorting)
                .filter(filter)
                .query(query)
                .batchSize(batchSize)
                .build());
    }

    @Override
    public ScrollResult scroll(ScrollCommand scrollCommand) {
        return scroll.scroll(scrollCommand);
    }

    @Override
    public SearchResult search(Set<String> indices, Set<IndexRange> indexRanges, SearchesConfig config) {
        final SearchSourceBuilder searchSourceBuilder = searchRequestFactory.create(config);

        if (indexRanges.isEmpty()) {
            return SearchResult.empty(config.query(), searchSourceBuilder.toString());
        }

        final SearchRequest searchRequest = new SearchRequest(indices.toArray(new String[0]))
                .source(searchSourceBuilder);
        final SearchResponse searchResult = client.search(searchRequest, "Unable to perform search query");

        final List<ResultMessage> resultMessages = extractResultMessages(searchResult);
        final long totalResults = searchResult.getHits().getTotalHits().value;
        final long tookMs = searchResult.getTook().getMillis();
        final String builtQuery = searchSourceBuilder.toString();

        return new SearchResult(resultMessages, totalResults, indexRanges, config.query(), builtQuery, tookMs);
    }

    private List<ResultMessage> extractResultMessages(SearchResponse searchResult) {
        return Streams.stream(searchResult.getHits())
                    .map(hit -> ResultMessage.parseFromSource(hit.getId(), hit.getIndex(), hit.getSourceAsMap()))
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
        final SearchSourceBuilder searchSourceBuilder = searchRequestFactory.create(config);

        if (includeCount) {
            searchSourceBuilder.aggregation(AggregationBuilders.count(AGG_VALUE_COUNT).field(field));
        }
        if (includeStats) {
            searchSourceBuilder.aggregation(AggregationBuilders.extendedStats(AGG_EXTENDED_STATS).field(field));
        }
        if (includeCardinality) {
            searchSourceBuilder.aggregation(AggregationBuilders.cardinality(AGG_CARDINALITY).field(field));
        }

        if (indices.isEmpty()) {
            return FieldStatsResult.empty(query, searchSourceBuilder.toString());
        }

        final SearchRequest searchRequest = new SearchRequest(indices.toArray(new String[0]))
                .source(searchSourceBuilder);

        final SearchResponse searchResult = client.search(searchRequest, "Unable to retrieve fields stats");

        final List<ResultMessage> resultMessages = extractResultMessages(searchResult);
        final long tookMs = searchResult.getTook().getMillis();

        final ExtendedStats extendedStatsAggregation = searchResult.getAggregations().get(AGG_EXTENDED_STATS);
        final ValueCount valueCountAggregation = searchResult.getAggregations().get(AGG_VALUE_COUNT);
        final Cardinality cardinalityAggregation = searchResult.getAggregations().get(AGG_CARDINALITY);

        return createFieldStatsResult(extendedStatsAggregation,
                valueCountAggregation,
                cardinalityAggregation,
                resultMessages,
                query,
                searchSourceBuilder.toString(),
                tookMs);
    }

    private FieldStatsResult createFieldStatsResult(ExtendedStats extendedStatsAggregation,
                                                    ValueCount valueCountAggregation,
                                                    Cardinality cardinalityAggregation,
                                                    List<ResultMessage> resultMessages,
                                                    String query,
                                                    String builtQuery,
                                                    long tookMs) {
        final long cardinality = cardinalityAggregation == null ? Long.MIN_VALUE : cardinalityAggregation.getValue();
        final long count = valueCountAggregation == null ? Long.MIN_VALUE : valueCountAggregation.getValue();

        double sum = Double.NaN;
        double sumOfSquares = Double.NaN;
        double mean = Double.NaN;
        double min = Double.NaN;
        double max = Double.NaN;
        double variance = Double.NaN;
        double stdDeviation = Double.NaN;

        if (extendedStatsAggregation != null) {
            sum = firstNonNull(extendedStatsAggregation.getSum(), Double.NaN);
            sumOfSquares = firstNonNull(extendedStatsAggregation.getSumOfSquares(), Double.NaN);
            mean = firstNonNull(extendedStatsAggregation.getAvg(), Double.NaN);
            min = firstNonNull(extendedStatsAggregation.getMin(), Double.NaN);
            max = firstNonNull(extendedStatsAggregation.getMax(), Double.NaN);
            variance = firstNonNull(extendedStatsAggregation.getVariance(), Double.NaN);
            stdDeviation = firstNonNull(extendedStatsAggregation.getStdDeviation(), Double.NaN);
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

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
package org.graylog.storage.elasticsearch7.views;

import com.google.common.collect.Maps;
import com.google.inject.name.Named;
import org.graylog.plugins.views.search.Filter;
import org.graylog.plugins.views.search.GlobalOverride;
import org.graylog.plugins.views.search.Query;
import org.graylog.plugins.views.search.QueryResult;
import org.graylog.plugins.views.search.SearchJob;
import org.graylog.plugins.views.search.SearchType;
import org.graylog.plugins.views.search.elasticsearch.IndexLookup;
import org.graylog.plugins.views.search.engine.BackendQuery;
import org.graylog.plugins.views.search.engine.QueryBackend;
import org.graylog.plugins.views.search.errors.SearchError;
import org.graylog.plugins.views.search.errors.SearchTypeError;
import org.graylog.plugins.views.search.errors.SearchTypeErrorParser;
import org.graylog.plugins.views.search.filter.AndFilter;
import org.graylog.plugins.views.search.filter.OrFilter;
import org.graylog.plugins.views.search.filter.QueryStringFilter;
import org.graylog.plugins.views.search.filter.StreamFilter;
import org.graylog.plugins.views.search.searchfilters.db.UsedSearchFiltersToQueryStringsMapper;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.action.ShardOperationFailedException;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.action.search.MultiSearchResponse;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.action.search.SearchRequest;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.action.search.SearchResponse;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.action.support.IndicesOptions;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.index.query.BoolQueryBuilder;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.index.query.QueryBuilder;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.index.query.QueryBuilders;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.aggregations.Aggregation;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.aggregations.bucket.MultiBucketsAggregation;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.builder.SearchSourceBuilder;
import org.graylog.storage.elasticsearch7.BoolQueryTools;
import org.graylog.storage.elasticsearch7.ElasticsearchClient;
import org.graylog.storage.elasticsearch7.views.searchtypes.ESSearchTypeHandler;
import org.graylog2.indexer.ElasticsearchException;
import org.graylog2.indexer.FieldTypeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.graylog2.indexer.fieldtypes.DiscoveredFieldTypeService.ALL_MESSAGE_FIELDS_DOCUMENT_FIELD;

public class ElasticsearchBackend implements QueryBackend<ESGeneratedQueryContext> {
    private static final Logger LOG = LoggerFactory.getLogger(ElasticsearchBackend.class);

    private final Map<String, Provider<ESSearchTypeHandler<? extends SearchType>>> elasticsearchSearchTypeHandlers;
    private final ElasticsearchClient client;
    private final IndexLookup indexLookup;
    private final ESGeneratedQueryContext.Factory queryContextFactory;
    private final UsedSearchFiltersToQueryStringsMapper usedSearchFiltersToQueryStringsMapper;
    private final boolean allowLeadingWildcard;

    @Inject
    public ElasticsearchBackend(Map<String, Provider<ESSearchTypeHandler<? extends SearchType>>> elasticsearchSearchTypeHandlers,
                                ElasticsearchClient client,
                                IndexLookup indexLookup,
                                ESGeneratedQueryContext.Factory queryContextFactory,
                                UsedSearchFiltersToQueryStringsMapper usedSearchFiltersToQueryStringsMapper,
                                @Named("allow_leading_wildcard_searches") boolean allowLeadingWildcard) {
        this.elasticsearchSearchTypeHandlers = elasticsearchSearchTypeHandlers;
        this.client = client;
        this.indexLookup = indexLookup;

        this.queryContextFactory = queryContextFactory;
        this.usedSearchFiltersToQueryStringsMapper = usedSearchFiltersToQueryStringsMapper;
        this.allowLeadingWildcard = allowLeadingWildcard;
    }

    private QueryBuilder translateQueryString(String queryString) {
        return (queryString.isEmpty() || queryString.trim().equals("*"))
                ? QueryBuilders.matchAllQuery()
                : QueryBuilders.queryStringQuery(queryString).allowLeadingWildcard(allowLeadingWildcard);
    }

    @Override
    public ESGeneratedQueryContext generate(Query query, Set<SearchError> validationErrors) {
        final SearchSourceBuilder searchSourceBuilder = createSearchSourceBuilder(query);

        final ESGeneratedQueryContext queryContext = queryContextFactory.create(this, searchSourceBuilder, validationErrors);
        query.searchTypes().stream()
                .filter(searchType -> !isSearchTypeWithError(queryContext, searchType.id()))
                .forEach(searchType -> {
                    final String type = searchType.type();
                    final Provider<ESSearchTypeHandler<? extends SearchType>> searchTypeHandler = elasticsearchSearchTypeHandlers.get(type);
                    if (searchTypeHandler == null) {
                        LOG.error("Unknown search type {} for elasticsearch backend, cannot generate query part. Skipping this search type.", type);
                        queryContext.addError(new SearchTypeError(query, searchType.id(), "Unknown search type '" + type + "' for elasticsearch backend, cannot generate query"));
                        return;
                    }

                    final SearchSourceBuilder searchTypeSourceBuilder = queryContext.searchSourceBuilder(searchType);

                    final Set<String> effectiveStreamIds = query.effectiveStreams(searchType);

                    final BoolQueryBuilder searchTypeOverrides = QueryBuilders.boolQuery()
                            .must(searchTypeSourceBuilder.query());
                    BoolQueryTools.addTimeRange(searchTypeOverrides, query.effectiveTimeRange(searchType), "search type " + searchType.id());
                    BoolQueryTools.addStreams(searchTypeOverrides, effectiveStreamIds);

                    searchType.query().ifPresent(searchTypeQuery -> {
                        final QueryBuilder normalizedSearchTypeQuery = translateQueryString(searchTypeQuery.queryString());
                        searchTypeOverrides.must(normalizedSearchTypeQuery);
                    });

                    usedSearchFiltersToQueryStringsMapper.map(searchType.filters())
                            .stream()
                            .map(this::translateQueryString)
                            .forEach(searchTypeOverrides::must);

                    searchTypeSourceBuilder.query(searchTypeOverrides);

                    searchTypeHandler.get().generateQueryPart(query, searchType, queryContext);
                });

        return queryContext;
    }

    // TODO make pluggable
    public Optional<QueryBuilder> generateFilterClause(Filter filter) {
        if (filter == null) {
            return Optional.empty();
        }

        switch (filter.type()) {
            case AndFilter.NAME:
                final BoolQueryBuilder andBuilder = QueryBuilders.boolQuery();
                filter.filters().stream()
                        .map(this::generateFilterClause)
                        .forEach(optQueryBuilder -> optQueryBuilder.ifPresent(andBuilder::must));
                return Optional.of(andBuilder);
            case OrFilter.NAME:
                final BoolQueryBuilder orBuilder = QueryBuilders.boolQuery();
                // TODO for the common case "any of these streams" we can optimize the filter into
                // a single "termsQuery" instead of "termQuery OR termQuery" if all direct children are "StreamFilter"
                filter.filters().stream()
                        .map(this::generateFilterClause)
                        .forEach(optQueryBuilder -> optQueryBuilder.ifPresent(orBuilder::should));
                return Optional.of(orBuilder);
            case StreamFilter.NAME:
                // Skipping stream filter, will be extracted elsewhere
                return Optional.empty();
            case QueryStringFilter.NAME:
                return Optional.of(QueryBuilders.queryStringQuery(((QueryStringFilter) filter).query()));
        }
        return Optional.empty();
    }

    private SearchSourceBuilder createSearchSourceBuilder(final Query query) {
        final BackendQuery backendQuery = query.query();
        final QueryBuilder normalizedRootQuery = translateQueryString(backendQuery.queryString());
        final BoolQueryBuilder boolQuery = QueryBuilders.boolQuery()
                .filter(normalizedRootQuery);

        usedSearchFiltersToQueryStringsMapper.map(query.filters())
                .stream()
                .map(this::translateQueryString)
                .forEach(boolQuery::filter);

        // add the optional root query filters
        generateFilterClause(query.filter()).map(boolQuery::filter);

        return new SearchSourceBuilder()
                .query(boolQuery)
                .from(0)
                .size(0)
                .trackTotalHits(true);
    }

    @Override
    public Set<String> getFieldsPresentInSearchResultDocuments(final Query normalizedQuery, final int size) {
        final Set<String> affectedIndices = indexLookup.indexNamesForStreamsInTimeRange(normalizedQuery.usedStreamIds(), normalizedQuery.timerange());
        final SearchSourceBuilder searchSourceBuilder = createSearchSourceBuilder(normalizedQuery);
        final QueryBuilder query = searchSourceBuilder.query();

        if (query instanceof BoolQueryBuilder boolQueryBuilder) {
            BoolQueryTools.addTimeRange(boolQueryBuilder, normalizedQuery.timerange(), "query " + normalizedQuery.id());
            BoolQueryTools.addStreams(boolQueryBuilder, normalizedQuery.usedStreamIds());
        }
        final String aggregationName = ALL_MESSAGE_FIELDS_DOCUMENT_FIELD + "_aggr";
        searchSourceBuilder.aggregation(new TermsAggregationBuilder(aggregationName)
                .field(ALL_MESSAGE_FIELDS_DOCUMENT_FIELD)
                .size(size));
        final SearchResponse res = client.search(new SearchRequest()
                .source(searchSourceBuilder)
                .indices(affectedIndices.toArray(new String[0]))
                .indicesOptions(IndicesOptions.fromOptions(false, false, true, false)), "Unable to retrieve fields used in search result documents");

        final Aggregation aggregationResult = res.getAggregations().get(aggregationName);
        if (aggregationResult instanceof MultiBucketsAggregation) {
            final List<? extends MultiBucketsAggregation.Bucket> buckets = ((MultiBucketsAggregation) aggregationResult).getBuckets();
            if (buckets != null) {
                return buckets.stream()
                        .filter(Objects::nonNull)
                        .map(MultiBucketsAggregation.Bucket::getKeyAsString)
                        .collect(Collectors.toSet());
            }
        }

        return Set.of();
    }

    @Override
    public QueryResult doRun(SearchJob job, Query query, ESGeneratedQueryContext queryContext) {
        if (query.searchTypes().isEmpty()) {
            return QueryResult.builder()
                    .query(query)
                    .searchTypes(Collections.emptyMap())
                    .errors(new HashSet<>(queryContext.errors()))
                    .build();
        }
        LOG.debug("Running query {} for job {}", query.id(), job.getId());
        final HashMap<String, SearchType.Result> resultsMap = Maps.newHashMap();

        final Set<String> affectedIndices = indexLookup.indexNamesForStreamsInTimeRange(query.usedStreamIds(), query.timerange());

        final Map<String, SearchSourceBuilder> searchTypeQueries = queryContext.searchTypeQueries();
        final List<String> searchTypeIds = new ArrayList<>(searchTypeQueries.keySet());

        final List<SearchRequest> searches = searchTypeIds
                .stream()
                .map(searchTypeId -> {
                    final Set<String> affectedIndicesForSearchType = query.searchTypes().stream()
                            .filter(s -> s.id().equalsIgnoreCase(searchTypeId)).findFirst()
                            .flatMap(searchType -> {
                                if (searchType.effectiveStreams().isEmpty()
                                        && !query.globalOverride().flatMap(GlobalOverride::timerange).isPresent()
                                        && !searchType.timerange().isPresent()) {
                                    return Optional.empty();
                                }
                                return Optional.of(indexLookup.indexNamesForStreamsInTimeRange(query.effectiveStreams(searchType), query.effectiveTimeRange(searchType)));
                            })
                            .orElse(affectedIndices);

                    Set<String> indices = affectedIndicesForSearchType.isEmpty() ? Collections.singleton("") : affectedIndicesForSearchType;
                    return new SearchRequest()
                            .source(searchTypeQueries.get(searchTypeId))
                            .indices(indices.toArray(new String[0]))
                            .indicesOptions(IndicesOptions.fromOptions(false, false, true, false));
                })
                .collect(Collectors.toList());

        final List<MultiSearchResponse.Item> results = client.msearch(searches, "Unable to perform search query: ");

        for (SearchType searchType : query.searchTypes()) {
            final String searchTypeId = searchType.id();
            final Provider<ESSearchTypeHandler<? extends SearchType>> handlerProvider = elasticsearchSearchTypeHandlers.get(searchType.type());
            if (handlerProvider == null) {
                LOG.error("Unknown search type '{}', cannot convert query result.", searchType.type());
                // no need to add another error here, as the query generation code will have added the error about the missing handler already
                continue;
            }

            if (isSearchTypeWithError(queryContext, searchTypeId)) {
                LOG.error("Failed search type '{}', cannot convert query result, skipping.", searchType.type());
                // no need to add another error here, as the query generation code will have added the error about the missing handler already
                continue;
            }

            // we create a new instance because some search type handlers might need to track information between generating the query and
            // processing its result, such as aggregations, which depend on the name and type
            final ESSearchTypeHandler<? extends SearchType> handler = handlerProvider.get();
            final int searchTypeIndex = searchTypeIds.indexOf(searchTypeId);
            final MultiSearchResponse.Item multiSearchResponse = results.get(searchTypeIndex);
            if (multiSearchResponse.isFailure()) {
                ElasticsearchException e = new ElasticsearchException("Search type returned error: ", multiSearchResponse.getFailure());
                queryContext.addError(SearchTypeErrorParser.parse(query, searchTypeId, e));
            } else if (checkForFailedShards(multiSearchResponse).isPresent()) {
                ElasticsearchException e = checkForFailedShards(multiSearchResponse).get();
                queryContext.addError(SearchTypeErrorParser.parse(query, searchTypeId, e));
            } else {
                final SearchType.Result searchTypeResult = handler.extractResult(job, query, searchType, multiSearchResponse.getResponse(), queryContext);
                if (searchTypeResult != null) {
                    resultsMap.put(searchTypeId, searchTypeResult);
                }
            }
        }

        LOG.debug("Query {} ran for job {}", query.id(), job.getId());
        return QueryResult.builder()
                .query(query)
                .searchTypes(resultsMap)
                .errors(new HashSet<>(queryContext.errors()))
                .build();
    }

    private Optional<ElasticsearchException> checkForFailedShards(MultiSearchResponse.Item multiSearchResponse) {
        if (multiSearchResponse.isFailure()) {
            return Optional.of(new ElasticsearchException(multiSearchResponse.getFailureMessage(), multiSearchResponse.getFailure()));
        }

        final SearchResponse searchResponse = multiSearchResponse.getResponse();
        if (searchResponse != null && searchResponse.getFailedShards() > 0) {
            final List<Throwable> shardFailures = Arrays.stream(searchResponse.getShardFailures())
                    .map(ShardOperationFailedException::getCause)
                    .collect(Collectors.toList());
            final List<String> nonNumericFieldErrors = shardFailures
                    .stream()
                    .filter(shardFailure -> shardFailure.getMessage().contains("Expected numeric type on field"))
                    .map(Throwable::getMessage)
                    .distinct()
                    .collect(Collectors.toList());
            if (!nonNumericFieldErrors.isEmpty()) {
                return Optional.of(new FieldTypeException("Unable to perform search query: ", nonNumericFieldErrors));
            }

            final List<String> errors = shardFailures
                    .stream()
                    .map(Throwable::getMessage)
                    .distinct()
                    .collect(Collectors.toList());
            return Optional.of(new ElasticsearchException("Unable to perform search query: ", errors));
        }

        return Optional.empty();
    }
}

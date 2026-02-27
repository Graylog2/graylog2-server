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
package org.graylog.storage.opensearch3.views;

import com.google.common.collect.Maps;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Provider;
import jakarta.validation.constraints.NotNull;
import org.graylog.plugins.views.search.Filter;
import org.graylog.plugins.views.search.GlobalOverride;
import org.graylog.plugins.views.search.Query;
import org.graylog.plugins.views.search.QueryResult;
import org.graylog.plugins.views.search.SearchJob;
import org.graylog.plugins.views.search.SearchType;
import org.graylog.plugins.views.search.elasticsearch.IndexLookup;
import org.graylog.plugins.views.search.engine.BackendQuery;
import org.graylog.plugins.views.search.engine.QueryBackend;
import org.graylog.plugins.views.search.engine.QueryExecutionStats;
import org.graylog.plugins.views.search.engine.monitoring.collection.StatsCollector;
import org.graylog.plugins.views.search.errors.SearchError;
import org.graylog.plugins.views.search.errors.SearchTypeError;
import org.graylog.plugins.views.search.errors.SearchTypeErrorParser;
import org.graylog.plugins.views.search.filter.AndFilter;
import org.graylog.plugins.views.search.filter.OrFilter;
import org.graylog.plugins.views.search.filter.QueryStringFilter;
import org.graylog.plugins.views.search.filter.StreamFilter;
import org.graylog.plugins.views.search.searchfilters.db.UsedSearchFiltersToQueryStringsMapper;
import org.graylog.storage.opensearch3.OSSerializationUtils;
import org.graylog.storage.opensearch3.OfficialOpensearchClient;
import org.graylog.storage.opensearch3.TimeRangeQueryFactory;
import org.graylog.storage.opensearch3.views.searchtypes.OSSearchTypeHandler;
import org.graylog2.indexer.ElasticsearchException;
import org.graylog2.indexer.FieldTypeException;
import org.graylog2.indexer.ranges.IndexRange;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.search.QueryStringUtils;
import org.graylog2.streams.StreamService;
import org.joda.time.DateTimeZone;
import org.opensearch.client.json.JsonData;
import org.opensearch.client.opensearch._types.ErrorCause;
import org.opensearch.client.opensearch._types.ExpandWildcard;
import org.opensearch.client.opensearch._types.FieldValue;
import org.opensearch.client.opensearch._types.OpenSearchException;
import org.opensearch.client.opensearch._types.ShardFailure;
import org.opensearch.client.opensearch._types.Time;
import org.opensearch.client.opensearch._types.query_dsl.BoolQuery;
import org.opensearch.client.opensearch._types.query_dsl.MatchAllQuery;
import org.opensearch.client.opensearch._types.query_dsl.QueryStringQuery;
import org.opensearch.client.opensearch._types.query_dsl.TermsQuery;
import org.opensearch.client.opensearch.core.MsearchRequest;
import org.opensearch.client.opensearch.core.MsearchResponse;
import org.opensearch.client.opensearch.core.SearchRequest;
import org.opensearch.client.opensearch.core.msearch.MultiSearchItem;
import org.opensearch.client.opensearch.core.msearch.MultiSearchResponseItem;
import org.opensearch.client.opensearch.core.msearch.RequestItem;
import org.opensearch.client.opensearch.core.search.TrackHits;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.graylog2.shared.utilities.StringUtils.f;

public class OpenSearchBackend implements QueryBackend<OSGeneratedQueryContext> {
    private static final Logger LOG = LoggerFactory.getLogger(OpenSearchBackend.class);

    private final Map<String, Provider<OSSearchTypeHandler<? extends SearchType>>> openSearchSearchTypeHandlers;
    private final OfficialOpensearchClient client;
    private final IndexLookup indexLookup;
    private final OSGeneratedQueryContext.Factory queryContextFactory;
    private final UsedSearchFiltersToQueryStringsMapper usedSearchFiltersToQueryStringsMapper;
    private final boolean allowLeadingWildcard;
    private final StatsCollector<QueryExecutionStats> executionStatsCollector;
    private final StreamService streamService;
    private final Optional<Integer> indexerMaxConcurrentSearches;
    private final Optional<Integer> indexerMaxConcurrentShardRequests;

    @Inject
    public OpenSearchBackend(Map<String, Provider<OSSearchTypeHandler<? extends SearchType>>> elasticsearchSearchTypeHandlers,
                             OfficialOpensearchClient client,
                             IndexLookup indexLookup,
                             OSGeneratedQueryContext.Factory queryContextFactory,
                             UsedSearchFiltersToQueryStringsMapper usedSearchFiltersToQueryStringsMapper,
                             StatsCollector<QueryExecutionStats> executionStatsCollector,
                             StreamService streamService,
                             @Named("allow_leading_wildcard_searches") boolean allowLeadingWildcard,
                             @Named("indexer_max_concurrent_searches") @Nullable Integer indexerMaxConcurrentSearches,
                             @Named("indexer_max_concurrent_shard_requests") @Nullable Integer indexerMaxConcurrentShardRequests) {
        this.openSearchSearchTypeHandlers = elasticsearchSearchTypeHandlers;
        this.client = client;
        this.indexLookup = indexLookup;

        this.queryContextFactory = queryContextFactory;
        this.usedSearchFiltersToQueryStringsMapper = usedSearchFiltersToQueryStringsMapper;
        this.executionStatsCollector = executionStatsCollector;
        this.streamService = streamService;
        this.allowLeadingWildcard = allowLeadingWildcard;
        this.indexerMaxConcurrentSearches = Optional.ofNullable(indexerMaxConcurrentSearches);
        this.indexerMaxConcurrentShardRequests = Optional.ofNullable(indexerMaxConcurrentShardRequests);
    }

    private org.opensearch.client.opensearch._types.query_dsl.Query translateQueryString(final String queryString) {
        return QueryStringUtils.isEmptyOrMatchAllQueryString(queryString)
                ? MatchAllQuery.of(f -> f).toQuery()
                : QueryStringQuery.of(b -> b.query(queryString).allowLeadingWildcard(allowLeadingWildcard)).toQuery();
    }

    @Override
    public StatsCollector<QueryExecutionStats> getExecutionStatsCollector() {
        return this.executionStatsCollector;
    }

    @Override
    public OSGeneratedQueryContext generate(Query query, Set<SearchError> validationErrors, DateTimeZone timezone) {
        final BackendQuery backendQuery = query.query();

        final Set<SearchType> searchTypes = query.searchTypes();

        final org.opensearch.client.opensearch._types.query_dsl.Query normalizedRootQuery =
                translateQueryString(backendQuery.queryString());

        final BoolQuery.Builder boolQuery = BoolQuery.builder()
                .filter(normalizedRootQuery);

        usedSearchFiltersToQueryStringsMapper.map(query.filters())
                .stream()
                .map(this::translateQueryString)
                .forEach(boolQuery::filter);

        // add the optional root query filters
        generateFilterQuery(query.filter()).ifPresent(boolQuery::filter);

        MutableSearchRequestBuilder searchRequest = new MutableSearchRequestBuilder()
                .query(boolQuery.build().toQuery())
                .from(0)
                .size(0)
                .trackTotalHits(TrackHits.builder().enabled(true).build());

        final OSGeneratedQueryContext queryContext = queryContextFactory.create(this, searchRequest, validationErrors, timezone);
        searchTypes.stream()
                .filter(searchType -> !isSearchTypeWithError(queryContext, searchType.id()))
                .forEach(searchType -> {
                    final String type = searchType.type();
                    final Provider<OSSearchTypeHandler<? extends SearchType>> searchTypeHandler = openSearchSearchTypeHandlers.get(type);
                    if (searchTypeHandler == null) {
                        LOG.error("Unknown search type {} for elasticsearch backend, cannot generate query part. Skipping this search type.", type);
                        queryContext.addError(new SearchTypeError(query, searchType.id(), "Unknown search type '" + type + "' for elasticsearch backend, cannot generate query"));
                        return;
                    }

                    final MutableSearchRequestBuilder searchTypeSourceBuilder = queryContext.searchSourceBuilder(searchType);

                    final Set<String> effectiveStreamIds = query.effectiveStreams(searchType);

                    final BoolQuery.Builder searchTypeOverrides = BoolQuery.builder()
                            .must(searchTypeSourceBuilder.query())
                            .must(
                                    Objects.requireNonNull(
                                            TimeRangeQueryFactory.createTimeRangeQuery(
                                                    query.effectiveTimeRange(searchType)
                                            ).toQuery(),
                                            "Timerange for search type " + searchType.id() + " cannot be found in query or search type."
                                    )
                            );

                    if (effectiveStreamIds.stream().noneMatch(s -> s.startsWith(Stream.DATASTREAM_PREFIX))) {
                        searchTypeOverrides
                                .must(TermsQuery.builder()
                                        .field(Message.FIELD_STREAMS)
                                        .terms(t -> t.value(effectiveStreamIds.stream()
                                                .map(FieldValue::of)
                                                .toList()))
                                        .build().toQuery());
                    }

                    searchType.query().ifPresent(searchTypeQuery -> {
                        final org.opensearch.client.opensearch._types.query_dsl.Query normalizedSearchTypeQuery =
                                translateQueryString(searchTypeQuery.queryString());
                        searchTypeOverrides.must(normalizedSearchTypeQuery);
                    });

                    usedSearchFiltersToQueryStringsMapper.map(searchType.filters())
                            .stream()
                            .map(this::translateQueryString)
                            .forEach(searchTypeOverrides::must);

                    searchTypeSourceBuilder.query(searchTypeOverrides.build().toQuery());

                    searchTypeHandler.get().generateQueryPart(query, searchType, queryContext);
                });

        return queryContext;
    }

    public Optional<org.opensearch.client.opensearch._types.query_dsl.Query> generateFilterQuery(Filter filter) {
        if (filter == null) {
            return Optional.empty();
        }

        switch (filter.type()) {
            case AndFilter.NAME:
                final BoolQuery.Builder andBuilder = BoolQuery.builder();
                filter.filters().stream()
                        .map(this::generateFilterQuery)
                        .forEach(b -> b.ifPresent(andBuilder::must));
                return Optional.of(andBuilder.build().toQuery());
            case OrFilter.NAME:
                final BoolQuery.Builder orBuilder = BoolQuery.builder();
                // TODO for the common case "any of these streams" we can optimize the filter into
                // a single "termsQuery" instead of "termQuery OR termQuery" if all direct children are "StreamFilter"
                filter.filters().stream()
                        .map(this::generateFilterQuery)
                        .forEach(b -> b.ifPresent(orBuilder::should));
                return Optional.of(orBuilder.build().toQuery());
            case StreamFilter.NAME:
                // Skipping stream filter, will be extracted elsewhere
                return Optional.empty();
            case QueryStringFilter.NAME:
                return Optional.of(QueryStringQuery.of(b -> b.query(((QueryStringFilter) filter).query())).toQuery());
        }
        return Optional.empty();
    }

    @Override
    public Set<IndexRange> indexRangesForStreamsInTimeRange(Set<String> streamIds, TimeRange timeRange) {
        return indexLookup.indexRangesForStreamsInTimeRange(streamIds, timeRange);
    }

    @Override
    public Optional<String> streamTitle(String streamId) {
        return Optional.ofNullable(streamService.streamTitleFromCache(streamId));
    }

    @Override
    @WithSpan
    public QueryResult doRun(SearchJob job, Query query, OSGeneratedQueryContext queryContext) {
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

        final Map<String, MutableSearchRequestBuilder> searchTypeQueries = queryContext.searchTypeQueries();
        final List<String> searchTypeIds = new ArrayList<>(searchTypeQueries.keySet());

        final List<SearchRequest> searches = searchTypeIds
                .stream()
                .map(searchTypeId -> {
                    final Set<String> affectedIndicesForSearchType = query.searchTypes().stream()
                            .filter(s -> s.id().equalsIgnoreCase(searchTypeId)).findFirst()
                            .flatMap(searchType -> {
                                if (searchType.effectiveStreams().isEmpty()
                                        && query.globalOverride().flatMap(GlobalOverride::timerange).isEmpty()
                                        && searchType.timerange().isEmpty()) {
                                    return Optional.empty();
                                }
                                return Optional.of(indexLookup.indexNamesForStreamsInTimeRange(query.effectiveStreams(searchType), query.effectiveTimeRange(searchType)));
                            })
                            .orElse(affectedIndices);

                    MutableSearchRequestBuilder searchRequest = searchTypeQueries.get(searchTypeId)
                            .copy()
                            .allowNoIndices(true)
                            .ignoreUnavailable(true)
                            .expandWildcards(ExpandWildcard.Open);
                    if (affectedIndices != null && !affectedIndicesForSearchType.isEmpty()) {
                        searchRequest.index(affectedIndicesForSearchType.stream().toList());
                    }

                    if (!SearchJob.NO_CANCELLATION.equals(job.getCancelAfterSeconds())) {
                        searchRequest.cancelAfterTimeInterval(Time.of(t -> t.time(job.getCancelAfterSeconds() + "s")));
                    }
                    return searchRequest;
                })
                .map(request -> request.preference(job.getId()))
                .map(MutableSearchRequestBuilder::build)
                .toList();

        final CompletableFuture<MsearchResponse<JsonData>> mSearchFuture = cancellableMsearch(searches);
        job.setQueryExecutionFuture(query.id(), mSearchFuture);
        final @NotNull List<MultiSearchResponseItem<JsonData>> results = getResults(mSearchFuture, searches.size());

        for (SearchType searchType : query.searchTypes()) {
            final String searchTypeId = searchType.id();
            final Provider<OSSearchTypeHandler<? extends SearchType>> handlerProvider = openSearchSearchTypeHandlers.get(searchType.type());
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
            final OSSearchTypeHandler<? extends SearchType> handler = handlerProvider.get();
            final int searchTypeIndex = searchTypeIds.indexOf(searchTypeId);
            final MultiSearchResponseItem<JsonData> multiSearchResponse = results.get(searchTypeIndex);
            if (multiSearchResponse.isFailure()) {
                ElasticsearchException e = new ElasticsearchException("Search type returned error: " + multiSearchResponse.failure().error().reason());
                queryContext.addError(SearchTypeErrorParser.parse(query, searchTypeId, e));
            } else {
                Optional<ElasticsearchException> failedShards = checkForFailedShards(multiSearchResponse);
                if (failedShards.isPresent()) {
                    queryContext.addError(SearchTypeErrorParser.parse(query, searchTypeId, failedShards.get()));
                } else {
                    try {
                        final SearchType.Result searchTypeResult = handler.extractResult(query, searchType, multiSearchResponse.result(), queryContext);
                        if (searchTypeResult != null) {
                            resultsMap.put(searchTypeId, searchTypeResult);
                        }
                    } catch (Exception e) {
                        LOG.warn("Unable to extract results: ", e);
                        queryContext.addError(new SearchTypeError(query, searchTypeId, e));
                    }

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

    CompletableFuture<MsearchResponse<JsonData>> cancellableMsearch(List<SearchRequest> searches) {
        return client.async(c -> {
            List<RequestItem> requestItems = searches.stream()
                    .map(searchRequest -> {
                        SearchRequest.Builder builder = searchRequest.toBuilder();
                        indexerMaxConcurrentShardRequests.ifPresent(maxShardRequests ->
                                builder.maxConcurrentShardRequests(maxShardRequests.longValue()));
                        return builder.build();
                    })
                    .map(OSSerializationUtils::toMsearch)
                    .toList();

            MsearchRequest.Builder request = new MsearchRequest.Builder();
            indexerMaxConcurrentSearches
                    .map(Integer::longValue)
                    .ifPresent(request::maxConcurrentSearches);
            request.searches(requestItems);

            return c.msearch(request.build(), JsonData.class);

        }, "Error executing multi search");
    }

    @NotNull
    private static List<MultiSearchResponseItem<JsonData>> getResults(CompletableFuture<MsearchResponse<JsonData>> mSearchFuture,
                                                                      final int numSearchTypes) {
        try {
            //TODO: Timeout
            return mSearchFuture.get(1L, TimeUnit.DAYS).responses();
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            OpenSearchException cause = findCause(e);
            if (cause != null) {
                return Collections.nCopies(numSearchTypes, MultiSearchResponseItem.of(i -> i
                        .failure(f -> f.error(c -> c
                                .type(cause.error().type())
                                .reason(cause.error().reason()
                                )))));
            }
            return Collections.nCopies(numSearchTypes, MultiSearchResponseItem.of(i -> i
                    .failure(f -> f.error(c -> c.type("generic").reason(e.getMessage())))));
        }
    }

    private static OpenSearchException findCause(Throwable e) {
        Throwable cause = e.getCause();
        if (cause == null) return null;
        if (cause instanceof OpenSearchException) {
            return (OpenSearchException) cause;
        }
        return findCause(cause);
    }

    private boolean isMaxClauseCountException(ErrorCause cause) {
        var found = cause.type().startsWith("too_many_clauses");

        if (!found && cause.causedBy() != null) {
            return isMaxClauseCountException(cause.causedBy());
       }

       return found;
    }

    private final static int MAX_MSG_LENGTH = 1024;

    private String mapExceptionToErrorMessage(ErrorCause cause) {
        if (isMaxClauseCountException(cause)) {
            return "Your query exceeded the maxClauseCount setting of OpenSearch. This is probably due to a custom parameter filled from a lookup table. Please check you query and settings.";
        }

        // in case of the default, return the message cut down to a reasonable length so that it's shown appropriately in the FE
        final var msg = f("OpenSearch exception [type=%s, reason=%s].", cause.type(), cause.reason());
        return msg != null && msg.length() > MAX_MSG_LENGTH ?  msg.substring(0, MAX_MSG_LENGTH) + "..." : msg;
    }

    private Optional<ElasticsearchException> checkForFailedShards(MultiSearchResponseItem<JsonData> multiSearchResponse) {
        if (multiSearchResponse.isFailure()) {
            return Optional.of(new ElasticsearchException(multiSearchResponse.failure().error().reason()));
        }

        final MultiSearchItem<JsonData> searchResponse = multiSearchResponse.result();
        if (searchResponse != null && searchResponse.shards().failed() > 0) {
            final List<ErrorCause> shardFailures = searchResponse.shards().failures().stream()
                    .map(ShardFailure::reason)
                    .toList();
            final List<String> nonNumericFieldErrors = shardFailures
                    .stream()
                    .map(this::mapExceptionToErrorMessage)
                    .filter(message -> message.contains("Expected numeric type on field"))
                    .distinct()
                    .toList();
            if (!nonNumericFieldErrors.isEmpty()) {
                return Optional.of(new FieldTypeException("Unable to perform search query: ", nonNumericFieldErrors));
            }

            final List<String> errors = shardFailures
                    .stream()
                    .map(this::mapExceptionToErrorMessage)
                    .distinct()
                    .toList();
            return Optional.of(new ElasticsearchException("Unable to perform search query: ", errors));
        }

        return Optional.empty();
    }
}

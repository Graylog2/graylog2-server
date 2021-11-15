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
package org.graylog.storage.elasticsearch6.views;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import com.google.inject.name.Named;
import io.searchbox.client.JestClient;
import io.searchbox.client.JestResult;
import io.searchbox.core.MultiSearch;
import io.searchbox.core.MultiSearchResult;
import io.searchbox.core.Search;
import io.searchbox.core.Validate;
import org.graylog.plugins.views.search.Filter;
import org.graylog.plugins.views.search.GlobalOverride;
import org.graylog.plugins.views.search.Query;
import org.graylog.plugins.views.search.QueryResult;
import org.graylog.plugins.views.search.SearchJob;
import org.graylog.plugins.views.search.SearchType;
import org.graylog.plugins.views.search.elasticsearch.ElasticsearchQueryString;
import org.graylog.plugins.views.search.elasticsearch.IndexLookup;
import org.graylog.plugins.views.search.elasticsearch.QueryStringDecorators;
import org.graylog.plugins.views.search.engine.LuceneQueryParser;
import org.graylog.plugins.views.search.engine.LuceneQueryParsingException;
import org.graylog.plugins.views.search.engine.QueryBackend;
import org.graylog.plugins.views.search.engine.SearchConfig;
import org.graylog.plugins.views.search.engine.ValidationExplanation;
import org.graylog.plugins.views.search.engine.ValidationRequest;
import org.graylog.plugins.views.search.engine.ValidationResponse;
import org.graylog.plugins.views.search.engine.ValidationStatus;
import org.graylog.plugins.views.search.errors.SearchTypeError;
import org.graylog.plugins.views.search.errors.SearchTypeErrorParser;
import org.graylog.plugins.views.search.filter.AndFilter;
import org.graylog.plugins.views.search.filter.OrFilter;
import org.graylog.plugins.views.search.filter.QueryStringFilter;
import org.graylog.plugins.views.search.filter.StreamFilter;
import org.graylog.plugins.views.search.rest.MappedFieldTypeDTO;
import org.graylog.shaded.elasticsearch6.org.elasticsearch.index.query.BoolQueryBuilder;
import org.graylog.shaded.elasticsearch6.org.elasticsearch.index.query.QueryBuilder;
import org.graylog.shaded.elasticsearch6.org.elasticsearch.index.query.QueryBuilders;
import org.graylog.shaded.elasticsearch6.org.elasticsearch.search.builder.SearchSourceBuilder;
import org.graylog.storage.elasticsearch6.TimeRangeQueryFactory;
import org.graylog.storage.elasticsearch6.jest.JestUtils;
import org.graylog.storage.elasticsearch6.views.searchtypes.ESSearchTypeHandler;
import org.graylog.storage.elasticsearch6.views.validate.ValidatePayload;
import org.graylog.storage.elasticsearch6.views.validate.ValidationResult;
import org.graylog2.indexer.ElasticsearchException;
import org.graylog2.indexer.IndexMapping;
import org.graylog2.indexer.fieldtypes.MappedFieldTypesService;
import org.graylog2.plugin.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.graylog.storage.elasticsearch6.jest.JestUtils.checkForFailedShards;

public class ElasticsearchBackend implements QueryBackend<ESGeneratedQueryContext> {
    private static final Logger LOG = LoggerFactory.getLogger(ElasticsearchBackend.class);

    private final Map<String, Provider<ESSearchTypeHandler<? extends SearchType>>> elasticsearchSearchTypeHandlers;
    private final JestClient jestClient;
    private final IndexLookup indexLookup;
    private final QueryStringDecorators queryStringDecorators;
    private final ESGeneratedQueryContext.Factory queryContextFactory;
    private final boolean allowLeadingWildcard;
    private final ObjectMapper objectMapper;
    private final MappedFieldTypesService mappedFieldTypesService;
    private final LuceneQueryParser luceneQueryParser;

    @Inject
    public ElasticsearchBackend(Map<String, Provider<ESSearchTypeHandler<? extends SearchType>>> elasticsearchSearchTypeHandlers,
                                JestClient jestClient,
                                IndexLookup indexLookup,
                                QueryStringDecorators queryStringDecorators,
                                ESGeneratedQueryContext.Factory queryContextFactory,
                                @Named("allow_leading_wildcard_searches") boolean allowLeadingWildcard,
                                ObjectMapper objectMapper,
                                MappedFieldTypesService mappedFieldTypesService,
                                LuceneQueryParser luceneQueryParser) {
        this.elasticsearchSearchTypeHandlers = elasticsearchSearchTypeHandlers;
        this.jestClient = jestClient;
        this.indexLookup = indexLookup;

        this.queryStringDecorators = queryStringDecorators;
        this.queryContextFactory = queryContextFactory;
        this.allowLeadingWildcard = allowLeadingWildcard;
        this.objectMapper = objectMapper;
        this.mappedFieldTypesService = mappedFieldTypesService;
        this.luceneQueryParser = luceneQueryParser;
    }

    private QueryBuilder normalizeQueryString(String queryString) {
        return (queryString.isEmpty() || queryString.trim().equals("*"))
                ? QueryBuilders.matchAllQuery()
                : QueryBuilders.queryStringQuery(queryString).allowLeadingWildcard(allowLeadingWildcard);
    }

    @Override
    public ESGeneratedQueryContext generate(SearchJob job, Query query, Set<QueryResult> results, SearchConfig searchConfig) {
        final ElasticsearchQueryString backendQuery = (ElasticsearchQueryString) query.query();

        validateQueryTimeRange(query, searchConfig);

        final Set<SearchType> searchTypes = query.searchTypes();

        final String queryString = this.queryStringDecorators.decorate(backendQuery.queryString(), job, query, results);
        final QueryBuilder normalizedRootQuery = normalizeQueryString(queryString);

        final BoolQueryBuilder boolQuery = QueryBuilders.boolQuery()
                .filter(normalizedRootQuery);

        // add the optional root query filters
        generateFilterClause(query.filter(), job, query, results)
                .map(boolQuery::filter);

        final SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder()
                .query(boolQuery)
                .from(0)
                .size(0);

        final ESGeneratedQueryContext queryContext = queryContextFactory.create(this, searchSourceBuilder, job, query, results);
        for (SearchType searchType : searchTypes) {

            final Optional<SearchTypeError> searchTypeError = validateSearchType(query, searchType, searchConfig);
            if(searchTypeError.isPresent()) {
                LOG.error("Invalid search type {} for elasticsearch backend, cannot generate query part. Skipping this search type.", searchType.type());
                queryContext.addError(searchTypeError.get());
                continue;
            }


            final SearchSourceBuilder searchTypeSourceBuilder = queryContext.searchSourceBuilder(searchType);

            final Set<String> effectiveStreamIds = searchType.effectiveStreams().isEmpty()
                    ? query.usedStreamIds()
                    : searchType.effectiveStreams();

            final BoolQueryBuilder searchTypeOverrides = QueryBuilders.boolQuery()
                    .must(searchTypeSourceBuilder.query())
                    .must(
                            Objects.requireNonNull(
                                    TimeRangeQueryFactory.create(
                                            query.effectiveTimeRange(searchType)
                                    ),
                                    "Timerange for search type " + searchType.id() + " cannot be found in query or search type."
                            )
                    )
                    .must(QueryBuilders.termsQuery(Message.FIELD_STREAMS, effectiveStreamIds));

            searchType.query().ifPresent(q -> {
                final ElasticsearchQueryString searchTypeBackendQuery = (ElasticsearchQueryString) q;
                final String searchTypeQueryString = this.queryStringDecorators.decorate(searchTypeBackendQuery.queryString(), job, query, results);
                final QueryBuilder normalizedSearchTypeQuery = normalizeQueryString(searchTypeQueryString);
                searchTypeOverrides.must(normalizedSearchTypeQuery);
            });

            searchTypeSourceBuilder.query(searchTypeOverrides);

            final String type = searchType.type();
            final Provider<ESSearchTypeHandler<? extends SearchType>> searchTypeHandler = elasticsearchSearchTypeHandlers.get(type);
            if (searchTypeHandler == null) {
                LOG.error("Unknown search type {} for elasticsearch backend, cannot generate query part. Skipping this search type.", type);
                queryContext.addError(new SearchTypeError(query, searchType.id(), "Unknown search type '" + type + "' for elasticsearch backend, cannot generate query"));
                continue;
            }

            searchTypeHandler.get().generateQueryPart(job, query, searchType, queryContext);
        }

        return queryContext;
    }

    // TODO make pluggable
    public Optional<QueryBuilder> generateFilterClause(Filter filter, SearchJob job, Query query, Set<QueryResult> results) {
        if (filter == null) {
            return Optional.empty();
        }

        switch (filter.type()) {
            case AndFilter.NAME:
                final BoolQueryBuilder andBuilder = QueryBuilders.boolQuery();
                filter.filters().stream()
                        .map(filter1 -> generateFilterClause(filter1, job, query, results))
                        .forEach(optQueryBuilder -> optQueryBuilder.ifPresent(andBuilder::must));
                return Optional.of(andBuilder);
            case OrFilter.NAME:
                final BoolQueryBuilder orBuilder = QueryBuilders.boolQuery();
                // TODO for the common case "any of these streams" we can optimize the filter into
                // a single "termsQuery" instead of "termQuery OR termQuery" if all direct children are "StreamFilter"
                filter.filters().stream()
                        .map(filter1 -> generateFilterClause(filter1, job, query, results))
                        .forEach(optQueryBuilder -> optQueryBuilder.ifPresent(orBuilder::should));
                return Optional.of(orBuilder);
            case StreamFilter.NAME:
                // Skipping stream filter, will be extracted elsewhere
                return Optional.empty();
            case QueryStringFilter.NAME:
                return Optional.of(QueryBuilders.queryStringQuery(this.queryStringDecorators.decorate(((QueryStringFilter) filter).query(), job, query, results)));
        }
        return Optional.empty();
    }

    @Override
    public QueryResult doRun(SearchJob job, Query query, ESGeneratedQueryContext queryContext, Set<QueryResult> predecessorResults) {
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
        final List<Search> searches = searchTypeIds
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
                                final Set<String> usedStreamIds = searchType.effectiveStreams().isEmpty()
                                        ? query.usedStreamIds()
                                        : searchType.effectiveStreams();

                                return Optional.of(indexLookup.indexNamesForStreamsInTimeRange(usedStreamIds, query.effectiveTimeRange(searchType)));
                            })
                            .orElse(affectedIndices);

                    return new Search.Builder(searchTypeQueries.get(searchTypeId).toString())
                            .addType(IndexMapping.TYPE_MESSAGE)
                            .addIndex(affectedIndicesForSearchType.isEmpty() ? Collections.singleton("") : affectedIndicesForSearchType)
                            .allowNoIndices(false)
                            .ignoreUnavailable(false)
                            .build();
                })
                .collect(Collectors.toList());
        final MultiSearch.Builder multiSearchBuilder = new MultiSearch.Builder(searches);
        final MultiSearchResult result = JestUtils.execute(jestClient, multiSearchBuilder.build(), () -> "Unable to perform search query: ");

        for (SearchType searchType : query.searchTypes()) {
            final String searchTypeId = searchType.id();
            final Provider<ESSearchTypeHandler<? extends SearchType>> handlerProvider = elasticsearchSearchTypeHandlers.get(searchType.type());
            if (handlerProvider == null) {
                LOG.error("Unknown search type '{}', cannot convert query result.", searchType.type());
                // no need to add another error here, as the query generation code will have added the error about the missing handler already
                continue;
            }

            if(isSearchTypeWithError(queryContext, searchTypeId)) {
                LOG.error("Failed search type '{}', cannot convert query result, skipping.", searchType.type());
                // no need to add another error here, as the query generation code will have added the error about the missing handler already
                continue;
            }

            // we create a new instance because some search type handlers might need to track information between generating the query and
            // processing its result, such as aggregations, which depend on the name and type
            final ESSearchTypeHandler<? extends SearchType> handler = handlerProvider.get();
            final int searchTypeIndex = searchTypeIds.indexOf(searchTypeId);
            final MultiSearchResult.MultiSearchResponse multiSearchResponse = result.getResponses().get(searchTypeIndex);
            if (multiSearchResponse.isError) {
                ElasticsearchException e = JestUtils.specificException(() -> "Search type returned error: ", multiSearchResponse.error);
                queryContext.addError(SearchTypeErrorParser.parse(query, searchTypeId, e));
            } else if (checkForFailedShards(multiSearchResponse.searchResult).isPresent()) {
                ElasticsearchException e = checkForFailedShards(multiSearchResponse.searchResult).get();
                queryContext.addError(SearchTypeErrorParser.parse(query, searchTypeId, e));
            } else {
                final SearchType.Result searchTypeResult = handler.extractResult(job, query, searchType, multiSearchResponse.searchResult, queryContext);
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

    @Override
    public ValidationResponse validate(ValidationRequest req) {
        final Set<String> affectedIndices = Optional.ofNullable(req.streams()).map(s -> indexLookup.indexNamesForStreamsInTimeRange(s, req.timerange())).orElse(Collections.emptySet());

        final String queryString = ((ElasticsearchQueryString) req.query()).queryString();
        final ElasticsearchQueryString backendQuery = (ElasticsearchQueryString) req.query();
        final Validate.Builder builder = new Validate.Builder(new ValidatePayload(queryString, false))
                .setParameter("explain", true)
                .setParameter("rewrite", true);
        final JestResult result = JestUtils.execute(jestClient, builder.build(), () -> "Unable to perform validation: ");
        final ValidationResult response = objectMapper.convertValue(result.getJsonObject(), ValidationResult.class);

        final List<ValidationExplanation> explanations = response.getExplanations().stream()
                .filter(e -> affectedIndices.contains(e.getIndex())) // TODO: is there a better way to get only results for our indices?
                .map(e -> new ValidationExplanation(e.getIndex(), -1, e.isValid(), e.getExplanation(), e.getError()))
                .collect(Collectors.toList());

        return new ValidationResponse(response.isValid(), explanations, getUnknownFields(req, backendQuery, response));
    }

    private Set<String> getUnknownFields(ValidationRequest req, ElasticsearchQueryString backendQuery, ValidationResult response) {
        if (response.isValid()) {
            try {
                final Set<String> detectedFields = luceneQueryParser.getFieldNames(backendQuery.queryString());
                final Set<String> availableFields = mappedFieldTypesService.fieldTypesByStreamIds(req.streams(), req.timerange())
                        .stream()
                        .map(MappedFieldTypeDTO::name)
                        .collect(Collectors.toSet());
                return detectedFields.stream().filter(f -> !availableFields.contains(f)).collect(Collectors.toSet());
            } catch (LuceneQueryParsingException e) {
                LOG.warn("Failed to parse lucene query", e);
            }
        }
        return Collections.emptySet();
    }
}

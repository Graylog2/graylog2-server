package org.graylog.plugins.views.search.elasticsearch;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.graph.Traverser;
import io.searchbox.client.JestClient;
import io.searchbox.core.MultiSearch;
import io.searchbox.core.MultiSearchResult;
import io.searchbox.core.Search;
import io.searchbox.core.SearchResult;
import org.apache.commons.collections4.CollectionUtils;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.graylog.plugins.views.search.Filter;
import org.graylog.plugins.views.search.Parameter;
import org.graylog.plugins.views.search.Query;
import org.graylog.plugins.views.search.QueryMetadata;
import org.graylog.plugins.views.search.QueryResult;
import org.graylog.plugins.views.search.SearchJob;
import org.graylog.plugins.views.search.SearchType;
import org.graylog.plugins.views.search.elasticsearch.searchtypes.ESSearchTypeHandler;
import org.graylog.plugins.views.search.engine.QueryBackend;
import org.graylog.plugins.views.search.errors.QueryError;
import org.graylog.plugins.views.search.errors.SearchException;
import org.graylog.plugins.views.search.errors.SearchTypeError;
import org.graylog.plugins.views.search.filter.AndFilter;
import org.graylog.plugins.views.search.filter.OrFilter;
import org.graylog.plugins.views.search.filter.QueryStringFilter;
import org.graylog.plugins.views.search.filter.StreamFilter;
import org.graylog2.database.NotFoundException;
import org.graylog2.indexer.ElasticsearchException;
import org.graylog2.indexer.FieldTypeException;
import org.graylog2.indexer.IndexHelper;
import org.graylog2.indexer.IndexMapping;
import org.graylog2.indexer.cluster.jest.JestUtils;
import org.graylog2.indexer.ranges.IndexRange;
import org.graylog2.indexer.ranges.IndexRangeService;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.streams.StreamService;
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
import java.util.stream.StreamSupport;

import static com.google.common.base.MoreObjects.firstNonNull;
import static com.google.common.base.Preconditions.checkArgument;

public class ElasticsearchBackend implements QueryBackend<ESGeneratedQueryContext> {
    private static final Logger LOG = LoggerFactory.getLogger(ElasticsearchBackend.class);

    private final Map<String, Provider<ESSearchTypeHandler<? extends SearchType>>> elasticsearchSearchTypeHandlers;
    private final QueryStringParser queryStringParser;
    private final JestClient jestClient;
    private final IndexRangeService indexRangeService;
    private final StreamService streamService;
    private final ESQueryDecorators esQueryDecorators;

    @Inject
    public ElasticsearchBackend(Map<String, Provider<ESSearchTypeHandler<? extends SearchType>>> elasticsearchSearchTypeHandlers,
                                QueryStringParser queryStringParser,
                                JestClient jestClient,
                                IndexRangeService indexRangeService,
                                StreamService streamService,
                                ESQueryDecorators esQueryDecorators) {
        this.elasticsearchSearchTypeHandlers = elasticsearchSearchTypeHandlers;
        this.queryStringParser = queryStringParser;
        this.jestClient = jestClient;
        this.indexRangeService = indexRangeService;
        this.streamService = streamService;
        this.esQueryDecorators = esQueryDecorators;
    }

    @Override
    public ESGeneratedQueryContext generate(SearchJob job, Query query, Set<QueryResult> results) {
        final ElasticsearchQueryString backendQuery = (ElasticsearchQueryString) query.query();

        final Set<SearchType> searchTypes = query.searchTypes();
        if (searchTypes.isEmpty()) {
            throw new SearchException(new QueryError(query, "Cannot generate query without any search types"));
        }

        final String queryString = this.esQueryDecorators.decorate(backendQuery.queryString(), job, query, results);
        final QueryBuilder esBuilder = (queryString.isEmpty() || queryString.trim().equals("*")) ?
                QueryBuilders.matchAllQuery() :
                QueryBuilders.queryStringQuery(queryString).allowLeadingWildcard(true);

        final BoolQueryBuilder boolQuery = QueryBuilders.boolQuery()
                .filter(esBuilder)
                .filter(Objects.requireNonNull(IndexHelper.getTimestampRangeFilter(query.timerange()), "Timerange is missing."));

        // add the specified filters
        generateFilterClause(query.filter(), job, query, results)
                .map(boolQuery::filter);

        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder()
                .query(boolQuery)
                .from(0)
                .size(0);

        final ESGeneratedQueryContext queryContext = new ESGeneratedQueryContext(this, searchSourceBuilder, job, query, results);
        for (SearchType searchType : searchTypes) {
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
                if (CollectionUtils.isNotEmpty(filter.filters())) {
                    LOG.debug("Ignoring meaningless subfilters of StreamFilter");
                }
                //noinspection ConstantConditions
                return Optional.of(QueryBuilders.termQuery(Message.FIELD_STREAMS, ((StreamFilter) filter).streamId()));
            case QueryStringFilter.NAME:
                return Optional.of(QueryBuilders.queryStringQuery(this.esQueryDecorators.decorate(((QueryStringFilter) filter).query(), job, query, results)));
        }
        return Optional.empty();
    }

    private Set<Stream> loadStreams(Set<String> streamIds) {
        // TODO: Use method from `StreamService` which loads a collection of ids (when implemented) to prevent n+1.
        // Track https://github.com/Graylog2/graylog2-server/issues/4897 for progress.
        return streamIds.stream().map(streamId -> {
            try {
                return streamService.load(streamId);
            } catch (NotFoundException e) {
                throw new RuntimeException(e);
            }
        }).collect(Collectors.toSet());
    }

    @Override
    public QueryResult doRun(SearchJob job, Query query, ESGeneratedQueryContext queryContext, Set<QueryResult> predecessorResults) {
        LOG.debug("Running query {} for job {}", query.id(), job.getId());
        final HashMap<String, SearchType.Result> resultsMap = Maps.newHashMap();

        final Set<Stream> usedStreams = loadStreams(query.usedStreamIds());

        final IndexRangeContainsOneOfStreams indexRangeContainsOneOfStreams = new IndexRangeContainsOneOfStreams(usedStreams);
        final Set<String> affectedIndices = indicesByTimeRange(query.timerange()).stream()
                .filter(indexRangeContainsOneOfStreams)
                .map(IndexRange::indexName)
                .collect(Collectors.toSet());

        final Map<String, SearchSourceBuilder> searchTypeQueries = queryContext.searchTypeQueries();
        final List<String> searchTypeIds = new ArrayList<>(searchTypeQueries.keySet());
        final List<Search> searches = searchTypeIds
                .stream()
                .map(searchTypeId -> new Search.Builder(searchTypeQueries.get(searchTypeId).toString())
                        .addType(IndexMapping.TYPE_MESSAGE)
                        .addIndex(affectedIndices.isEmpty() ? Collections.singleton("") : affectedIndices)
                        .allowNoIndices(false)
                        .ignoreUnavailable(false)
                        .build())
                .collect(Collectors.toList());
        final MultiSearch.Builder multiSearchBuilder = new MultiSearch.Builder(searches);
        final MultiSearchResult result = JestUtils.execute(jestClient, multiSearchBuilder.build(), () -> "Unable to perform search query");

        for (SearchType searchType : query.searchTypes()) {
            final String searchTypeId = searchType.id();
            final Provider<ESSearchTypeHandler<? extends SearchType>> handlerProvider = elasticsearchSearchTypeHandlers.get(searchType.type());
            if (handlerProvider == null) {
                LOG.error("Unknown search type '{}', cannot convert query result.", searchType.type());
                // no need to add another error here, as the query generation code will have added the error about the missing handler already
                continue;
            }
            // we create a new instance because some search type handlers might need to track information between generating the query and
            // processing its result, such as aggregations, which depend on the name and type
            final ESSearchTypeHandler<? extends SearchType> handler = handlerProvider.get();
            final int searchTypeIndex = searchTypeIds.indexOf(searchTypeId);
            final MultiSearchResult.MultiSearchResponse multiSearchResponse = result.getResponses().get(searchTypeIndex);
            if (multiSearchResponse.isError) {
                queryContext.addError(new SearchTypeError(query, searchTypeId, JestUtils.specificException(() -> "Search type returned error: ", multiSearchResponse.error)));
            } else if (checkForFailedShards(multiSearchResponse.searchResult).isPresent()) {
                queryContext.addError(new SearchTypeError(query, searchTypeId, checkForFailedShards(multiSearchResponse.searchResult).get()));
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

    private Optional<ElasticsearchException> checkForFailedShards(SearchResult result) {
        // unwrap shard failure due to non-numeric mapping. this happens when searching across index sets
        // if at least one of the index sets comes back with a result, the overall result will have the aggregation
        // but not considered failed entirely. however, if one shard has the error, we will refuse to respond
        // otherwise we would be showing empty graphs for non-numeric fields.
        final JsonNode shards = result.getJsonObject().path("_shards");
        final double failedShards = shards.path("failed").asDouble();

        if (failedShards > 0) {
            final List<String> errors = StreamSupport.stream(shards.path("failures").spliterator(), false)
                    .map(failure -> failure.path("reason").path("reason").asText())
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());

            final List<String> nonNumericFieldErrors = errors.stream()
                    .filter(error -> error.startsWith("Expected numeric type on field"))
                    .collect(Collectors.toList());
            if (!nonNumericFieldErrors.isEmpty()) {
                return Optional.of(new FieldTypeException("Unable to perform search query", nonNumericFieldErrors));
            }

            return Optional.of(new ElasticsearchException("Unable to perform search query", errors));
        }

        return Optional.empty();
    }

    private Set<IndexRange> indicesByTimeRange(TimeRange timerange) {
        return indexRangeService.find(timerange.getFrom(), timerange.getTo());
    }

    private Set<String> queryStringsFromFilter(Filter entry) {
        if (entry != null) {
            final Traverser<Filter> filterTraverser = Traverser.forTree(filter -> firstNonNull(filter.filters(), Collections.emptySet()));
            return StreamSupport.stream(filterTraverser.breadthFirst(entry).spliterator(), false)
                    .filter(filter -> filter instanceof QueryStringFilter)
                    .map(queryStringFilter -> ((QueryStringFilter) queryStringFilter).query())
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
        }
        return Collections.emptySet();
    }

    @Override
    public QueryMetadata parse(ImmutableSet<Parameter> declaredParameters, Query query) {
        checkArgument(query.query() instanceof ElasticsearchQueryString);
        final String mainQueryString = ((ElasticsearchQueryString) query.query()).queryString();
        final java.util.stream.Stream<String> queryStringStreams = java.util.stream.Stream.concat(
                java.util.stream.Stream.of(mainQueryString),
                query.searchTypes().stream().flatMap(searchType -> queryStringsFromFilter(searchType.filter()).stream())
        );

        final QueryMetadata metadataForParameters = queryStringStreams
                .map(queryStringParser::parse)
                .reduce(QueryMetadata.builder().build(), (meta1, meta2) -> QueryMetadata.builder().usedParameterNames(
                        Sets.union(meta1.usedParameterNames(), meta2.usedParameterNames())
                ).build());


        return metadataForParameters;
    }
}

package org.graylog.plugins.views.search.engine;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableSet;
import org.graylog.plugins.views.search.Query;
import org.graylog.plugins.views.search.Search;
import org.graylog.plugins.views.search.SearchType;
import org.graylog.plugins.views.search.elasticsearch.ElasticsearchQueryString;
import org.graylog.plugins.views.search.elasticsearch.QueryStringDecorators;
import org.graylog.plugins.views.search.permissions.SearchUser;
import org.graylog.plugins.views.search.rest.ExecutionState;

import javax.inject.Inject;
import java.util.Set;
import java.util.stream.Collectors;

import static com.google.common.base.MoreObjects.firstNonNull;

public class PluggableSearchNormalizer implements SearchNormalizer {
    private final ObjectMapper objectMapper;
    private final QueryStringDecorators queryStringDecorators;

    @Inject
    public PluggableSearchNormalizer(ObjectMapper objectMapper, QueryStringDecorators queryStringDecorators) {
        this.objectMapper = objectMapper;
        this.queryStringDecorators = queryStringDecorators;
    }

    private Query normalizeQuery(Query query, Search search) {
        return query.toBuilder()
                .query(ElasticsearchQueryString.of(this.queryStringDecorators.decorate(query.query().queryString(), search::getParameter, query)))
                .searchTypes(query.searchTypes().stream().map(searchType -> normalizeSearchType(searchType, query, search)).collect(Collectors.toSet()))
                .build();
    }

    private SearchType normalizeSearchType(SearchType searchType, Query query, Search search) {
        return searchType.query()
                .map(backendQuery -> searchType.withQuery(ElasticsearchQueryString.of(this.queryStringDecorators.decorate(backendQuery.queryString(), search::getParameter, query))))
                .orElse(searchType);
    }

    public Search normalize(Search search, SearchUser searchUser, ExecutionState executionState) {
        final Search searchWithStreams = search.addStreamsToQueriesWithoutStreams(() -> searchUser.streams().loadAll());

        final Search searchWithExecutionState = searchWithStreams.applyExecutionState(objectMapper, firstNonNull(executionState, ExecutionState.empty()));
        final Set<Query> newQueries = searchWithExecutionState.queries().stream()
                .map(query -> normalizeQuery(query, search))
                .collect(Collectors.toSet());
        return searchWithExecutionState.toBuilder().queries(ImmutableSet.copyOf(newQueries)).build();
    }
}

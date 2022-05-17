package org.graylog.plugins.views.search.engine;

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

public class DecorateQueryStringsNormalizer implements SearchNormalizer{
    private final QueryStringDecorators queryStringDecorators;

    @Inject
    public DecorateQueryStringsNormalizer(QueryStringDecorators queryStringDecorators) {
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
        final Set<Query> newQueries = search.queries().stream()
                .map(query -> normalizeQuery(query, search))
                .collect(Collectors.toSet());
        return search.toBuilder().queries(ImmutableSet.copyOf(newQueries)).build();
    }

}

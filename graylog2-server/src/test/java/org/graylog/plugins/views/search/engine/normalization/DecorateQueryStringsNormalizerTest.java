package org.graylog.plugins.views.search.engine.normalization;

import com.google.common.collect.ImmutableSet;
import org.graylog.plugins.views.search.Query;
import org.graylog.plugins.views.search.Search;
import org.graylog.plugins.views.search.SearchType;
import org.graylog.plugins.views.search.elasticsearch.ElasticsearchQueryString;
import org.graylog.plugins.views.search.elasticsearch.QueryStringDecorators;
import org.graylog.plugins.views.search.engine.PositionTrackingQuery;
import org.graylog.plugins.views.search.engine.QueryStringDecorator;
import org.graylog.plugins.views.search.filter.QueryStringFilter;
import org.graylog.plugins.views.search.rest.ExecutionState;
import org.graylog.plugins.views.search.rest.TestSearchUser;
import org.graylog.plugins.views.search.searchtypes.MessageList;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class DecorateQueryStringsNormalizerTest {
    private final QueryStringDecorator dummyQueryStringDecorator = (queryString, parameterProvider, query) -> PositionTrackingQuery.of("Hey there!");
    private final DecorateQueryStringsNormalizer decorateQueryStringsNormalizer = new DecorateQueryStringsNormalizer(
            new QueryStringDecorators(Optional.of(dummyQueryStringDecorator))
    );

    @Test
    void decoratesQueryStrings() {
        final Search search = Search.builder()
                .queries(ImmutableSet.of(
                        Query.builder()
                                .query(ElasticsearchQueryString.of("action:index"))
                                .build()
                ))
                .build();


        final Search normalizedSearch = decorateQueryStringsNormalizer.normalize(search, TestSearchUser.builder().build(), ExecutionState.empty());

        assertThat(normalizedSearch.queries())
                .hasSize(1)
                .allMatch(query -> query.query().queryString().equals("Hey there!"));
    }

    @Test
    void decoratesQueryStringFilters() {
        final Search search = Search.builder()
                .queries(ImmutableSet.of(
                        Query.builder()
                                .filter(QueryStringFilter.builder().query("action:index").build())
                                .build()
                ))
                .build();


        final Search normalizedSearch = decorateQueryStringsNormalizer.normalize(search, TestSearchUser.builder().build(), ExecutionState.empty());

        assertThat(normalizedSearch.queries())
                .hasSize(1)
                .extracting(Query::filter)
                .allMatch(queryFilter -> (queryFilter instanceof QueryStringFilter && ((QueryStringFilter) queryFilter).query().equals("Hey there!")));
    }

    @Test
    void decoratesSearchTypes() {
        final Search search = Search.builder()
                .queries(ImmutableSet.of(
                        Query.builder()
                                .searchTypes(
                                        Collections.singleton(MessageList.builder()
                                                .query(ElasticsearchQueryString.of("action:index"))
                                                .build())
                                )
                                .build()
                ))
                .build();


        final Search normalizedSearch = decorateQueryStringsNormalizer.normalize(search, TestSearchUser.builder().build(), ExecutionState.empty());

        assertThat(normalizedSearch.queries())
                .hasSize(1)
                .flatExtracting(Query::searchTypes)
                .hasSize(1)
                .extracting(searchType -> searchType.query().orElseThrow(IllegalStateException::new))
                .allMatch(query -> query.queryString().equals("Hey there!"));
    }

    @Test
    void decoratesSearchTypeFilters() {
        final Search search = Search.builder()
                .queries(ImmutableSet.of(
                        Query.builder()
                                .searchTypes(
                                        Collections.singleton(MessageList.builder()
                                                .filter(QueryStringFilter.builder()
                                                        .query("action:index")
                                                        .build())
                                                .build())
                                )
                                .build()
                ))
                .build();


        final Search normalizedSearch = decorateQueryStringsNormalizer.normalize(search, TestSearchUser.builder().build(), ExecutionState.empty());

        assertThat(normalizedSearch.queries())
                .hasSize(1)
                .flatExtracting(Query::searchTypes)
                .hasSize(1)
                .extracting(SearchType::filter)
                .allMatch(filter -> (filter instanceof QueryStringFilter && ((QueryStringFilter) filter).query().equals("Hey there!")));
    }
}



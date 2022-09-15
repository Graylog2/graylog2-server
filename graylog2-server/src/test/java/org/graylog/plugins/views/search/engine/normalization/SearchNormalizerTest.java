package org.graylog.plugins.views.search.engine.normalization;

import com.google.common.collect.ImmutableSet;
import org.graylog.plugins.views.search.Query;
import org.graylog.plugins.views.search.Search;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;


class SearchNormalizerTest {

    @Test
    void normalizesAllQueriesInSearch() {
        SearchNormalizer exclamationAddingTestNormalizer = (query, p) -> query.toBuilder().id(query.id() + "!").build();

        Search toTest = Search.builder()
                .id("test_search")
                .queries(ImmutableSet.of(
                        Query.builder().id("Hey").build(),
                        Query.builder().id("Ho").build()
                ))
                .build();

        Search normalized = exclamationAddingTestNormalizer.normalize(toTest);

        assertThat(normalized.queries())
                .hasSize(2)
                .contains(Query.builder().id("Hey!").build())
                .contains(Query.builder().id("Ho!").build());
    }

}

package org.graylog2.indexer.results;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.elasticsearch.search.SearchHits;
import org.graylog2.plugin.Message;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SearchResultTest {
    private SearchResult searchResult;

    @Before
    public void setUp() throws Exception {
        final SearchHits searchHits = mock(SearchHits.class);
        when(searchHits.iterator()).thenReturn(Collections.emptyIterator());
        this.searchResult = new SearchResult(searchHits, null, null, null, null);
    }

    @Test
    public void extractFieldsForEmptyResult() throws Exception {
        final Set<String> result = searchResult.extractFields(Collections.emptyList());

        assertThat(result)
            .isNotNull()
            .isEmpty();
    }

    @Test
    public void extractFieldsForTwoMessagesContainingDifferentFields() throws Exception {
        final ResultMessage r1 = mock(ResultMessage.class);
        final Message m1 = mock(Message.class);
        when(m1.getFieldNames()).thenReturn(ImmutableSet.of(
            "message",
            "source",
            "timestamp",
            "http_response",
            "gl2_source_node",
            "_index"
        ));
        when(r1.getMessage()).thenReturn(m1);

        final ResultMessage r2 = mock(ResultMessage.class);
        final Message m2 = mock(Message.class);
        when(m2.getFieldNames()).thenReturn(ImmutableSet.of(
            "message",
            "source",
            "timestamp",
            "took_ms",
            "gl2_source_collector"
        ));
        when(r2.getMessage()).thenReturn(m2);

        final Set<String> result = searchResult.extractFields(ImmutableList.of(r1, r2));

        assertThat(result)
            .isNotNull()
            .isNotEmpty()
            .hasSize(5)
            .containsExactlyInAnyOrder(
                "message",
                "source",
                "timestamp",
                "http_response",
                "took_ms"
            );
    }
}

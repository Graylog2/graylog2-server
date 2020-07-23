package org.graylog.storage.elasticsearch7;

import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.builder.SearchSourceBuilder;
import org.graylog2.indexer.searches.ScrollCommand;
import org.graylog2.plugin.indexer.searches.timeranges.AbsoluteRange;
import org.joda.time.DateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.graylog2.utilities.AssertJsonPath.assertJsonPath;

class SearchRequestFactoryTest {
    private SearchRequestFactory searchRequestFactory;

    @BeforeEach
    void setUp() {
        this.searchRequestFactory = new SearchRequestFactory(new SortOrderMapper(), true, true);
    }

    @Test
    void searchIncludesTimerange() {
        final SearchSourceBuilder search = this.searchRequestFactory.create(ScrollCommand.builder()
                .indices(Collections.singleton("graylog_0"))
                .range(AbsoluteRange.create(
                        DateTime.parse("2020-07-23T11:03:32.243Z"),
                        DateTime.parse("2020-07-23T11:08:32.243Z")
                ))
                .build());

        assertJsonPath(search, request -> {
            request.jsonPathAsListOf("$.query.bool.filter..range.timestamp.from", String.class)
                    .containsExactly("2020-07-23 11:03:32.243");
            request.jsonPathAsListOf("$.query.bool.filter..range.timestamp.to", String.class)
                    .containsExactly("2020-07-23 11:08:32.243");
        });
    }
}

package org.graylog.storage.elasticsearch7;

import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.builder.SearchSourceBuilder;
import org.graylog2.indexer.searches.ScrollCommand;
import org.graylog2.plugin.indexer.searches.timeranges.InvalidRangeParametersException;
import org.graylog2.plugin.indexer.searches.timeranges.RelativeRange;
import org.joda.time.DateTime;
import org.joda.time.DateTimeUtils;
import org.junit.jupiter.api.AfterEach;
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

    @AfterEach
    public void tearDown() {
        DateTimeUtils.setCurrentMillisSystem();
    }

    @Test
    void searchIncludesTimerange() throws InvalidRangeParametersException {
        DateTimeUtils.setCurrentMillisFixed(DateTime.parse("2020-07-23T11:08:32.243Z").getMillis());

        final SearchSourceBuilder search = this.searchRequestFactory.create(ScrollCommand.builder()
                .indices(Collections.singleton("graylog_0"))
                .range(RelativeRange.create(300))
                .build());

        assertJsonPath(search, request -> {
            request.jsonPathAsListOf("$.query.bool.filter..range.timestamp.from", String.class)
                    .containsExactly("2020-07-23 11:03:32.243");
            request.jsonPathAsListOf("$.query.bool.filter..range.timestamp.to", String.class)
                    .containsExactly("2020-07-23 11:08:32.243");
        });
    }
}

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

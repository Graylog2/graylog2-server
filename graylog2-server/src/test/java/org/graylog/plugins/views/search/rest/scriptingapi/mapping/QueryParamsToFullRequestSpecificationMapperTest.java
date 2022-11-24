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
package org.graylog.plugins.views.search.rest.scriptingapi.mapping;

import org.graylog.plugins.views.search.rest.scriptingapi.parsing.TimerangeParser;
import org.graylog.plugins.views.search.rest.scriptingapi.request.Grouping;
import org.graylog.plugins.views.search.rest.scriptingapi.request.Metric;
import org.graylog.plugins.views.search.rest.scriptingapi.request.SearchRequestSpec;
import org.graylog2.plugin.indexer.searches.timeranges.KeywordRange;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.graylog.plugins.views.search.rest.scriptingapi.request.SearchRequestSpec.DEFAULT_TIMERANGE;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;


@ExtendWith(MockitoExtension.class)
class QueryParamsToFullRequestSpecificationMapperTest {

    private QueryParamsToFullRequestSpecificationMapper toTest;

    @Mock
    private TimerangeParser timerangeParser;

    @BeforeEach
    void setUp() {
        toTest = new QueryParamsToFullRequestSpecificationMapper(timerangeParser);
    }

    @Test
    void throwsExceptionOnNullGroups() {
        assertThrows(IllegalArgumentException.class, () -> toTest.simpleQueryParamsToFullRequestSpecification("*",
                Set.of(),
                "42d",
                null,
                List.of("avg:joe")));
    }

    @Test
    void throwsExceptionOnEmptyGroups() {
        assertThrows(IllegalArgumentException.class, () -> toTest.simpleQueryParamsToFullRequestSpecification("*",
                Set.of(),
                "42d",
                List.of(),
                List.of("avg:joe")));
    }

    @Test
    void throwsExceptionOnWrongMetricFormat() {
        assertThrows(IllegalArgumentException.class, () -> toTest.simpleQueryParamsToFullRequestSpecification("*",
                Set.of(),
                "42d",
                List.of("http_method"),
                List.of("avg:joe", "ayayayayay!")));
    }

    @Test
    void usesProperDefaults() {
        SearchRequestSpec searchRequestSpec = toTest.simpleQueryParamsToFullRequestSpecification(null,
                null,
                null,
                List.of("http_method"),
                null);

        assertThat(searchRequestSpec).isEqualTo(new SearchRequestSpec(
                        "*",
                        Set.of(),
                        DEFAULT_TIMERANGE,
                        List.of(new Grouping("http_method")),
                        List.of(new Metric(null, "count"))
                )
        );

        searchRequestSpec = toTest.simpleQueryParamsToFullRequestSpecification(null,
                null,
                null,
                List.of("http_method"),
                List.of());

        assertThat(searchRequestSpec).isEqualTo(new SearchRequestSpec(
                        "*",
                        Set.of(),
                        DEFAULT_TIMERANGE,
                        List.of(new Grouping("http_method")),
                List.of(new Metric(null, "count"))
                )
        );
    }

    @Test
    void createsProperRequestSpec() {
        doReturn(KeywordRange.create("last 1 day", "UTC")).when(timerangeParser).parseTimeRange("1d");
        final SearchRequestSpec searchRequestSpec = toTest.simpleQueryParamsToFullRequestSpecification("http_method:GET",
                Set.of("000000000000000000000001"),
                "1d",
                List.of("http_method", "controller"),
                List.of("avg:took_ms"));

        assertThat(searchRequestSpec).isEqualTo(new SearchRequestSpec(
                        "http_method:GET",
                        Set.of("000000000000000000000001"),
                        KeywordRange.create("last 1 day", "UTC"),
                        List.of(new Grouping("http_method"), new Grouping("controller")),
                List.of(new Metric("took_ms", "avg"))
                )
        );
    }
}

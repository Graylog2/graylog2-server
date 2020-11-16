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
package org.graylog2.decorators;

import com.floreysoft.jmte.Engine;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Range;
import org.graylog2.plugin.Tools;
import org.graylog2.rest.models.messages.responses.ResultMessageSummary;
import org.graylog2.rest.models.system.indexer.responses.IndexRangeSummary;
import org.graylog2.rest.resources.search.responses.SearchResponse;
import org.junit.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.graylog2.rest.models.messages.responses.ResultMessageSummary.create;

public class FormatStringDecoratorTest {
    private final Engine templateEngine = new Engine();

    @Test
    public void testFormat() {

        final DecoratorImpl decorator = getDecoratorConfig("${field_a}: ${field_b}", "message", true);

        final FormatStringDecorator formatStringDecorator = new FormatStringDecorator(decorator, templateEngine);
        final SearchResponse searchResponse = getSearchResponse();

        final SearchResponse response = formatStringDecorator.apply(searchResponse);

        assertThat(response.messages().size()).isEqualTo(4);
        assertThat(response.messages().get(0).message().get("message")).isEqualTo("1: b");
        assertThat(response.messages().get(1).message().containsKey("message")).isFalse();
        assertThat(response.messages().get(2).message().containsKey("message")).isFalse();
        assertThat(response.messages().get(3).message().containsKey("message")).isFalse();
    }

    @Test
    public void formatAllowEmptyValues() {
        final DecoratorImpl decorator = getDecoratorConfig("${field_a}: ${field_b}", "message", false);

        final FormatStringDecorator formatStringDecorator = new FormatStringDecorator(decorator, templateEngine);
        final SearchResponse searchResponse = getSearchResponse();

        final SearchResponse response = formatStringDecorator.apply(searchResponse);

        assertThat(response.messages().size()).isEqualTo(4);
        assertThat(response.messages().get(0).message().get("message")).isEqualTo("1: b");
        assertThat(response.messages().get(1).message().get("message")).isEqualTo("1:");
        assertThat(response.messages().get(2).message().get("message")).isEqualTo(": b");
        assertThat(response.messages().get(3).message().get("message")).isEqualTo(":");
    }

    private SearchResponse getSearchResponse() {
        final IndexRangeSummary indexRangeSummary = IndexRangeSummary.create("graylog_0",
                                                                             Tools.nowUTC().minusDays(1),
                                                                             Tools.nowUTC(),
                                                                             null,
                                                                             100);

        final ImmutableMultimap<String, Range<Integer>> hlRanges = ImmutableMultimap.of();
        final List<ResultMessageSummary> messages = ImmutableList.of(
                create(hlRanges, ImmutableMap.of("_id", "h", "field_a", "1", "field_b", "b"), "graylog_0"),
                create(hlRanges, ImmutableMap.of("_id", "h", "field_a", "1"), "graylog_0"),
                create(hlRanges, ImmutableMap.of("_id", "h", "field_b", "b"), "graylog_0"),
                create(hlRanges, ImmutableMap.of("_id", "i", "foo", "1"), "graylog_0")
        );

        return SearchResponse.builder()
                .query("foo")
                .builtQuery("foo")
                .usedIndices(ImmutableSet.of(indexRangeSummary))
                .messages(messages)
                .fields(ImmutableSet.of("field_a", "field_b", "foo"))
                .time(100L)
                .totalResults(messages.size())
                .from(Tools.nowUTC().minusHours(1))
                .to(Tools.nowUTC()).build();
    }

    private DecoratorImpl getDecoratorConfig(String formatString, String targetField, boolean requireAllField) {
        return DecoratorImpl.create(
                "id",
                FormatStringDecorator.class.getCanonicalName(),
                ImmutableMap.of("format_string",
                                formatString, "target_field", targetField, "require_all_fields", requireAllField),
                Optional.empty(),
                1);
    }
}
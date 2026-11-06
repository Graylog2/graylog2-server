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
import org.graylog2.plugin.MessageFactory;
import org.graylog2.plugin.TestMessageFactory;
import org.graylog2.plugin.Tools;
import org.graylog2.rest.models.messages.responses.ResultMessageSummary;
import org.graylog2.rest.models.system.indexer.responses.IndexRangeSummary;
import org.graylog2.rest.resources.search.responses.SearchResponse;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.graylog2.rest.models.messages.responses.ResultMessageSummary.create;

public class FormatStringDecoratorTest {
    private static final DateTime FROM = new DateTime(2026, 11, 4, 10, 0, DateTimeZone.UTC);
    private static final DateTime TO = new DateTime(2026, 11, 4, 11, 30, DateTimeZone.UTC);

    private final MessageFactory messageFactory = new TestMessageFactory();
    private final Engine templateEngine = new DecoratorTemplateEngineProvider().get();

    @Test
    public void testFormat() {

        final DecoratorImpl decorator = getDecoratorConfig("${field_a}: ${field_b}", "message", true);

        final FormatStringDecorator formatStringDecorator = new FormatStringDecorator(decorator, templateEngine, messageFactory);
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

        final FormatStringDecorator formatStringDecorator = new FormatStringDecorator(decorator, templateEngine, messageFactory);
        final SearchResponse searchResponse = getSearchResponse();

        final SearchResponse response = formatStringDecorator.apply(searchResponse);

        assertThat(response.messages().size()).isEqualTo(4);
        assertThat(response.messages().get(0).message().get("message")).isEqualTo("1: b");
        assertThat(response.messages().get(1).message().get("message")).isEqualTo("1:");
        assertThat(response.messages().get(2).message().get("message")).isEqualTo(": b");
        assertThat(response.messages().get(3).message().get("message")).isEqualTo(":");
    }

    @Test
    public void exposesTimeRange() {
        final DecoratorImpl decorator = getDecoratorConfig(
                "${search.from} - ${search.to} | ${search.from_ms} - ${search.to_ms}",
                "message", false);

        final FormatStringDecorator formatStringDecorator = new FormatStringDecorator(decorator, templateEngine, messageFactory);
        final SearchResponse response = formatStringDecorator.apply(getSearchResponse());

        assertThat(response.messages().get(0).message().get("message"))
                .isEqualTo("2026-11-04T10:00:00.000Z - 2026-11-04T11:30:00.000Z | 1793786400000 - 1793791800000");
    }

    @Test
    public void formatsTimeRangeWithDateRenderer() {
        final DecoratorImpl decorator = getDecoratorConfig(
                "${search.from;date(yyyy-MM-dd HH:mm)} to ${search.to;date(HH:mm)}", "message", false);

        final FormatStringDecorator formatStringDecorator = new FormatStringDecorator(decorator, templateEngine, messageFactory);
        final SearchResponse response = formatStringDecorator.apply(getSearchResponse());

        assertThat(response.messages().get(0).message().get("message")).isEqualTo("2026-11-04 10:00 to 11:30");
    }

    @Test
    public void requireAllFieldsIgnoresTimeRangeVariables() {
        final DecoratorImpl decorator = getDecoratorConfig("${field_a} ${search.from_ms}", "message", true);

        final FormatStringDecorator formatStringDecorator = new FormatStringDecorator(decorator, templateEngine, messageFactory);
        final SearchResponse response = formatStringDecorator.apply(getSearchResponse());

        assertThat(response.messages().get(0).message().get("message")).isEqualTo("1 1793786400000");
        assertThat(response.messages().get(1).message().get("message")).isEqualTo("1 1793786400000");
        assertThat(response.messages().get(2).message().containsKey("message")).isFalse();
        assertThat(response.messages().get(3).message().containsKey("message")).isFalse();
    }

    @Test
    public void messageFieldAndSearchContextDoNotCollide() {
        final DecoratorImpl decorator = getDecoratorConfig("${search} ${search.from_ms}", "message", false);
        final SearchResponse searchResponse = getSearchResponse().toBuilder()
                .messages(ImmutableList.of(create(ImmutableMultimap.of(), ImmutableMap.of("_id", "h", "search", "from-message"), "graylog_0")))
                .build();

        final FormatStringDecorator formatStringDecorator = new FormatStringDecorator(decorator, templateEngine, messageFactory);
        final SearchResponse response = formatStringDecorator.apply(searchResponse);

        assertThat(response.messages().get(0).message().get("message")).isEqualTo("from-message 1793786400000");
        assertThat(response.messages().get(0).message().get("search")).isEqualTo("from-message");
    }

    @Test
    public void requireAllFieldsTreatsBareSearchAsMessageField() {
        final DecoratorImpl decorator = getDecoratorConfig("${search}", "message", true);

        final FormatStringDecorator formatStringDecorator = new FormatStringDecorator(decorator, templateEngine, messageFactory);
        final SearchResponse response = formatStringDecorator.apply(getSearchResponse());

        assertThat(response.messages()).allSatisfy(summary -> assertThat(summary.message()).doesNotContainKey("message"));
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
                .from(FROM)
                .to(TO).build();
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

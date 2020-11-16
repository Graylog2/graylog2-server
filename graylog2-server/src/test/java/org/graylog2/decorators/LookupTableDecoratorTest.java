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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import org.apache.commons.lang3.tuple.Pair;
import org.graylog2.lookup.LookupTableService;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.lookup.LookupResult;
import org.graylog2.rest.models.messages.responses.ResultMessageSummary;
import org.graylog2.rest.models.system.indexer.responses.IndexRangeSummary;
import org.graylog2.rest.resources.search.responses.SearchResponse;
import org.junit.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

public class LookupTableDecoratorTest {
    @Test
    public void decorate() throws Exception {
        final String sourceField = "source";
        final String targetField = "source_decorated";
        final String lookupTableName = "test";

        final Decorator decorator = createDecorator(sourceField, targetField, lookupTableName);
        final Pair<LookupTableDecorator, LookupTableService.Function> lookupTableDecoratorPair = createLookupTableDecorator(decorator);

        final LookupTableDecorator lookupTableDecorator = lookupTableDecoratorPair.getLeft();
        final LookupTableService.Function function = lookupTableDecoratorPair.getRight();

        final List<ResultMessageSummary> messages = ImmutableList.of(
                ResultMessageSummary.create(ImmutableMultimap.of(), ImmutableMap.of("_id", "a", sourceField, "0"), "graylog_0"),
                ResultMessageSummary.create(ImmutableMultimap.of(), ImmutableMap.of("_id", "b", sourceField, "1"), "graylog_0"),
                ResultMessageSummary.create(ImmutableMultimap.of(), ImmutableMap.of("_id", "c", sourceField, "2"), "graylog_0"),
                ResultMessageSummary.create(ImmutableMultimap.of(), ImmutableMap.of("_id", "d", sourceField, "3"), "graylog_0"),
                ResultMessageSummary.create(ImmutableMultimap.of(), ImmutableMap.of("_id", "e", "invalid", "4"), "graylog_0")
        );

        final SearchResponse searchResponse = createSearchResponse(messages);

        when(function.lookup("0")).thenReturn(LookupResult.single("zero"));
        when(function.lookup("1")).thenReturn(LookupResult.single("one"));
        when(function.lookup("2")).thenReturn(LookupResult.empty());
        when(function.lookup("3")).thenReturn(null);

        final SearchResponse response = lookupTableDecorator.apply(searchResponse);

        assertThat(response.messages().get(0).message().get(sourceField)).isEqualTo("0");
        assertThat(response.messages().get(0).message().get(targetField)).isEqualTo("zero");

        assertThat(response.messages().get(1).message().get(sourceField)).isEqualTo("1");
        assertThat(response.messages().get(1).message().get(targetField)).isEqualTo("one");

        assertThat(response.messages().get(2).message().get(sourceField)).isEqualTo("2");
        assertThat(response.messages().get(2).message()).doesNotContainKey(targetField);

        assertThat(response.messages().get(3).message().get(sourceField)).isEqualTo("3");
        assertThat(response.messages().get(3).message()).doesNotContainKey(targetField);

        assertThat(response.messages().get(4).message().get("invalid")).isEqualTo("4");
        assertThat(response.messages().get(4).message()).doesNotContainKey(targetField);
    }

    @Test(expected = IllegalStateException.class)
    public void withNullSourceField() throws Exception {
        createLookupTableDecorator(createDecorator("", "bar", "test"));
    }

    @Test(expected = IllegalStateException.class)
    public void withNullTargetField() throws Exception {
        createLookupTableDecorator(createDecorator("foo", "", "test"));
    }

    @Test(expected = IllegalStateException.class)
    public void withoutLookupTableName() throws Exception {
        createLookupTableDecorator(createDecorator("foo", "bar", ""));
    }

    private Decorator createDecorator(String sourceField, String targetField, String lookupTableName) {
        return DecoratorImpl.create("id",
                LookupTableDecorator.class.getCanonicalName(),
                ImmutableMap.of(
                        "source_field", sourceField,
                        "target_field", targetField,
                        "lookup_table_name", lookupTableName
                ),
                Optional.empty(),
                1);

    }

    private Pair<LookupTableDecorator, LookupTableService.Function> createLookupTableDecorator(Decorator decorator) {
        final LookupTableService lookupTableService = mock(LookupTableService.class);

        final LookupTableService.Builder builder = spy(new LookupTableService.Builder(lookupTableService));
        final LookupTableService.Function function = mock(LookupTableService.Function.class);

        when(lookupTableService.hasTable("test")).thenReturn(true);
        when(lookupTableService.newBuilder()).thenReturn(builder);
        when(builder.build()).thenReturn(function);

        final LookupTableDecorator lookupTableDecorator = new LookupTableDecorator(decorator, lookupTableService);

        return Pair.of(lookupTableDecorator, function);
    }

    private SearchResponse createSearchResponse(List<ResultMessageSummary> messages) {
        final IndexRangeSummary indexRangeSummary = IndexRangeSummary.create("graylog_0",
                Tools.nowUTC().minusDays(1),
                Tools.nowUTC(),
                null,
                100);

        return SearchResponse.builder()
                .query("foo")
                .builtQuery("foo")
                .usedIndices(ImmutableSet.of(indexRangeSummary))
                .messages(messages)
                .fields(ImmutableSet.of("source"))
                .time(100L)
                .totalResults(messages.size())
                .from(Tools.nowUTC().minusHours(1))
                .to(Tools.nowUTC())
                .build();
    }
}
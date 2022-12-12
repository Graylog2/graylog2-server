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
package org.graylog.storage.opensearch2.views.searchtypes;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.revinate.assertj.json.JsonPathAssert;
import org.graylog.plugins.views.search.LegacyDecoratorProcessor;
import org.graylog.plugins.views.search.Query;
import org.graylog.plugins.views.search.SearchType;
import org.graylog.plugins.views.search.elasticsearch.ElasticsearchQueryString;
import org.graylog.plugins.views.search.searchtypes.MessageList;
import org.graylog.plugins.views.search.searchtypes.Sort;
import org.graylog.shaded.opensearch2.org.opensearch.search.SearchHits;
import org.graylog.shaded.opensearch2.org.opensearch.search.builder.SearchSourceBuilder;
import org.graylog.storage.opensearch2.views.OSGeneratedQueryContext;
import org.graylog2.plugin.indexer.searches.timeranges.InvalidRangeParametersException;
import org.graylog2.plugin.indexer.searches.timeranges.RelativeRange;
import org.junit.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class OSMessageListTest {

    @Test
    public void includesCustomNameInResultIfPresent() {
        final OSMessageList esMessageList = new OSMessageList();
        final MessageList messageList = someMessageList().toBuilder().name("customResult").build();

        final org.graylog.shaded.opensearch2.org.opensearch.action.search.SearchResponse result =
                mock(org.graylog.shaded.opensearch2.org.opensearch.action.search.SearchResponse.class);

        when(result.getHits()).thenReturn(SearchHits.empty());

        final SearchType.Result searchTypeResult = esMessageList.doExtractResult(null, someQuery(), messageList, result, null, null);

        assertThat(searchTypeResult.name()).contains("customResult");
    }

    @Test
    public void usesHighlightingIfActivatedInConfig() {
        MessageList messageList = someMessageList();

        OSGeneratedQueryContext context = generateQueryPartWithHighlighting(messageList);

        assertThat(context.searchSourceBuilder(messageList).highlighter()).isNotNull();
    }

    @Test
    public void doesNotUseHighlightingIfDeactivatedInConfig() {
        MessageList messageList = someMessageList();

        OSGeneratedQueryContext context = generateQueryPartWithoutHighlighting(messageList);

        assertThat(context.searchSourceBuilder(messageList).highlighter())
                .as("there should be no highlighter configured")
                .isNull();
    }

    @Test
    public void passesTypeOfSortingFieldAsUnmappedType() {
        final MessageList messageList = someMessageListWithSorting("stream1", "somefield", Sort.Order.ASC);
        final OSGeneratedQueryContext context = mockQueryContext(messageList);
        when(context.fieldType(Collections.singleton("stream1"), "somefield")).thenReturn(Optional.of("long"));

        final OSGeneratedQueryContext queryContext = generateQueryPartWithContextFor(messageList, true, context);

        final DocumentContext doc = JsonPath.parse(queryContext.searchSourceBuilder(messageList).toString());
        JsonPathAssert.assertThat(doc).jsonPathAsString("$.sort[0].somefield.unmapped_type").isEqualTo("long");
    }

    @Test
    public void passesNullForUnmappedTypeIfTypeIsNotFound() {
        final MessageList messageList = someMessageListWithSorting("stream1", "somefield", Sort.Order.ASC);
        final OSGeneratedQueryContext context = mockQueryContext(messageList);
        when(context.fieldType(Collections.singleton("stream1"), "somefield")).thenReturn(Optional.empty());

        final OSGeneratedQueryContext queryContext = generateQueryPartWithContextFor(messageList, true, context);

        final DocumentContext doc = JsonPath.parse(queryContext.searchSourceBuilder(messageList).toString());
        assertThat(doc.read("$.sort[0].somefield", Map.class)).doesNotContainKey("unmapped_type");
    }

    @Test
    public void addsGl2MessageIdtoSort() {
        final MessageList messageList = someMessageListWithSorting("stream1", "timestamp", Sort.Order.ASC);
        final OSGeneratedQueryContext context = mockQueryContext(messageList);

        final OSGeneratedQueryContext queryContext = generateQueryPartWithContextFor(messageList, true, context);
        final DocumentContext doc = JsonPath.parse(queryContext.searchSourceBuilder(messageList).toString());

        JsonPathAssert.assertThat(doc).jsonPathAsString("$.sort[0].timestamp.order").isEqualTo("asc");
        JsonPathAssert.assertThat(doc).jsonPathAsString("$.sort[1].gl2_message_id.order").isEqualTo("asc");
        JsonPathAssert.assertThat(doc).jsonPathAsString("$.sort[1].gl2_message_id.unmapped_type").isEqualTo("keyword");
    }

    @Test
    public void addsGl2MessageIdtoSortWithOrder() {
        final MessageList messageList = someMessageListWithSorting("stream1", "timestamp", Sort.Order.DESC);
        final OSGeneratedQueryContext context = mockQueryContext(messageList);

        final OSGeneratedQueryContext queryContext = generateQueryPartWithContextFor(messageList, true, context);
        final DocumentContext doc = JsonPath.parse(queryContext.searchSourceBuilder(messageList).toString());

        JsonPathAssert.assertThat(doc).jsonPathAsString("$.sort[0].timestamp.order").isEqualTo("desc");
        JsonPathAssert.assertThat(doc).jsonPathAsString("$.sort[1].gl2_message_id.order").isEqualTo("desc");
    }

    @Test
    public void onlyAddsGl2MessageIdWhenNotAlreadyPresent() {
        final MessageList messageList = someMessageList().toBuilder()
                .sort(List.of(
                        Sort.create("gl2_message_id", Sort.Order.DESC),
                        Sort.create("timestamp", Sort.Order.ASC)
                ))
                .build();
        final OSGeneratedQueryContext context = mockQueryContext(messageList);
        final OSGeneratedQueryContext queryContext = generateQueryPartWithContextFor(messageList, true, context);
        final DocumentContext doc = JsonPath.parse(queryContext.searchSourceBuilder(messageList).toString());

        assertThat((List<?>) doc.read("$.sort")).hasSize(2);
        JsonPathAssert.assertThat(doc).jsonPathAsString("$.sort[0].gl2_message_id.order").isEqualTo("desc");
        JsonPathAssert.assertThat(doc).jsonPathAsString("$.sort[1].timestamp.order").isEqualTo("asc");
    }

    private Query someQuery() {
        return Query.builder()
                .id("deadbeef")
                .query(ElasticsearchQueryString.of("Something else"))
                .timerange(someTimeRange())
                .searchTypes(Collections.emptySet())
                .build();
    }

    private RelativeRange someTimeRange() {
        try {
            return RelativeRange.create(300);
        } catch (InvalidRangeParametersException e) {
            throw new RuntimeException("invalid query time range spec");
        }
    }

    private MessageList someMessageList() {
        return MessageList.builder()
                .id("amessagelist")
                .limit(100)
                .offset(0)
                .build();
    }

    private MessageList someMessageListWithSorting(String stream, String sortField, Sort.Order order) {
        return MessageList.builder()
                .id("amessagelist")
                .limit(100)
                .offset(0)
                .streams(Collections.singleton(stream))
                .sort(Collections.singletonList(Sort.create(sortField, order)))
                .build();
    }

    private OSGeneratedQueryContext generateQueryPartWithHighlighting(MessageList messageList) {
        return generateQueryPartFor(messageList, true);
    }

    private OSGeneratedQueryContext generateQueryPartWithoutHighlighting(MessageList messageList) {
        return generateQueryPartFor(messageList, false);
    }

    private OSGeneratedQueryContext generateQueryPartFor(MessageList messageList, boolean allowHighlighting) {
        final OSGeneratedQueryContext context = mockQueryContext(messageList);

        return generateQueryPartWithContextFor(messageList, allowHighlighting, context);
    }

    private OSGeneratedQueryContext mockQueryContext(MessageList messageList) {
        OSGeneratedQueryContext context = mock(OSGeneratedQueryContext.class);

        when(context.searchSourceBuilder(messageList)).thenReturn(new SearchSourceBuilder());

        return context;
    }

    private OSGeneratedQueryContext generateQueryPartWithContextFor(MessageList messageList,
                                                                    boolean allowHighlighting,
                                                                    OSGeneratedQueryContext context) {
        OSMessageList sut = new OSMessageList(
                new LegacyDecoratorProcessor.Fake(),
                allowHighlighting);

        sut.doGenerateQueryPart(someQuery(), messageList, context);

        return context;
    }
}

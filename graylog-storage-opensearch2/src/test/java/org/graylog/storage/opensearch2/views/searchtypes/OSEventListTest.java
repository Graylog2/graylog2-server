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
import org.graylog.plugins.views.search.Query;
import org.graylog.plugins.views.search.elasticsearch.ElasticsearchQueryString;
import org.graylog.plugins.views.search.filter.StreamFilter;
import org.graylog.plugins.views.search.searchtypes.events.EventList;
import org.graylog.shaded.opensearch2.org.opensearch.index.query.QueryBuilders;
import org.graylog.shaded.opensearch2.org.opensearch.search.builder.SearchSourceBuilder;
import org.graylog.storage.opensearch2.views.OSGeneratedQueryContext;
import org.graylog2.plugin.indexer.searches.timeranges.RelativeRange;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.graylog2.plugin.streams.Stream.DEFAULT_EVENTS_STREAM_ID;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OSEventListTest {
    @Test
    void filtersBySourceStreamsWhenSelectionContainsSourceStreams() throws Exception {
        final DocumentContext doc = generateQuery(EventList.builder().build(), DEFAULT_EVENTS_STREAM_ID, "source-stream");

        assertThat(doc.read("$.query.bool.must[*].terms.source_streams[*]", List.class)).containsExactlyInAnyOrder(DEFAULT_EVENTS_STREAM_ID, "source-stream");
        assertThat(doc.read("$.query.bool.must[*].terms.streams", List.class)).isEmpty();
    }

    @Test
    void filtersByEventStreamsAndPermittedSourceStreamsWhenSelectionHasOnlyEventStreams() throws Exception {
        final EventList eventList = EventList.builder().permittedSourceStreams(Set.of("readable")).build();

        final DocumentContext doc = generateQuery(eventList, DEFAULT_EVENTS_STREAM_ID);

        assertThat(doc.read("$.query.bool.must[*].terms.streams", List.class)).containsExactly(List.of(DEFAULT_EVENTS_STREAM_ID));
        assertThat(doc.read("$.query.bool.must[*].terms.source_streams", List.class)).containsExactly(List.of("readable"));
    }

    private DocumentContext generateQuery(EventList eventList, String... streamIds) throws Exception {
        final Query query = Query.builder()
                .id("query")
                .query(ElasticsearchQueryString.empty())
                .timerange(RelativeRange.create(300))
                .filter(StreamFilter.anyIdOf(streamIds))
                .build();
        final SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder().query(QueryBuilders.boolQuery());
        final OSGeneratedQueryContext context = mock(OSGeneratedQueryContext.class);
        when(context.searchSourceBuilder(eventList)).thenReturn(searchSourceBuilder);

        new OSEventList(new ObjectMapperProvider().get()).doGenerateQueryPart(query, eventList, context);

        return JsonPath.parse(searchSourceBuilder.toString());
    }
}

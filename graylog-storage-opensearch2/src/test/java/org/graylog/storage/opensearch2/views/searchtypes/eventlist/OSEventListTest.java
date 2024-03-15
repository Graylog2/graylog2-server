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
package org.graylog.storage.opensearch2.views.searchtypes.eventlist;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.graylog.events.event.EventDto;
import org.graylog.plugins.views.search.Query;
import org.graylog.plugins.views.search.SearchJob;
import org.graylog.plugins.views.search.searchtypes.events.CommonEventSummary;
import org.graylog.plugins.views.search.searchtypes.events.EventList;
import org.graylog.plugins.views.search.searchtypes.events.EventSummary;
import org.graylog.shaded.opensearch2.org.apache.lucene.search.TotalHits;
import org.graylog.shaded.opensearch2.org.opensearch.action.search.SearchResponse;
import org.graylog.shaded.opensearch2.org.opensearch.search.aggregations.Aggregations;
import org.graylog.storage.opensearch2.views.OSGeneratedQueryContext;
import org.graylog.storage.opensearch2.views.searchtypes.OSEventList;
import org.graylog2.plugin.Tools;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.junit.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class OSEventListTest {

    @Test
    public void testSortingOfStreamsInDoExtractResult() {
        final OSEventList esEventList = new TestOSEventList();
        final SearchJob searchJob = mock(SearchJob.class);
        final Query query = mock(Query.class);
        final SearchResponse searchResult = mock(SearchResponse.class, RETURNS_DEEP_STUBS);
        final Aggregations metricAggregation = mock(Aggregations.class);
        final OSGeneratedQueryContext queryContext = mock(OSGeneratedQueryContext.class);
        when(searchResult.getHits().getTotalHits()).thenReturn(new TotalHits(1000, TotalHits.Relation.EQUAL_TO));

        final EventList eventList = EventList.builder()
                .id("search-type-id")
                .streams(ImmutableSet.of("stream-id-1", "stream-id-2"))
                .build();
        final EventList.Result eventResult = (EventList.Result) esEventList.doExtractResult(searchJob, query, eventList, searchResult,
                metricAggregation, queryContext);
        assertThat(stripRawEvents(eventResult.events())).containsExactly(
                eventSummary("find-1", ImmutableSet.of("stream-id-1")),
                eventSummary("find-2", ImmutableSet.of("stream-id-2")),
                eventSummary("find-3", ImmutableSet.of("stream-id-1", "stream-id-2"))
        );
    }

    private List<EventSummary> stripRawEvents(List<CommonEventSummary> events) {
        return events.stream()
                .map(event -> (EventSummary) event)
                .map(event -> event.toBuilder().rawEvent(null).build())
                .toList();
    }

    final private static DateTimeFormatter formatter = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss");
    final private static DateTime timestamp = formatter.parseDateTime("2019-03-30 14:00:00");

    private EventSummary eventSummary(String id, Set<String> streams) {
        return EventSummary.builder()
                .id(id)
                .message("message")
                .streams(streams)
                .timestamp(DateTime.parse(timestamp.toString(Tools.ES_DATE_FORMAT_FORMATTER), Tools.ES_DATE_FORMAT_FORMATTER))
                .alert(false)
                .eventDefinitionId("deadbeef")
                .priority(2L)
                .eventKeys(List.of())
                .build();
    }

    static class TestOSEventList extends OSEventList {
        public TestOSEventList() {
            super(new ObjectMapperProvider().get());
        }

        private Map<String, Object> hit(String id, List<String> streams) {
            return ImmutableMap.<String, Object>builder()
                    .put(EventDto.FIELD_ID, id)
                    .put(EventDto.FIELD_MESSAGE, "message")
                    .put(EventDto.FIELD_SOURCE_STREAMS, streams)
                    .put(EventDto.FIELD_EVENT_TIMESTAMP, timestamp.toString(Tools.ES_DATE_FORMAT_FORMATTER))
                    .put(EventDto.FIELD_EVENT_DEFINITION_ID, "deadbeef")
                    .put(EventDto.FIELD_ALERT, false)
                    .put(EventDto.FIELD_PRIORITY, 2)
                    .put(EventDto.FIELD_KEY_TUPLE, List.of())
                    .put(EventDto.FIELD_EVENT_DEFINITION_TYPE, "aggregation-v1")
                    .put(EventDto.FIELD_PROCESSING_TIMESTAMP, timestamp.toString(Tools.ES_DATE_FORMAT_FORMATTER))
                    .put(EventDto.FIELD_STREAMS, List.of())
                    .put(EventDto.FIELD_SOURCE, "localhost")
                    .put(EventDto.FIELD_FIELDS, Map.of())
                    .build();
        }

        @Override
        protected List<Map<String, Object>> extractResult(SearchResponse result) {
            return ImmutableList.of(
                    hit("find-1", List.of("stream-id-1")),
                    hit("find-2", List.of("stream-id-2")),
                    hit("find-3", List.of("stream-id-1", "stream-id-2")),
                    hit("do-not-find-1", List.of("stream-id-3")),
                    hit("do-not-find-2", List.of("stream-id-1", "stream-id-3")),
                    hit("do-not-find-3", List.of("stream-id-2", "stream-id-3")),
                    hit("do-not-find-4", List.of("stream-id-1", "stream-id-2", "stream-id-3"))
            );
        }
    }
}

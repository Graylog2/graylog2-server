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
package org.graylog.events.search;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.shiro.subject.Subject;
import org.graylog.events.processor.DBEventDefinitionService;
import org.graylog.plugins.views.search.permissions.SearchUser;
import org.graylog.plugins.views.search.rest.scriptingapi.ScriptingApiService;
import org.graylog2.plugin.indexer.searches.timeranges.RelativeRange;
import org.graylog2.rest.resources.entities.Slice;
import org.graylog2.rest.resources.entities.Slices;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.graylog2.streams.StreamService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.graylog.events.event.EventDto.FIELD_ALERT;
import static org.graylog.events.event.EventDto.FIELD_EVENT_DEFINITION_ID;
import static org.graylog.events.event.EventDto.FIELD_EVENT_DEFINITION_TYPE;
import static org.graylog.events.event.EventDto.FIELD_KEY;
import static org.graylog.events.event.EventDto.FIELD_PRIORITY;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventsSearchServiceSlicesTest {

    @Mock
    private StreamService streamService;

    @Mock
    private DBEventDefinitionService eventDefinitionService;

    @Mock
    private ScriptingApiService scriptingApiService;

    @Mock
    private MoreSearch moreSearch;

    private EventsSearchService service;
    private Subject mockSubject;
    private SearchUser mockSearchUser;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        // Setup object mapper
        objectMapper = new ObjectMapperProvider().get();

        // Setup service
        service = new EventsSearchService(moreSearch, streamService,
            eventDefinitionService, scriptingApiService, objectMapper);

        // Setup mock user permissions - allow all streams
        mockSubject = mock(Subject.class);
        when(mockSubject.isPermitted(any(String.class))).thenReturn(true);

        mockSearchUser = mock(SearchUser.class);
    }

    @Test
    void testSlicesByPriority_AllLevelsPresent() throws Exception {
        // Mock ScriptingApiService to return aggregation results for priority
        mockAggregationResponse(FIELD_PRIORITY, Map.of(
            "1", 5L,
            "2", 8L,
            "3", 4L,
            "4", 3L
        ));

        EventsSlicesRequest request = EventsSlicesRequest.builder()
            .query("")
            .sliceColumn(FIELD_PRIORITY)
            .includeAll(false)
            .timerange(RelativeRange.create(86400))
            .filter(EventsSearchFilter.empty())
            .build();

        Slices result = service.slices(request, mockSubject, mockSearchUser);

        // Assert all 4 priority levels present
        assertThat(result.slices())
            .hasSize(4)
            .extracting(Slice::value)
            .containsExactlyInAnyOrder("1", "2", "3", "4");

        // Assert counts match fixture data
        assertSliceCount(result.slices(), "1", 5);
        assertSliceCount(result.slices(), "2", 8);
        assertSliceCount(result.slices(), "3", 4);
        assertSliceCount(result.slices(), "4", 3);
    }

    @Test
    void testSlicesByPriority_WithQuery() throws Exception {
        // Mock filtered results with query
        mockAggregationResponse(FIELD_PRIORITY, Map.of(
            "1", 3L,
            "2", 5L,
            "3", 2L,
            "4", 1L
        ));

        EventsSlicesRequest request = EventsSlicesRequest.builder()
            .query("source:test")
            .sliceColumn(FIELD_PRIORITY)
            .includeAll(false)
            .timerange(RelativeRange.create(86400))
            .filter(EventsSearchFilter.empty())
            .build();

        Slices result = service.slices(request, mockSubject, mockSearchUser);

        // Total count should be less than unfiltered
        int totalCount = result.slices().stream()
            .mapToInt(Slice::count)
            .sum();
        assertThat(totalCount).isEqualTo(11);
    }

    @Test
    void testSlicesByAlert_TrueFalsePresent() throws Exception {
        // Mock alert aggregation
        mockAggregationResponse(FIELD_ALERT, Map.of(
            "true", 12L,
            "false", 8L
        ));

        EventsSlicesRequest request = EventsSlicesRequest.builder()
            .query("")
            .sliceColumn(FIELD_ALERT)
            .includeAll(false)
            .timerange(RelativeRange.create(86400))
            .filter(EventsSearchFilter.empty())
            .build();

        Slices result = service.slices(request, mockSubject, mockSearchUser);

        // Verify both true and false values are present
        assertThat(result.slices())
            .hasSize(2)
            .extracting(Slice::value)
            .containsExactlyInAnyOrder("true", "false");

        assertSliceCount(result.slices(), "true", 12);
        assertSliceCount(result.slices(), "false", 8);
    }

    @Test
    void testSlicesByEventDefinitionId() throws Exception {
        // Mock event definition ID aggregation
        mockAggregationResponse(FIELD_EVENT_DEFINITION_ID, Map.of(
            "def-001", 9L,
            "def-002", 7L,
            "def-003", 4L
        ));

        EventsSlicesRequest request = EventsSlicesRequest.builder()
            .query("")
            .sliceColumn(FIELD_EVENT_DEFINITION_ID)
            .includeAll(false)
            .timerange(RelativeRange.create(86400))
            .filter(EventsSearchFilter.empty())
            .build();

        Slices result = service.slices(request, mockSubject, mockSearchUser);

        assertThat(result.slices())
            .hasSize(3)
            .extracting(Slice::value)
            .containsExactlyInAnyOrder("def-001", "def-002", "def-003");

        assertSliceCount(result.slices(), "def-001", 9);
        assertSliceCount(result.slices(), "def-002", 7);
        assertSliceCount(result.slices(), "def-003", 4);
    }

    @Test
    void testSlicesByEventDefinitionType() throws Exception {
        // Mock event definition type aggregation
        mockAggregationResponse(FIELD_EVENT_DEFINITION_TYPE, Map.of(
            "aggregation", 10L,
            "correlation", 6L,
            "notification", 4L
        ));

        EventsSlicesRequest request = EventsSlicesRequest.builder()
            .query("")
            .sliceColumn(FIELD_EVENT_DEFINITION_TYPE)
            .includeAll(false)
            .timerange(RelativeRange.create(86400))
            .filter(EventsSearchFilter.empty())
            .build();

        Slices result = service.slices(request, mockSubject, mockSearchUser);

        assertThat(result.slices())
            .hasSize(3)
            .extracting(Slice::value)
            .containsExactlyInAnyOrder("aggregation", "correlation", "notification");
    }

    @Test
    void testSlicesByKey_IncludeAll_True() throws Exception {
        // Mock key aggregation with "(Empty Value)" for missing keys
        mockAggregationResponse(FIELD_KEY, Map.of(
            "key-001", 1L,
            "key-002", 1L,
            "key-003", 1L,
            "key-004", 1L,
            "key-005", 1L,
            "key-006", 1L,
            "(Empty Value)", 14L
        ));

        EventsSlicesRequest request = EventsSlicesRequest.builder()
            .query("")
            .sliceColumn(FIELD_KEY)
            .includeAll(true)
            .timerange(RelativeRange.allTime())  // includeAll uses all time
            .filter(EventsSearchFilter.empty())
            .build();

        Slices result = service.slices(request, mockSubject, mockSearchUser);

        // Verify empty value slice is included
        assertThat(result.slices())
            .anySatisfy(slice -> {
                assertThat(slice.value()).isEqualTo("(Empty Value)");
                assertThat(slice.count()).isEqualTo(14);
            });
    }

    @Test
    void testSlicesByKey_IncludeAll_False() throws Exception {
        // Mock key aggregation without empty values
        mockAggregationResponse(FIELD_KEY, Map.of(
            "key-001", 1L,
            "key-002", 1L,
            "key-003", 1L,
            "key-004", 1L,
            "key-005", 1L,
            "key-006", 1L
        ));

        EventsSlicesRequest request = EventsSlicesRequest.builder()
            .query("")
            .sliceColumn(FIELD_KEY)
            .includeAll(false)
            .timerange(RelativeRange.create(86400))
            .filter(EventsSearchFilter.empty())
            .build();

        Slices result = service.slices(request, mockSubject, mockSearchUser);

        // Verify NO empty value slice
        assertThat(result.slices())
            .noneSatisfy(slice ->
                assertThat(slice.value()).isEqualTo("(Empty Value)"));
    }

    @Test
    void testSlicesWithEmptyQuery() throws Exception {
        // Mock full dataset
        mockAggregationResponse(FIELD_PRIORITY, Map.of(
            "1", 5L,
            "2", 8L,
            "3", 4L,
            "4", 3L
        ));

        EventsSlicesRequest request = EventsSlicesRequest.builder()
            .query("")
            .sliceColumn(FIELD_PRIORITY)
            .includeAll(false)
            .timerange(RelativeRange.create(86400))
            .filter(EventsSearchFilter.empty())
            .build();

        Slices result = service.slices(request, mockSubject, mockSearchUser);

        int totalCount = result.slices().stream()
            .mapToInt(Slice::count)
            .sum();
        assertThat(totalCount).isEqualTo(20);
    }

    // Helper methods

    private void mockAggregationResponse(String field, Map<String, Long> counts) throws Exception {
        List<List<Object>> datarows = new ArrayList<>();
        counts.forEach((value, count) -> {
            datarows.add(List.of(value, count));
        });

        org.graylog.plugins.views.search.rest.scriptingapi.response.ResponseSchemaEntry fieldSchema =
            org.graylog.plugins.views.search.rest.scriptingapi.response.ResponseSchemaEntry.groupBy(field);
        org.graylog.plugins.views.search.rest.scriptingapi.response.ResponseSchemaEntry countSchema =
            org.graylog.plugins.views.search.rest.scriptingapi.response.ResponseSchemaEntry.metric("count", null);

        org.graylog2.plugin.indexer.searches.timeranges.AbsoluteRange timeRange =
            org.graylog2.plugin.indexer.searches.timeranges.AbsoluteRange.create(
                "2024-01-01T00:00:00.000Z", "2024-01-02T00:00:00.000Z");

        org.graylog.plugins.views.search.rest.scriptingapi.response.Metadata metadata =
            new org.graylog.plugins.views.search.rest.scriptingapi.response.Metadata(timeRange);

        org.graylog.plugins.views.search.rest.scriptingapi.response.TabularResponse response =
            new org.graylog.plugins.views.search.rest.scriptingapi.response.TabularResponse(
                List.of(fieldSchema, countSchema),
                datarows,
                metadata
            );

        when(scriptingApiService.executeAggregation(any(), any()))
            .thenReturn(response);
    }

    private void assertSliceCount(List<Slice> slices, String value, int expectedCount) {
        assertThat(slices)
            .anySatisfy(slice -> {
                assertThat(slice.value()).isEqualTo(value);
                assertThat(slice.count()).isEqualTo(expectedCount);
            });
    }
}

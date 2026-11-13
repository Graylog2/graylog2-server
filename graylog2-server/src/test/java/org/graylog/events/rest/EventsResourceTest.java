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
package org.graylog.events.rest;

import jakarta.ws.rs.NotFoundException;
import org.apache.shiro.subject.Subject;
import org.graylog.events.event.EventDto;
import org.graylog.events.search.EventsSearchParameters;
import org.graylog.events.search.EventsSearchResult;
import org.graylog.events.search.EventsSearchService;
import org.graylog.events.search.EventsSliceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventsResourceTest {
    private static final String EVENT_ID = "01M788KWTZH0J8919DTRBZG2JM";

    @Mock
    private EventsSearchService searchService;
    @Mock
    private EventsSliceService sliceService;

    private Subject subject;
    private EventsResource resource;

    @BeforeEach
    void setUp() {
        subject = mock(Subject.class);
        resource = new TestEventsResource(subject);
    }

    @Test
    void getByIdReturnsTheEvent() {
        final var event = EventsSearchResult.Event.create(mock(EventDto.class), "gl-events_0", "message");
        when(searchService.searchByIds(List.of(EVENT_ID), subject)).thenReturn(searchResult(List.of(event)));

        assertThat(resource.getById(EVENT_ID)).isEqualTo(event);
    }

    @Test
    void getByIdThrowsNotFoundExceptionForMissingEvent() {
        when(searchService.searchByIds(List.of(EVENT_ID), subject)).thenReturn(searchResult(List.of()));

        assertThatThrownBy(() -> resource.getById(EVENT_ID))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Event <" + EVENT_ID + "> doesn't exist");
    }

    private static EventsSearchResult searchResult(List<EventsSearchResult.Event> events) {
        return EventsSearchResult.builder()
                .parameters(EventsSearchParameters.empty())
                .events(events)
                .usedIndices(Set.of())
                .totalEvents(events.size())
                .duration(0)
                .context(EventsSearchResult.Context.create(Map.of(), Map.of()))
                .build();
    }

    private class TestEventsResource extends EventsResource {
        private final Subject subject;

        TestEventsResource(Subject subject) {
            super(searchService, sliceService);
            this.subject = subject;
        }

        @Override
        protected Subject getSubject() {
            return subject;
        }
    }
}

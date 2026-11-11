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
package org.graylog.plugins.views.search.searchtypes.eventlist;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import com.google.common.collect.ImmutableSet;
import org.graylog.events.search.SourceStreamFilter;
import org.graylog.plugins.views.search.Query;
import org.graylog.plugins.views.search.filter.StreamFilter;
import org.graylog.plugins.views.search.searchtypes.events.EventList;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.graylog.plugins.views.search.TestData.validQueryBuilder;
import static org.graylog2.plugin.streams.Stream.DEFAULT_EVENTS_STREAM_ID;
import static org.graylog2.plugin.streams.Stream.DEFAULT_SYSTEM_EVENTS_STREAM_ID;

public class EventListTest {

    @Test
    public void testEffectiveStream() {
        final EventList eventList = EventList.builder()
                .streams(ImmutableSet.of("dead-beef", "1337-beef"))
                .build();
        assertThat(eventList.effectiveStreams()).isEqualTo(
                ImmutableSet.of(DEFAULT_EVENTS_STREAM_ID, DEFAULT_SYSTEM_EVENTS_STREAM_ID)
        );
        assertThat(eventList.streams()).isEqualTo(ImmutableSet.of("dead-beef", "1337-beef"));
    }

    @Test
    void sourceStreamsInSelectionFilterSourceStreamsAndIgnoreEventStreams() {
        final EventList eventList = EventList.builder().build();
        final Query query = queryWithStreams(DEFAULT_SYSTEM_EVENTS_STREAM_ID, "source-stream");

        assertThat(eventList.streamSelection(query).isEventStreamsOnly()).isFalse();
        assertThat(eventList.sourceStreamFilter(query)).isEqualTo(SourceStreamFilter.allowList(Set.of("source-stream")));
    }

    @Test
    void eventStreamsOnlySelectionHasNoSourceStreamFilter() {
        final EventList eventList = EventList.builder().build();
        final Query query = queryWithStreams(DEFAULT_EVENTS_STREAM_ID);

        assertThat(eventList.streamSelection(query).isEventStreamsOnly()).isTrue();
        assertThat(eventList.streamSelection(query).eventStreams()).containsExactly(DEFAULT_EVENTS_STREAM_ID);
        assertThat(eventList.sourceStreamFilter(query)).isEqualTo(SourceStreamFilter.allAllowed());
    }

    @Test
    void permittedSourceStreamsRestrictEventStreamsOnlySelection() {
        final Query query = queryWithStreams(DEFAULT_EVENTS_STREAM_ID);

        assertThat(EventList.builder().permittedSourceStreams(Set.of("readable")).build().sourceStreamFilter(query))
                .isEqualTo(SourceStreamFilter.allowList(Set.of("readable")));
        assertThat(EventList.builder().permittedSourceStreams(Set.of()).build().sourceStreamFilter(query))
                .isEqualTo(SourceStreamFilter.allowList(Set.of()));
    }

    @Test
    void searchTypeStreamsTakePrecedenceOverQueryStreams() {
        final EventList eventList = EventList.builder().streams(Set.of(DEFAULT_EVENTS_STREAM_ID)).build();
        final Query query = queryWithStreams("source-stream");

        assertThat(eventList.streamSelection(query).isEventStreamsOnly()).isTrue();
        assertThat(eventList.sourceStreamFilter(query)).isEqualTo(SourceStreamFilter.allAllowed());
    }

    @Test
    void permittedSourceStreamsAreNotPartOfTheJsonRepresentation() throws Exception {
        final ObjectMapper objectMapper = new ObjectMapperProvider().get();
        final EventList eventList = EventList.builder().id("event-list").permittedSourceStreams(Set.of("readable")).build();

        final String json = objectMapper.writeValueAsString(eventList);
        assertThat(json).doesNotContain("readable");

        final String injected = json.replaceFirst("\\{", "{\"permitted_source_streams\":[\"injected\"],");
        assertThatThrownBy(() -> objectMapper.readValue(injected, EventList.class))
                .isInstanceOf(UnrecognizedPropertyException.class);
    }

    private Query queryWithStreams(String... streamIds) {
        return validQueryBuilder().filter(StreamFilter.anyIdOf(streamIds)).build();
    }
}

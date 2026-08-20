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

import org.graylog.events.event.EventDto;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class EventsFilterBuilderTest {

    private static final String EVENT_DEFINITION_ID_CLAUSE = "(_exists_:event_definition_id)";

    private String build(EventsSearchFilter filter) {
        return new EventsFilterBuilder(EventsSearchParameters.builder().filter(filter).build()).build();
    }

    @Test
    void buildWithSingleTagFilter() {
        final var filter = EventsSearchFilter.builder()
                .extraFilters(Map.of(EventDto.FIELD_TAGS, Set.of("phishing")))
                .build();

        assertThat(build(filter)).isEqualTo(EVENT_DEFINITION_ID_CLAUSE + " AND (tags:\"phishing\")");
    }

    @Test
    void buildWithMultipleTagFiltersJoinsWithAND() {
        // LinkedHashSet keeps insertion order so the assertion against the full query string is
        // deterministic — EventsFilterBuilder iterates the tag set as-is.
        final var filter = EventsSearchFilter.builder()
                .extraFilters(Map.of(EventDto.FIELD_TAGS, new LinkedHashSet<>(List.of("phishing", "exfil"))))
                .build();

        assertThat(build(filter)).isEqualTo(EVENT_DEFINITION_ID_CLAUSE + " AND (tags:\"phishing\" AND tags:\"exfil\")");
    }

    @Test
    void buildWithoutTagFilterDoesNotEmitTagsClause() {
        final var filter = EventsSearchFilter.empty();

        assertThat(build(filter)).isEqualTo(EVENT_DEFINITION_ID_CLAUSE);
    }

    @Test
    void buildWithEmptyTagSetDoesNotEmitTagsClause() {
        final var filter = EventsSearchFilter.builder()
                .extraFilters(Map.of(EventDto.FIELD_TAGS, Set.of()))
                .build();

        assertThat(build(filter)).isEqualTo(EVENT_DEFINITION_ID_CLAUSE);
    }

    @Test
    void buildEscapesLuceneMetaCharactersInTagFilter() {
        final var filter = EventsSearchFilter.builder()
                .extraFilters(Map.of(EventDto.FIELD_TAGS, Set.of("phish\"ing OR alert:true")))
                .build();

        assertThat(build(filter)).isEqualTo(EVENT_DEFINITION_ID_CLAUSE + " AND (tags:\"phish\\\"ing OR alert\\:true\")");
    }

}

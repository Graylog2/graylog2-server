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

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class EventsFilterBuilderTest {

    private String build(EventsSearchFilter filter) {
        return new EventsFilterBuilder(EventsSearchParameters.builder().filter(filter).build()).build();
    }

    @Test
    void buildWithSingleTagFilter() {
        final var filter = EventsSearchFilter.builder()
                .extraFilters(Map.of(EventDto.FIELD_TAGS, Set.of("phishing")))
                .build();

        assertThat(build(filter)).contains("(tags:\"phishing\")");
    }

    @Test
    void buildWithMultipleTagFiltersJoinsWithOR() {
        final var filter = EventsSearchFilter.builder()
                .extraFilters(Map.of(EventDto.FIELD_TAGS, new java.util.LinkedHashSet<>(java.util.List.of("phishing", "exfil"))))
                .build();

        final var query = build(filter);
        assertThat(query).contains("tags:\"phishing\"");
        assertThat(query).contains("tags:\"exfil\"");
        assertThat(query).contains(" OR ");
    }

    @Test
    void buildWithNullValueTagUsesNotExists() {
        final var filter = EventsSearchFilter.builder()
                .extraFilters(Map.of(EventDto.FIELD_TAGS, Set.of(EventsSearchFilter.NULL_VALUE)))
                .build();

        assertThat(build(filter)).contains("(NOT _exists_:tags)");
    }

    @Test
    void buildWithoutTagFilterDoesNotEmitTagsClause() {
        final var filter = EventsSearchFilter.empty();

        assertThat(build(filter)).doesNotContain("tags:");
    }

    @Test
    void buildWithEmptyTagSetDoesNotEmitTagsClause() {
        final var filter = EventsSearchFilter.builder()
                .extraFilters(Map.of(EventDto.FIELD_TAGS, Set.of()))
                .build();

        assertThat(build(filter)).doesNotContain("tags:");
    }

    @Test
    void buildEscapesLuceneMetaCharactersInTagFilter() {
        final var filter = EventsSearchFilter.builder()
                .extraFilters(Map.of(EventDto.FIELD_TAGS, Set.of("phish\"ing OR alert:true")))
                .build();

        final var query = build(filter);
        assertThat(query).doesNotContain("OR alert:true\"");
        // Positive assertion: full escaped value appears once inside a quoted clause.
        assertThat(query).contains("tags:\"phish\\\"ing OR alert\\:true\"");
    }

    @Test
    void buildCombinesNullValueWithConcreteTagsUsingOR() {
        final var filter = EventsSearchFilter.builder()
                .extraFilters(Map.of(EventDto.FIELD_TAGS,
                        new java.util.LinkedHashSet<>(java.util.List.of(EventsSearchFilter.NULL_VALUE, "phishing"))))
                .build();

        final var query = build(filter);
        assertThat(query).contains("NOT _exists_:tags");
        assertThat(query).contains("tags:\"phishing\"");
        assertThat(query).contains(" OR ");
    }
}

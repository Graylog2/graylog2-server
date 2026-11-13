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
package org.graylog.plugins.views.search.engine.normalization;

import com.google.common.collect.ImmutableSet;
import org.graylog.plugins.views.search.Query;
import org.graylog.plugins.views.search.Search;
import org.graylog.plugins.views.search.filter.StreamFilter;
import org.graylog.plugins.views.search.permissions.SearchUser;
import org.graylog.plugins.views.search.rest.ExecutionState;
import org.graylog.plugins.views.search.rest.TestSearchUser;
import org.graylog.plugins.views.search.searchtypes.events.EventList;
import org.graylog2.plugin.indexer.searches.timeranges.InvalidRangeParametersException;
import org.graylog2.plugin.indexer.searches.timeranges.RelativeRange;
import org.graylog2.streams.StreamService;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.graylog.plugins.views.search.TestData.validQueryBuilder;
import static org.graylog2.plugin.streams.Stream.DEFAULT_EVENTS_STREAM_ID;
import static org.mockito.Mockito.mock;

class PluggableSearchNormalizationTest {
    private final PluggableSearchNormalization normalization = new PluggableSearchNormalization(Set.of(), mock(StreamService.class));

    @Test
    void restrictsEventStreamsOnlyEventListsToReadableStreamsForRestrictedUsers() throws Exception {
        final SearchUser searchUser = TestSearchUser.builder()
                .allowStream(DEFAULT_EVENTS_STREAM_ID)
                .allowStream("readable")
                .denyStream("unreadable")
                .build();

        assertThat(permittedSourceStreams(normalization.preValidation(eventStreamsOnlySearch(), searchUser, ExecutionState.empty())))
                .contains(Set.of(DEFAULT_EVENTS_STREAM_ID, "readable"));
    }

    @Test
    void leavesEventListsUnrestrictedForUsersWithBlanketStreamReadPermission() throws Exception {
        final SearchUser searchUser = TestSearchUser.builder().allowAllStreams().build();

        assertThat(permittedSourceStreams(normalization.preValidation(eventStreamsOnlySearch(), searchUser, ExecutionState.empty())))
                .isEmpty();
    }

    private Search eventStreamsOnlySearch() throws InvalidRangeParametersException {
        final Query query = validQueryBuilder()
                .timerange(RelativeRange.create(300))
                .filter(StreamFilter.anyIdOf(DEFAULT_EVENTS_STREAM_ID))
                .searchTypes(Set.of(EventList.builder().id("events").build()))
                .build();
        return Search.builder().queries(ImmutableSet.of(query)).build();
    }

    private Optional<Set<String>> permittedSourceStreams(Search search) {
        return ((EventList) search.queries().iterator().next().searchTypes().iterator().next()).permittedSourceStreams();
    }
}

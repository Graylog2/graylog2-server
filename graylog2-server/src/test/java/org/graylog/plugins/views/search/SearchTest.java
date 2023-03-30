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
package org.graylog.plugins.views.search;

import com.google.common.collect.ImmutableSet;
import org.graylog.plugins.views.search.filter.StreamFilter;
import org.graylog.plugins.views.search.rest.ExecutionState;
import org.graylog.plugins.views.search.rest.ExecutionStateGlobalOverride;
import org.graylog.plugins.views.search.searchtypes.MessageList;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.graylog2.shared.rest.exceptions.MissingStreamPermissionException;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.graylog.plugins.views.search.TestData.validQueryBuilder;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SearchTest {

    @Test
    void addsDefaultStreamsToQueriesWithoutStreams() {
        Search before = searchWithQueriesWithStreams("");

        Search after = before.addStreamsToQueriesWithoutStreams(() -> ImmutableSet.of("one", "two", "three"));

        assertThat(after.queries().asList().get(0).usedStreamIds()).containsExactlyInAnyOrder("one", "two", "three");
    }

    @Test
    void leavesQueriesWithDefinedStreamsUntouched() {
        Search before = searchWithQueriesWithStreams("a,b,c", "");

        Search after = before.addStreamsToQueriesWithoutStreams(() -> ImmutableSet.of("one", "two", "three"));

        assertThat(after.queries().asList().get(0).usedStreamIds()).containsExactlyInAnyOrder("a", "b", "c");
    }

    @Test
    void doesNothingIfAllQueriesHaveDefinedStreams() {
        Search before = searchWithQueriesWithStreams("a,b,c", "a,d,f");

        Search after = before.addStreamsToQueriesWithoutStreams(() -> ImmutableSet.of("one", "two", "three"));

        assertThat(before).isEqualTo(after);
    }

    @Test
    void throwsExceptionIfQueryHasNoStreamsAndThereAreNoDefaultStreams() {
        Search search = searchWithQueriesWithStreams("a,b,c", "");

        assertThatExceptionOfType(MissingStreamPermissionException.class)
                .isThrownBy(() -> search.addStreamsToQueriesWithoutStreams(ImmutableSet::of))
                .satisfies(ex -> assertThat(ex.streamsWithMissingPermissions()).isEmpty());
    }

    @Test
    void usedStreamIdsReturnsStreamIdsOfSearchTypes() {
        final Query query1 = queryWithStreams("a,b,d").toBuilder()
                .searchTypes(ImmutableSet.of(
                        searchTypeWithStreams("e,f,g"),
                        searchTypeWithStreams("a,h,b")
                ))
                .build();
        final Search search = Search.builder().queries(ImmutableSet.of(query1)).build();

        assertThat(search.usedStreamIds()).containsExactlyInAnyOrder("a", "b", "d", "e", "f", "g", "h");
    }

    @Test
    void usedStreamIdsReturnsEmptySetForMissingQueries() {
        final Search search = Search.builder().build();

        assertThat(search.usedStreamIds()).isEmpty();
    }

    @Test
    void usedStreamIdsReturnsQueryStreamsWhenSearchTypesAreMissing() {
        final Search search = searchWithQueriesWithStreams("c,d,e");

        assertThat(search.usedStreamIds()).containsExactlyInAnyOrder("c", "d", "e");
    }

    private static final ObjectMapperProvider objectMapperProvider = new ObjectMapperProvider();

    @Test
    void keepsSingleSearchTypeWhenOverridden() {
        Search before = Search.builder().queries(queriesWithSearchTypes("oans,zwoa")).build();

        ExecutionState executionState = partialResultsMapWithSearchTypes("oans");

        Search after = before.applyExecutionState(executionState);

        assertThat(searchTypeIdsFrom(after)).containsOnly("oans");
    }

    @Test
    void keepsMultipleSearchTypesWhenOverridden() {
        Search before = Search.builder().queries(queriesWithSearchTypes("oans,zwoa", "gsuffa")).build();

        ExecutionState executionState = partialResultsMapWithSearchTypes("oans", "gsuffa");

        Search after = before.applyExecutionState(executionState);

        assertThat(searchTypeIdsFrom(after)).containsExactlyInAnyOrder("oans", "gsuffa");
    }

    @Test
    void collectsStreamIdsForPermissionsCheckFromQueries() {
        Query query1 = mock(Query.class);
        doReturn(Set.of("x", "y", "z")).when(query1).streamIdsForPermissionsCheck();
        Query query2 = mock(Query.class);
        doReturn(Set.of("x", "a", "b")).when(query2).streamIdsForPermissionsCheck();
        Query query3 = mock(Query.class);
        doReturn(Set.of()).when(query3).streamIdsForPermissionsCheck();

        Search search = Search.builder()
                .id("Test search")
                .queries(ImmutableSet.of(query1, query2, query3))
                .build();

        assertThat(search.streamIdsForPermissionsCheck())
                .isNotNull()
                .hasSize(5)
                .contains("a", "b", "x", "y", "z");

    }

    private Set<String> searchTypeIdsFrom(Search search) {
        return search.queries().stream()
                .flatMap(q -> q.searchTypes().stream().map(SearchType::id))
                .collect(Collectors.toSet());
    }

    private ExecutionState partialResultsMapWithSearchTypes(String... searchTypeIds) {
        final ExecutionStateGlobalOverride.Builder builder = ExecutionStateGlobalOverride.builder();
        for (String id : searchTypeIds) {
            builder.keepSearchTypesBuilder().add(id);
        }
        return ExecutionState.builder()
                .setGlobalOverride(builder.build())
                .build();
    }

    private MessageList messageList(String id) {
        return MessageList.builder().id(id).build();
    }

    private Search searchWithQueriesWithStreams(String... queriesWithStreams) {
        Set<Query> queries = Arrays.stream(queriesWithStreams).map(this::queryWithStreams).collect(Collectors.toSet());

        return Search.builder().queries(ImmutableSet.copyOf(queries)).build();
    }

    private ImmutableSet<Query> queriesWithSearchTypes(String... queriesWithSearchTypes) {
        Set<Query> queries = Arrays.stream(queriesWithSearchTypes)
                .map(this::queryWithSearchTypes).collect(Collectors.toSet());
        return ImmutableSet.copyOf(queries);
    }

    private Query queryWithSearchTypes(String searchTypeIds) {
        String[] split = searchTypeIds.isEmpty() ? new String[0] : searchTypeIds.split(",");
        return validQueryBuilder().searchTypes(searchTypes(split)).build();
    }

    private Set<SearchType> searchTypes(String... ids) {
        return Arrays.stream(ids).map(this::messageList).collect(Collectors.toSet());
    }

    private Query queryWithStreams(String streamIds) {
        String[] split = streamIds.isEmpty() ? new String[0] : streamIds.split(",");
        return queryWithStreams(split);
    }

    private Query queryWithStreams(String... streamIds) {
        Query.Builder builder = validQueryBuilder();

        if (streamIds.length > 0) {
            builder = builder.filter(StreamFilter.anyIdOf(streamIds));
        }

        return builder.build();
    }

    private SearchType searchTypeWithStreams(String streamIds) {
        final SearchType searchType = mock(SearchType.class);
        final Set<String> streamIdSet = streamIds.isEmpty() ? Collections.emptySet() : new HashSet<>(Arrays.asList(streamIds.split(",")));
        when(searchType.effectiveStreams()).thenReturn(streamIdSet);
        when(searchType.id()).thenReturn(UUID.randomUUID().toString());

        return searchType;
    }
}

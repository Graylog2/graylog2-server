/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog.plugins.views.search;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import org.graylog.plugins.views.search.engine.BackendQuery;
import org.graylog.plugins.views.search.errors.PermissionException;
import org.graylog.plugins.views.search.filter.StreamFilter;
import org.graylog.plugins.views.search.searchtypes.MessageList;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SearchTest {
    @Test
    public void addsDefaultStreamsToQueriesWithoutStreams() {
        Search before = searchWithQueriesWithStreams("");

        Search after = before.addStreamsToQueriesWithoutStreams(() -> ImmutableSet.of("one", "two", "three"));

        assertThat(after.queries().asList().get(0).usedStreamIds()).containsExactlyInAnyOrder("one", "two", "three");
    }

    @Test
    public void leavesQueriesWithDefinedStreamsUntouched() {
        Search before = searchWithQueriesWithStreams("a,b,c", "");

        Search after = before.addStreamsToQueriesWithoutStreams(() -> ImmutableSet.of("one", "two", "three"));

        assertThat(after.queries().asList().get(0).usedStreamIds()).containsExactlyInAnyOrder("a", "b", "c");
    }

    @Test
    public void doesNothingIfAllQueriesHaveDefinedStreams() {
        Search before = searchWithQueriesWithStreams("a,b,c", "a,d,f");

        Search after = before.addStreamsToQueriesWithoutStreams(() -> ImmutableSet.of("one", "two", "three"));

        assertThat(before).isEqualTo(after);
    }

    @Test
    public void throwsExceptionIfQueryHasNoStreamsAndThereAreNoDefaultStreams() {
        Search search = searchWithQueriesWithStreams("a,b,c", "");

        assertThatExceptionOfType(PermissionException.class)
                .isThrownBy(() -> search.addStreamsToQueriesWithoutStreams(ImmutableSet::of));
    }

    @Test
    public void usedStreamIdsReturnsStreamIdsOfSearchTypes() {
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
    public void usedStreamIdsReturnsEmptySetForMissingQueries() {
        final Search search = Search.builder().build();

        assertThat(search.usedStreamIds()).isEmpty();
    }

    @Test
    public void usedStreamIdsReturnsQueryStreamsWhenSearchTypesAreMissing() {
        final Search search = searchWithQueriesWithStreams("c,d,e");

        assertThat(search.usedStreamIds()).containsExactlyInAnyOrder("c", "d", "e");
    }

    private static final ObjectMapperProvider objectMapperProvider = new ObjectMapperProvider();

    @Test
    public void keepsSingleSearchTypeWhenOverridden() {
        Search before = Search.builder().queries(queriesWithSearchTypes("oans,zwoa")).build();

        Map<String, Object> executionState = partialResultsMapWithSearchTypes("oans");

        Search after = before.applyExecutionState(objectMapperProvider.get(), executionState);

        assertThat(searchTypeIdsFrom(after)).containsOnly("oans");
    }

    @Test
    public void keepsMultipleSearchTypesWhenOverridden() {
        Search before = Search.builder().queries(queriesWithSearchTypes("oans,zwoa", "gsuffa")).build();

        Map<String, Object> executionState = partialResultsMapWithSearchTypes("oans", "gsuffa");

        Search after = before.applyExecutionState(objectMapperProvider.get(), executionState);

        assertThat(searchTypeIdsFrom(after)).containsExactlyInAnyOrder("oans", "gsuffa");
    }

    @Test
    public void removesQueryIfNoneOfItsSearchTypesIsRequired() {
        Search before = Search.builder().queries(queriesWithSearchTypes("oans,zwoa", "gsuffa")).build();

        Map<String, Object> executionState = partialResultsMapWithSearchTypes("oans");

        Search after = before.applyExecutionState(objectMapperProvider.get(), executionState);

        String expected = idOfQueryWithSearchType(before.queries(), "oans");

        assertThat(after.queries()).extracting(Query::id).containsExactly(expected);
    }

    private Set<String> searchTypeIdsFrom(Search search) {
        return search.queries().stream()
                .flatMap(q -> q.searchTypes().stream().map(SearchType::id))
                .collect(Collectors.toSet());
    }

    private String idOfQueryWithSearchType(ImmutableSet<Query> queries, @SuppressWarnings("SameParameterValue") String searchTypeId) {
        return queries.stream()
                .filter(q -> hasSearchType(q, searchTypeId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("no matching query for search type " + searchTypeId))
                .id();
    }

    private boolean hasSearchType(Query q, String searchTypeId) {
        return q.searchTypes().stream().map(SearchType::id).anyMatch(id -> id.equals(searchTypeId));
    }

    private Map<String, Object> partialResultsMapWithSearchTypes(String... searchTypeIds) {
        Multimap<String, String> searchTypes = HashMultimap.create();
        for (String id : searchTypeIds)
            searchTypes.put("keep_search_types", id);

        Map<String, Object> executionState = new HashMap<>();
        executionState.put("global_override", searchTypes);

        return executionState;
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

        if (streamIds.length > 0)
            builder = builder.filter(StreamFilter.anyIdOf(streamIds));

        return builder.build();
    }

    private Query.Builder validQueryBuilder() {
        return Query.builder().id(UUID.randomUUID().toString()).timerange(mock(TimeRange.class)).query(new BackendQuery.Fallback());
    }

    private SearchType searchTypeWithStreams(String streamIds) {
        final SearchType searchType = mock(SearchType.class);
        final Set<String> streamIdSet = streamIds.isEmpty() ? Collections.emptySet() : new HashSet<>(Arrays.asList(streamIds.split(",")));
        when(searchType.effectiveStreams()).thenReturn(streamIdSet);
        when(searchType.id()).thenReturn(UUID.randomUUID().toString());

        return searchType;
    }
}
